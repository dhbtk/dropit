package dropit.application.model

import dropit.Application
import dropit.application.dto.TransferRequest
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.records.TransferRecord
import dropit.jooq.tables.references.TRANSFER
import dropit.jooq.tables.references.TRANSFER_FILE

object Transfers : ApplicationModel() {
    init {
        Application.component.inject(this)
    }

    fun create(phone: PhoneRecord, request: TransferRequest): TransferRecord {
        val transfer = jooq.newRecord(TRANSFER, request.toTransfer(phone)).apply { insert() }
        for (file in request.files) {
            jooq.newRecord(TRANSFER_FILE, file.toTransferFile(transfer)).insert()
        }
        bus.broadcast(FileTransfers.NewTransferEvent(transfer))
        return transfer
    }
}
