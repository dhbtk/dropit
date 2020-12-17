package dropit.application

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.SentFileInfo
import dropit.application.dto.TokenStatus
import dropit.application.model.Phones
import dropit.application.model.TransferSource
import dropit.application.model.isDefault
import dropit.application.settings.AppSettings
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.jooq.tables.pojos.ClipboardLog
import dropit.jooq.tables.pojos.SentFile
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.CLIPBOARD_LOG
import dropit.jooq.tables.references.PHONE
import dropit.jooq.tables.references.SENT_FILE
import dropit.logger
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsErrorContext
import io.javalin.websocket.WsHandler
import org.jooq.DSLContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneSessions @Inject constructor(
    private val bus: EventBus,
    private val jooq: DSLContext,
    private val objectMapper: ObjectMapper,
    private val appSettings: AppSettings
) {
    val fileDownloadStatus = HashMap<FileUpload, MutableList<Pair<LocalDateTime, Long>>>()

    val phoneSessions = HashMap<UUID, PhoneSession>()

    data class PhoneSession(
        var session: WsContext? = null,
        var clipboardData: String? = null,
        val files: MutableList<FileUpload> = ArrayList<FileUpload>()
    )

    data class FileUpload(
        val file: File,
        val size: Long,
        val id: UUID = UUID.randomUUID()
    )

    data class UploadStartedEvent(override val payload: FileUpload) : AppEvent<FileUpload>
    data class UploadProgressEvent(override val payload: FileUpload) : AppEvent<FileUpload>
    data class UploadFinishedEvent(override val payload: FileUpload) : AppEvent<FileUpload>

    private val WsContext.token
        get() = header("Authorization")
            ?.replace(Regex("^Bearer "), "")
            ?.let(UUID::fromString)
    private val WsContext._phone get() = token?.let(this@PhoneSessions::getPhoneById)
    private val WsContext.isForDefaultPhone get() = _phone != null && _phone!!.isDefault
    private val WsContext.phone get() = _phone!!
    private val WsContext.phoneId get() = phone.id!!

    /**
     *
     */
    private fun openSession(context: WsContext) {
        if (!context.isForDefaultPhone) {
            context.session.close()
            return
        }

        if (phoneSessions[context.phoneId] != null) {
            updateSession(context)
        } else {
            phoneSessions[context.phoneId] = PhoneSession(context)
        }

        updatePhoneLastConnected(context.phoneId)
        bus.broadcast(Phones.PhoneChangedEvent(context.phone))
    }

    private fun updateSession(context: WsContext) {
        val phoneSession = phoneSessions[context.phoneId]!!
        phoneSession.session = context

        phoneSession.clipboardData?.also { data ->
            context.send(data)
            phoneSession.clipboardData = null
        }
        sendFileList(phoneSession)
    }

    fun defaultPhoneConnected() = phoneSessions[Phones.current()?.id]?.session != null

    fun configureEndpoint(wsHandler: WsHandler) {
        wsHandler.onConnect(this::openSession)
        wsHandler.onError(this::closeSession)
        wsHandler.onClose(this::closeSession)
    }

    fun recordUploadFinished(phone: PhoneRecord, fileId: UUID) {
        val phoneSession = phoneSessions[phone.id!!]!!
        val sentFile = phoneSession.files.find { it.id == fileId } ?: return
        fileDownloadStatus.remove(sentFile)
        bus.broadcast(UploadFinishedEvent(sentFile))
        phoneSession.files.remove(sentFile)
        jooq.newRecord(
            SENT_FILE, SentFile(
                id = sentFile.id,
                phoneId = appSettings.currentPhoneId,
                fileName = sentFile.file.toString(),
                mimeType = Files.probeContentType(sentFile.file.toPath()),
                fileSize = sentFile.size
            )
        ).insert()
    }

    private fun closeSession(session: WsContext) {
        if (session is WsErrorContext) {
            logger.warn("Error on phone session with ID ${session.sessionId}", session.error())
        } else if (session is WsCloseContext) {
            logger.info("Closing session: id = ${session.sessionId} statusCode = ${session.status()}, reason: ${session.reason()}")
        }
        phoneSessions.values.find { it.session?.token == session.token }?.let { phoneSession ->
            phoneSession.session = null
            bus.broadcast(Phones.PhoneChangedEvent(session.phone))
        }
    }

    fun getFileDownload(phone: PhoneRecord, id: UUID): File {
        val file = phoneSessions[phone.id]!!.files.find { it.id == id }!!
        fileDownloadStatus[file] = ArrayList() // reset download data
        bus.broadcast(UploadStartedEvent(file))
        return file.file
    }

    fun sendFile(phoneId: UUID, file: File) {
        val session = phoneSessions.computeIfAbsent(phoneId) { PhoneSession() }
        val sentFile = FileUpload(file, file.length())
        session.files.add(sentFile)
        fileDownloadStatus[sentFile] = ArrayList()
        sendFileList(session)
    }

    fun sendClipboard(phoneId: UUID, data: String) {
        val session = phoneSessions.computeIfAbsent(phoneId) { PhoneSession() }
        session.clipboardData = data
        session.session?.send(data)
        if (session.session != null && appSettings.logClipboardTransfers) {
            jooq.newRecord(
                CLIPBOARD_LOG, ClipboardLog(
                    id = UUID.randomUUID(),
                    content = data,
                    source = TransferSource.COMPUTER
                )
            ).insert()
        }
        session.clipboardData = null
    }

    private fun sendFileList(session: PhoneSession) {
        val wsContext = session.session ?: return

        session.files.forEach { sentFile ->
            if (fileDownloadStatus[sentFile]!!.isEmpty()) {
                SentFileInfo(sentFile.id, sentFile.file.length())
                    .let { objectMapper.writeValueAsBytes(it) }
                    .let { ByteBuffer.wrap(it) }
                    .let { wsContext.send(it) }
            }
        }
    }

    private fun getPhoneById(id: UUID): PhoneRecord? {
        return jooq.fetchOne(PHONE, PHONE.ID.eq(id).and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED)))
    }

    private fun updatePhoneLastConnected(id: UUID) {
        jooq.fetchOne(PHONE, PHONE.ID.eq(id))?.apply {
            lastConnected = LocalDateTime.now()
            update()
        }
    }
}
