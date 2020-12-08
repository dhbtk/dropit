package dropit.domain.service

import dropit.application.settings.AppSettings
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.jooq.tables.records.PhoneRecord
import org.jooq.DSLContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneService @Inject constructor(
    val create: DSLContext,
    val bus: EventBus,
    val appSettings: AppSettings
) {
    data class NewPhoneRequestEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
    data class PhoneChangedEvent(override val payload: PhoneRecord) : AppEvent<PhoneRecord>
}
