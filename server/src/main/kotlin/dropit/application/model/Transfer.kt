package dropit.application.model

import dropit.jooq.keys.FK_TRANSFER_FILE_TRANSFER_1
import dropit.jooq.tables.records.TransferFileRecord
import dropit.jooq.tables.records.TransferRecord

val TransferRecord.files: List<TransferFileRecord>
    get() = fetchChildren(FK_TRANSFER_FILE_TRANSFER_1)
