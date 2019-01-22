package dropit.domain.entity

import java.util.UUID

data class Settings(
    val computerId: UUID = UUID.randomUUID(),
    val computerSecret: UUID = UUID.randomUUID(),
    val computerName: String,
    val transferFolderName: String,
    val rootTransferFolder: String,
    val serverPort: Int = 58992,
    val currentPhoneId: UUID? = null,
    // actual app settings
    val separateTransferFolders: Boolean = true,
    val openTransferOnCompletion: Boolean = true,
    val showTransferAction: ShowFileAction = ShowFileAction.OPEN_FILE,
    val logClipboardTransfers: Boolean = true
)

data class FileTypeSettings(
    var mimeType: String? = null,
    var showAction: ShowFileAction? = ShowFileAction.OPEN_FOLDER,
    var clipboardDestination: ClipboardFileDestination? = ClipboardFileDestination.TRANSFER_FOLDER
)

enum class ShowFileAction {
    OPEN_FILE, OPEN_FOLDER
}

enum class ClipboardFileDestination {
    TRANSFER_FOLDER, TEMP_FOLDER
}