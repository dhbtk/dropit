package dropit.infrastructure.fs

import dropit.application.settings.AppSettings
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.records.TransferRecord
import java.nio.file.Path
import java.nio.file.Paths
import java.text.MessageFormat
import java.time.ZoneOffset
import java.util.*

class TransferFolderProvider(val appSettings: AppSettings) {

    fun getForTransfer(transfer: TransferRecord): Path {
        return if (appSettings.separateTransferFolders) {
            val folderName = MessageFormat(appSettings.transferFolderName).format(
                arrayOf(
                    Date.from(transfer.createdAt!!.toInstant(ZoneOffset.UTC)),
                    transfer.name ?: t("transferFolderProvider.defaultTransferName"))
            )
            val transferPath = Paths.get(appSettings.rootTransferFolder, folderName)
            val file = transferPath.toFile()
            if (!(file.exists() && file.isDirectory)) {
                if (!file.mkdirs()) {
                    throw IllegalStateException(t("transferFolderProvider.getForTransfer.folderCreationFailed"))
                }
            }
            transferPath
        } else {
            Paths.get(appSettings.rootTransferFolder)
        }
    }
}
