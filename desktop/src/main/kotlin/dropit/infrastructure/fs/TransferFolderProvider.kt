package dropit.infrastructure.fs

import dropit.application.settings.AppSettings
import dropit.domain.entity.Transfer
import dropit.infrastructure.i18n.t
import java.nio.file.Path
import java.nio.file.Paths
import java.text.MessageFormat
import java.time.ZoneOffset
import java.util.*

class TransferFolderProvider(val appSettings: AppSettings) {

    fun getForTransfer(transfer: Transfer): Path {
        return if (appSettings.settings.separateTransferFolders) {
            val folderName = MessageFormat(appSettings.settings.transferFolderName).format(
                arrayOf(
                    Date.from(transfer.createdAt!!.toInstant(ZoneOffset.UTC)),
                    transfer.name ?: t("transferFolderProvider.defaultTransferName"))
            )
            val transferPath = Paths.get(appSettings.settings.rootTransferFolder, folderName)
            val file = transferPath.toFile()
            (file.exists() && file.isDirectory) || file.mkdirs() || throw RuntimeException(t("transferFolderProvider.getForTransfer.folderCreationFailed"))
            transferPath
        } else {
            Paths.get(appSettings.settings.rootTransferFolder)
        }
    }
}