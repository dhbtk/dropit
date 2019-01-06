package dropit.application.dto

import java.io.Serializable
import java.util.UUID

data class TransferRequest(
    val name: String? = null,
    val sendToClipboard: Boolean? = false,
    val files: List<FileRequest> = emptyList()
) : Serializable

data class FileRequest(
    val id: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSize: Long? = null
) : Serializable

enum class TransferStatus {
    PENDING, FINISHED
}

enum class FileStatus {
    PENDING, FAILED, FINISHED
}

data class SentFileInfo(val id: UUID = UUID.randomUUID(), val size: Long = 0L)

data class DownloadStatus(val id: UUID = UUID.randomUUID(), val bytes: Long = 0L)