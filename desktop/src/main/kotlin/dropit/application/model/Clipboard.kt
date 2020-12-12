package dropit.application.model

import dropit.Application
import dropit.jooq.tables.pojos.ClipboardLog
import dropit.jooq.tables.references.CLIPBOARD_LOG
import java.util.*

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
        bus.broadcast(FileTransfers.ClipboardReceiveEvent(data))
    }
}
