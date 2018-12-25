package dropit.domain.service

import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenResponse
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.Phone
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.Phone.PHONE
import dropit.jooq.tables.Transfer.TRANSFER
import dropit.jooq.tables.TransferFile.TRANSFER_FILE
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneService @Inject constructor(
    val create: DSLContext,
    val bus: EventBus,
    val appSettings: AppSettings) {
    data class NewPhoneRequestEvent(override val payload: Phone) : AppEvent<Phone>
    class PhoneChangedEvent(override val payload: Phone) : AppEvent<Phone>

    /**
     * Called from web
     *
     * Allows the phone to request a token.
     */
    fun requestToken(request: TokenRequest): String {
        return create.transactionResult { _ ->
            val alreadyExists = create.select()
                .from(PHONE)
                .where(PHONE.ID.eq(request.id))
                .fetchOneInto(Phone::class.java)
            if (alreadyExists != null) {
                alreadyExists.token.toString()
            } else {
                val phone = Phone(id = UUID.fromString(request.id), name = request.name, token = UUID.randomUUID(), status = TokenStatus.PENDING)
                val inserted = create.newRecord(PHONE, phone).store()
                if (inserted == 0) {
                    throw RuntimeException("Could not save phone record")
                }
                bus.broadcast(PhoneChangedEvent(phone))
                bus.broadcast(NewPhoneRequestEvent(phone))
                phone.token.toString()
            }
        }
    }

    /**
     * Called from web
     *
     * Returns the status for a given phone token.
     */
    fun getTokenStatus(token: String): TokenResponse {
        return create.selectFrom(PHONE)
            .where(PHONE.TOKEN.eq(token))
            .fetchOptionalInto(Phone::class.java)
            .map { it.status!! }
            .map { TokenResponse(it, if (it == TokenStatus.AUTHORIZED) appSettings.settings.computerSecret else null) }
            .orElseThrow { UnauthorizedException(token) }
    }

    /**
     * Called from UI
     *
     * Authorizes the request for a given phone.
     */
    fun authorizePhone(id: UUID): Phone {
        return create.transactionResult { _ ->
            val pendingPhone: Phone = create.fetchOne(PHONE, PHONE.ID.eq(id.toString())).into(Phone::class.java)
                ?: throw RuntimeException(t("phoneService.common.phoneNotFound", id))
            create.newRecord(PHONE, pendingPhone.copy(updatedAt = LocalDateTime.now(), status = TokenStatus.AUTHORIZED)).update()
            val authorizedPhone = create.fetchOne(PHONE, PHONE.ID.eq(id.toString())).into(Phone::class.java)
            bus.broadcast(PhoneChangedEvent(authorizedPhone))
            authorizedPhone
        }
    }

    /**
     * Called from UI
     *
     * Denies/unauthorizes the request for a given phone.
     */
    fun denyPhone(id: UUID): Phone {
        return create.transactionResult { _ ->
            val pendingPhone: Phone = create.fetchOne(PHONE, PHONE.ID.eq(id.toString())).into(Phone::class.java)
                ?: throw RuntimeException(t("phoneService.common.phoneNotFound", id))
            create.newRecord(PHONE, pendingPhone.copy(updatedAt = LocalDateTime.now(), status = TokenStatus.DENIED)).update()
            val deniedPhone = create.fetchOne(PHONE, PHONE.ID.eq(id.toString())).into(Phone::class.java)
            bus.broadcast(PhoneChangedEvent(deniedPhone))
            deniedPhone
        }
    }

    /**
     * Called from UI
     *
     * Lists all phones.
     */
    fun listPhones(showDenied: Boolean): List<Phone> {
        val condition = if (showDenied) {
            PHONE.STATUS.isNotNull
        } else {
            PHONE.STATUS.ne(TokenStatus.DENIED.name)
        }
        return create.selectFrom(PHONE).where(condition).orderBy(PHONE.CREATED_AT.desc()).fetchInto(Phone::class.java)
    }

    /**
     * Called from UI
     *
     * Deletes a denied phone.
     */
    fun deletePhone(id: UUID) {
        create.transaction { _ ->
            create.deleteFrom(TRANSFER_FILE).where(TRANSFER_FILE.TRANSFER_ID.`in`(
                create.select(TRANSFER.ID).from(TRANSFER).where(TRANSFER.PHONE_ID.eq(id.toString()))))
                .execute()
            create.deleteFrom(TRANSFER).where(TRANSFER.PHONE_ID.eq(id.toString())).execute()
            create.deleteFrom(PHONE).where(PHONE.ID.eq(id.toString())).execute()
        }
    }
}