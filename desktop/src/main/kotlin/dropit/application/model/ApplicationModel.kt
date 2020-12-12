package dropit.application.model

import dropit.application.settings.AppSettings
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.fs.TransferFolderProvider
import org.jooq.DSLContext
import javax.inject.Inject

abstract class ApplicationModel {
    @Inject
    lateinit var jooq: DSLContext

    @Inject
    lateinit var bus: EventBus

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var transferFolderProvider: TransferFolderProvider
}
