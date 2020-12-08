package dropit.application.model

import dropit.application.dto.FileRequest
import dropit.application.dto.FileStatus
import dropit.jooq.tables.pojos.TransferFile
import dropit.jooq.tables.records.TransferRecord
import java.util.*

fun FileRequest.toTransferFile(transfer: TransferRecord) = TransferFile(
    id = UUID.fromString(id),
    transferId = transfer.id,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    status = FileStatus.PENDING
)
