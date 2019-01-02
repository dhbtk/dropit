package dropit.domain.service

import dropit.application.settings.AppSettings
import dropit.domain.entity.ClipboardLog
import dropit.domain.entity.TransferSource
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.jooq.tables.ClipboardLog.CLIPBOARD_LOG
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(
    private val jooq: DSLContext,
    private val bus: EventBus,
    private val appSettings: AppSettings
) {
    data class ClipboardReceiveEvent(override val payload: String) : AppEvent<String>

    fun receive(data: String) {
        if (appSettings.settings.logClipboardTransfers) {
            ClipboardLog(
                id = UUID.randomUUID(),
                content = data,
                source = TransferSource.PHONE
            ).apply {
                jooq.newRecord(CLIPBOARD_LOG, this).insert()
            }
        }
        bus.broadcast(ClipboardReceiveEvent(data))
    }
}