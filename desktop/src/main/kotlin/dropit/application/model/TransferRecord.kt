package dropit.application.model

import dropit.application.dto.FileStatus
import dropit.jooq.keys.FK_TRANSFER_FILE_TRANSFER_1
import dropit.jooq.tables.records.TransferFileRecord
import dropit.jooq.tables.records.TransferRecord
import java.nio.file.Path

val TransferRecord.files: List<TransferFileRecord> by ExtLazy { fetchChildren(FK_TRANSFER_FILE_TRANSFER_1) }

val TransferRecord.filesFinished
    get() = fetchChildren(FK_TRANSFER_FILE_TRANSFER_1).all { it.status == FileStatus.FINISHED }

val TransferRecord.folder: Path by ExtLazy { FileTransfers.transferFolderProvider.getForTransfer(this) }
