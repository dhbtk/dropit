package dropit.application.model

import dropit.Application
import dropit.domain.service.IncomingService
import java.util.*
import dropit.jooq.tables.pojos.ClipboardLog
import dropit.jooq.tables.references.CLIPBOARD_LOG

object Clipboard : ApplicationModel() {
    init {
        Application.component.inject(this)
    }

    fun receive(data: String) {
        if (appSettings.logClipboardTransfers) {
            jooq.newRecord(
                CLIPBOARD_LOG, ClipboardLog(
                    id = UUID.randomUUID(),
                    content = data,
                    source = TransferSource.PHONE
                )
            ).insert()
        }
        bus.broadcast(IncomingService.ClipboardReceiveEvent(data))
    }
}
