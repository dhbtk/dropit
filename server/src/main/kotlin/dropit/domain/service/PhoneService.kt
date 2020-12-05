package dropit.domain.service

import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenResponse
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.pojos.Phone
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.PHONE
import dropit.jooq.tables.references.TRANSFER
import dropit.jooq.tables.references.TRANSFER_FILE
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneService @Inject constructor(
    val create: DSLContext,
    val bus: EventBus,
    val appSettings: AppSettings
) {
    data class NewPhoneRequestEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
    class PhoneChangedEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>

    /**
     * Called from web
     *
     * Allows the phone to request a token.
     */
    fun requestToken(request: TokenRequest): String {
        return create.transactionResult { _ ->
            val alreadyExists = create.selectFrom(PHONE)
                .where(PHONE.ID.eq(request.id))
                .fetchOne()
            if (alreadyExists != null) {
                alreadyExists.token.toString()
            } else {
                val phone = create.newRecord(
                    PHONE, Phone(
                        id = request.id,
                        name = request.name,
                        token = UUID.randomUUID(),
                        status = TokenStatus.PENDING
                    )
                )
                val inserted = phone.insert()
                if (inserted == 0) {
                    throw IllegalStateException("Could not save phone record")
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
    fun getTokenStatus(token: UUID): TokenResponse {
        return create.selectFrom(PHONE)
            .where(PHONE.TOKEN.eq(token))
            .fetchOptional()
            .map { it.status!! }
            .map { TokenResponse(it, if (it == TokenStatus.AUTHORIZED) appSettings.computerSecret else null) }
            .orElseThrow { UnauthorizedException(token.toString()) }
    }

    /**
     * Called from UI
     *
     * Authorizes the request for a given phone.
     */
    fun authorizePhone(id: UUID): PhoneRecord {
        return create.transactionResult { _ ->
            val phone = create.fetchOne(PHONE, PHONE.ID.eq(id))!!
            phone.status = TokenStatus.AUTHORIZED
            phone.update()
            bus.broadcast(PhoneChangedEvent(phone))
            appSettings.currentPhoneId = id
            phone
        }
    }

    /**
     * Called from UI
     *
     * Denies/unauthorizes the request for a given phone.
     */
    fun denyPhone(id: UUID): PhoneRecord {
        return create.transactionResult { _ ->
            val phone = create.fetchOne(PHONE, PHONE.ID.eq(id))!!
            phone.status = TokenStatus.DENIED
            phone.update()
            bus.broadcast(PhoneChangedEvent(phone))
            phone
        }
    }

    /**
     * Called from UI
     *
     * Lists all phones.
     */
    fun listPhones(showDenied: Boolean): List<PhoneRecord> {
        val condition = if (showDenied) {
            PHONE.STATUS.isNotNull
        } else {
            PHONE.STATUS.ne(TokenStatus.DENIED)
        }
        return create.selectFrom(PHONE).where(condition).orderBy(PHONE.CREATED_AT.desc()).fetch()
    }

    /**
     * Called from UI
     *
     * Deletes a denied phone.
     */
    fun deletePhone(id: UUID) {
        create.transaction { _ ->
            create.deleteFrom(TRANSFER_FILE).where(
                TRANSFER_FILE.TRANSFER_ID.`in`(
                    create.select(TRANSFER.ID).from(TRANSFER).where(TRANSFER.PHONE_ID.eq(id))
                )
            )
                .execute()
            create.deleteFrom(TRANSFER).where(TRANSFER.PHONE_ID.eq(id)).execute()
            create.deleteFrom(PHONE).where(PHONE.ID.eq(id)).execute()
        }
    }
}
