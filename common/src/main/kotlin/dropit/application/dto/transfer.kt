package dropit.application.dto

import java.io.Serializable

data class TransferRequest(val name: String? = null, val files: List<FileRequest> = emptyList()) : Serializable

data class FileRequest(
        val id: String? = null,
        val fileName: String? = null,
        val mimeType: String? = null,
        val fileSize: Long? = null) : Serializable

data class PendingTransfer(val id: String? = null, val items: List<String> = emptyList()) : Serializable

data class TransferInfo(val status: TransferStatus? = null, val files: Map<String, FileStatus> = emptyMap()) : Serializable

enum class TransferStatus {
    PENDING, FINISHED
}

enum class FileStatus {
    PENDING, FAILED, FINISHED
}