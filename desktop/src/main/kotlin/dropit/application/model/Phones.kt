package dropit.application.model

import dropit.Application
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.infrastructure.event.AppEvent
import dropit.jooq.tables.pojos.Phone
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.PHONE
import java.util.*

object Phones : ApplicationModel() {
    init {
        Application.component.inject(this)
    }

    fun findOrCreate(request: TokenRequest): PhoneRecord {
        val existing = jooq.fetchOne(PHONE, PHONE.ID.eq(request.id))
        if (existing != null) return existing

        val phone = jooq.newRecord(PHONE, Phone(
            id = request.id,
            name = request.name,
            token = UUID.randomUUID(),
            status = TokenStatus.PENDING
        )).apply { insert() }
        bus.broadcast(PhoneChangedEvent(phone))
        bus.broadcast(NewPhoneRequestEvent(phone))

        return phone
    }

    fun all(): List<PhoneRecord> = jooq.selectFrom(PHONE).orderBy(PHONE.NAME).fetch()

    fun current(): PhoneRecord? = jooq.fetchOne(PHONE, PHONE.ID.eq(appSettings.currentPhoneId))

    fun pending(): List<PhoneRecord> =
        jooq.selectFrom(PHONE).where(PHONE.STATUS.eq(TokenStatus.PENDING)).fetch()

    data class NewPhoneRequestEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
    data class PhoneChangedEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
    data class PhonePairedEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
    data class PhoneRejectedEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
}
