package dropit.application

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.DownloadStatus
import dropit.application.dto.SentFileInfo
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.ClipboardLog
import dropit.domain.entity.Phone
import dropit.domain.entity.SentFile
import dropit.domain.entity.TransferSource
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.jooq.tables.ClipboardLog.Companion.CLIPBOARD_LOG
import dropit.jooq.tables.Phone.Companion.PHONE
import dropit.jooq.tables.SentFile.Companion.SENT_FILE
import dropit.jooq.tables.records.PhoneRecord
import dropit.logger
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.ArrayList
import java.util.HashMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneSessionService @Inject constructor(
        val bus: EventBus,
        val jooq: DSLContext,
        val objectMapper: ObjectMapper,
        val appSettings: AppSettings
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

    /**
     *
     */
    fun openSession(context: WsContext) {
        val token = context.header("Authorization")?.split(" ")?.last()
        if (token == null) {
            context.session.close()
            return
        }
        val phone = getPhoneByToken(token)
        if (phone == null || phone.id != appSettings.settings.currentPhoneId) {
            context.session.close()
            return
        }
        context.session.idleTimeout = Long.MAX_VALUE
        phoneSessions.compute(phone.id!!) { _, phoneSession ->
            if (phoneSession != null) {
                if (phoneSession.session != null) {
                    context.session.close()
                } else {
                    phoneSession.session = context
                    if (phoneSession.clipboardData != null) {
                        context.send(phoneSession.clipboardData!!)
                        phoneSession.clipboardData = null
                    }
                    sendFileList(phoneSession)
                }
                phoneSession
            } else {
                PhoneSession(context)
            }
        }
        updatePhoneLastConnected(phone.id)
        bus.broadcast(PhoneService.PhoneChangedEvent(phone))
    }

    /**
     *
     */
    fun receiveDownloadStatus(session: WsMessageContext) {
        try {
            val (fileId, downloaded) = session.message(DownloadStatus::class.java)
            val phoneSession = phoneSessions.filterValues { it.session?.header("Authorization") == session.header("Authorization") }.values.first()
            val sentFile = phoneSession.files.find { it.id == fileId } ?: return
            if (sentFile.size == downloaded) {
                fileDownloadStatus.remove(sentFile)
                bus.broadcast(UploadFinishedEvent(sentFile))
                phoneSession.files.remove(sentFile)
                val fileRecord = jooq.newRecord(SENT_FILE, SentFile(
                        sentFile.id,
                        null,
                        null,
                        appSettings.settings.currentPhoneId,
                        sentFile.file.toString(),
                        Files.probeContentType(sentFile.file.toPath()),
                        sentFile.size
                ))
                jooq.insertInto(SENT_FILE).set(fileRecord).execute()
            } else {
                fileDownloadStatus[sentFile]?.add(Pair(LocalDateTime.now(), downloaded))
                bus.broadcast(UploadProgressEvent(sentFile))
            }
        } catch (e: JsonMappingException) {
            // nop
        } catch (e: JsonParseException) {
            // nop
        }
    }

    /**
     *
     */
    fun closeSession(session: WsContext) {
        logger.debug("Closing session with header ${session.header("Authorization")}")
        phoneSessions.forEach { (id, value) ->
            logger.debug("checking phone session token ${value.session?.header("Authorization")}")
            if (value.session?.header("Authorization") == session.header("Authorization")) {
                value.session = null
                bus.broadcast(PhoneService.PhoneChangedEvent(getPhoneById(id)!!))
            }
        }
    }

    fun getFileDownload(phone: Phone, id: UUID): File {
        val file = phoneSessions[phone.id!!]!!.files.find { it.id == id }!!
        fileDownloadStatus[file] = ArrayList() // reset download data
        bus.broadcast(UploadStartedEvent(file))
        return file.file
    }

    fun sendFile(phoneId: UUID, file: File) {
        getPhoneById(phoneId) ?: return
        val session = phoneSessions.computeIfAbsent(phoneId) { PhoneSession() }
        val sentFile = FileUpload(file, file.length())
        session.files.add(sentFile)
        fileDownloadStatus[sentFile] = ArrayList()
        sendFileList(session)
    }

    fun sendClipboard(phoneId: UUID, data: String) {
        getPhoneById(phoneId) ?: return
        val session = phoneSessions.computeIfAbsent(phoneId) { PhoneSession() }
        session.clipboardData = data
        session.session?.send(data)
        if (session.session != null && appSettings.settings.logClipboardTransfers) {
            ClipboardLog(
                    id = UUID.randomUUID(),
                    content = data,
                    source = TransferSource.COMPUTER
            ).apply {
                jooq.newRecord(CLIPBOARD_LOG, this).insert()
            }
        }
        session.clipboardData = null
    }

    private fun sendFileList(session: PhoneSession) {
        val wsContext = session.session
        if (wsContext != null) {
            session.files.forEach { sentFile ->
                if (fileDownloadStatus[sentFile]!!.isEmpty()) {
                    wsContext.send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(
                            SentFileInfo(sentFile.id, sentFile.file.length()))))
                }
            }
        }
    }

    private fun getPhoneById(id: UUID): Phone? {
        return jooq.selectFrom<PhoneRecord>(PHONE)
                .where(PHONE.ID.eq(id))
                .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED))
                .fetchOptionalInto(Phone::class.java).orElse(null)
    }

    private fun getPhoneByToken(token: String): Phone? {
        return jooq.selectFrom<PhoneRecord>(PHONE)
                .where(PHONE.TOKEN.eq(UUID.fromString(token)))
                .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED))
                .fetchOptionalInto(Phone::class.java).orElse(null)
    }

    private fun updatePhoneLastConnected(id: UUID) {
        jooq.fetchOne(PHONE, PHONE.ID.eq(id))?.into(Phone::class.java)
                ?.copy(lastConnected = LocalDateTime.now())
                ?.let { phone ->
                    jooq.newRecord(PHONE)
                            .apply {
                                from(phone)
                                update()
                            }
                }
    }
}
