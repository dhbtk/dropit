package dropit.application.model

import dropit.jooq.keys.FK_TRANSFER_FILE_TRANSFER_1
import dropit.jooq.tables.records.TransferFileRecord
import dropit.jooq.tables.records.TransferRecord

val TransferFileRecord.transfer: TransferRecord by ExtLazy { fetchParent(FK_TRANSFER_FILE_TRANSFER_1)!! }
