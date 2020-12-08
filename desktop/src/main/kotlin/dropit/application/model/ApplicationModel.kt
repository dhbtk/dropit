package dropit.application.model

import dropit.application.settings.AppSettings
import dropit.infrastructure.event.EventBus
import org.jooq.DSLContext
import javax.inject.Inject

abstract class ApplicationModel {
    @Inject
    lateinit var jooq: DSLContext

    @Inject
    lateinit var bus: EventBus

    @Inject
    lateinit var appSettings: AppSettings
}
