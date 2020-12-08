package dropit.application.model

import dropit.application.dto.TokenResponse
import dropit.application.dto.TokenStatus
import dropit.application.model.Phones.appSettings
import dropit.application.model.Phones.bus
import dropit.application.model.Phones.jooq
import dropit.domain.service.PhoneService
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.PHONE
import dropit.jooq.tables.references.TRANSFER
import dropit.jooq.tables.references.TRANSFER_FILE

fun PhoneRecord.role(): PhoneRole = status?.let { PhoneRole.valueOf(it.name) } ?: PhoneRole.PENDING

fun PhoneRecord.tokenResponse(): TokenResponse {
    if (status != TokenStatus.AUTHORIZED) return TokenResponse(status)

    return TokenResponse(status, appSettings.computerSecret)
}

fun PhoneRecord.authorize() {
    status = TokenStatus.AUTHORIZED
    update()
    appSettings.currentPhoneId = id
    bus.broadcast(PhoneService.PhoneChangedEvent(this))
}

fun PhoneRecord.destroy() {
    jooq.transaction { _ ->
        jooq.deleteFrom(TRANSFER_FILE).where(
            TRANSFER_FILE.TRANSFER_ID.`in`(
                jooq.select(TRANSFER.ID).from(TRANSFER).where(TRANSFER.PHONE_ID.eq(id))
            )
        )
            .execute()
        jooq.deleteFrom(TRANSFER).where(TRANSFER.PHONE_ID.eq(id)).execute()
        delete()
    }
}
