package dropit.infrastructure.fs

import dropit.application.settings.AppSettings
import dropit.domain.entity.Transfer
import dropit.infrastructure.i18n.t
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

class TransferFolderProvider(val appSettings: AppSettings) {

    fun getForTransfer(transfer: Transfer): Path {
        val transferPath = Paths.get(appSettings.settings.rootTransferFolder, transfer.createdAt!!.format(
                DateTimeFormatter.ofPattern(appSettings.settings.transferFolderName)
        ).replaceFirst("%transfer%", transfer.name ?: t("transferFolderProvider.defaultTransferName")))
        val file = transferPath.toFile()
        (file.exists() && file.isDirectory) || file.mkdirs() || throw RuntimeException(t("transferFolderProvider.getForTransfer.folderCreationFailed"))
        return transferPath
    }
}