package dropit.application

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.TokenStatus
import dropit.domain.entity.Phone
import dropit.jooq.tables.Phone.PHONE
import dropit.jooq.tables.records.PhoneRecord
import io.javalin.websocket.WsSession
import org.jooq.DSLContext
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneSessionManager @Inject constructor(
    val jooq: DSLContext,
    val objectMapper: ObjectMapper
) {
    class PhoneSession(
        var session: WsSession? = null,
        var clipboardData: String? = null,
        val files: List<SentFile> = mutableListOf()
    )

    data class SentFile(
        val file: File,
        val id: UUID = UUID.randomUUID()
    )

    private val phoneSessions = HashMap<UUID, PhoneSession>()

    fun handleNewSession(session: WsSession) {
        val token = session.header("Authorization")?.split(" ")?.last()
        if (token == null) {
            session.disconnect()
            return
        }
        val phone = getPhoneByToken(token)
        if (phone == null) {
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
                }
                phoneSession
            } else {
                PhoneSession(session)
            }
        }
    }

    fun closeSession(session: WsSession) {
        phoneSessions.forEach { (_, value) ->
            if (value.session?.id == session.id) {
                value.session = null
            }
        }
    }

    fun sendClipboard(phoneId: UUID, data: String) {
        getPhoneById(phoneId) ?: return
        val session = phoneSessions.computeIfAbsent(phoneId) { PhoneSession() }
        session.clipboardData = data
        session.session?.send(data)
        session.clipboardData = null
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