package dropit.application

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.DownloadStatus
import dropit.application.dto.SentFileId
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.ClipboardLog
import dropit.domain.entity.Phone
import dropit.domain.entity.TransferSource
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.jooq.tables.ClipboardLog.CLIPBOARD_LOG
import dropit.jooq.tables.Phone.PHONE
import dropit.jooq.tables.SentFile.SENT_FILE
import dropit.jooq.tables.records.PhoneRecord
import io.javalin.websocket.WsSession
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
class OutgoingService @Inject constructor(
    val bus: EventBus,
    val jooq: DSLContext,
    val objectMapper: ObjectMapper,
    val appSettings: AppSettings
) {
    val fileDownloadStatus = HashMap<SentFile, MutableList<Pair<LocalDateTime, Long>>>()

    private val phoneSessions = HashMap<UUID, PhoneSession>()

    private val logger = LoggerFactory.getLogger(javaClass)

    data class PhoneSession(
        var session: WsSession? = null,
        var clipboardData: String? = null,
        val files: MutableList<SentFile> = mutableListOf()
    )

    data class SentFile(
        val file: File,
        val size: Long,
        val id: UUID = UUID.randomUUID()
    )

    data class DownloadStartedEvent(override val payload: SentFile) : AppEvent<SentFile>
    data class DownloadProgressEvent(override val payload: SentFile) : AppEvent<SentFile>
    data class DownloadFinishedEvent(override val payload: SentFile) : AppEvent<SentFile>

    /**
     *
     */
    fun openSession(session: WsSession) {
        val token = session.header("Authorization")?.split(" ")?.last()
        if (token == null) {
            session.disconnect()
            return
        }
        val phone = getPhoneByToken(token)
        if (phone == null || phone.id != appSettings.settings.currentPhoneId) {
            session.disconnect()
            return
        }
        phoneSessions.compute(phone.id!!) { _, phoneSession ->
            if (phoneSession != null) {
                if (phoneSession.session != null) {
                    session.disconnect()
                } else {
                    phoneSession.session = session
                    if (phoneSession.clipboardData != null) {
                        session.send(phoneSession.clipboardData!!)
                        phoneSession.clipboardData = null
                    }
                    sendFileList(phoneSession)
                }
                phoneSession
            } else {
                PhoneSession(session)
            }
        }
    }

    /**
     *
     */
    fun receiveDownloadStatus(session: WsSession, data: Array<Byte>, offset: Int, length: Int) {
        try {
            val (fileId, downloaded) = objectMapper.readValue<DownloadStatus>(
                data.toByteArray(),
                offset,
                length,
                DownloadStatus::class.java)
            val sentFile = getPhoneSession(session).files.find { it.id == fileId } ?: return
            if (sentFile.size == downloaded) {
                fileDownloadStatus.remove(sentFile)
                bus.broadcast(DownloadFinishedEvent(sentFile))
                getPhoneSession(session).files.remove(sentFile)
                val fileRecord = jooq.newRecord(SENT_FILE, dropit.domain.entity.SentFile(
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
                bus.broadcast(DownloadProgressEvent(sentFile))
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
    fun closeSession(session: WsSession) {
        phoneSessions.forEach { (_, value) ->
            if (value.session?.id == session.id) {
                value.session = null
            }
        }
    }

    fun getFileDownload(phone: Phone, id: UUID): File {
        val file = phoneSessions[phone.id]!!.files.find { it.id == id }!!
        fileDownloadStatus[file] = ArrayList() // reset download data
        bus.broadcast(DownloadStartedEvent(file))
        return file.file
    }

    fun sendFile(phoneId: UUID, file: File) {
        getPhoneById(phoneId) ?: return
        val session = phoneSessions.computeIfAbsent(phoneId) { PhoneSession() }
        val sentFile = SentFile(file, file.length())
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
        val wsSession = session.session
        if (wsSession != null) {
            session.files.forEach { sentFile ->
                if (fileDownloadStatus[sentFile]!!.isEmpty()) {
                    wsSession.send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(SentFileId(sentFile.id))))
                }
            }
        }
    }

    private fun getPhoneSession(session: WsSession): PhoneSession {
        return phoneSessions.filterValues { it.session?.id == session.id }.values.first()
    }

    private fun getPhoneById(id: UUID): Phone? {
        return jooq.selectFrom<PhoneRecord>(PHONE)
            .where(PHONE.ID.eq(id.toString()))
            .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED.toString()))
            .fetchOptionalInto(Phone::class.java).orElse(null)
    }

    private fun getPhoneByToken(token: String): Phone? {
        return jooq.selectFrom<PhoneRecord>(PHONE)
            .where(PHONE.TOKEN.eq(token))
            .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED.toString()))
            .fetchOptionalInto(Phone::class.java).orElse(null)
    }
}