package dropit.application.dto

data class TransferRequest(val name: String? = null, val files: List<FileRequest> = emptyList())

data class FileRequest(
        val id: String? = null,
        val fileName: String? = null,
        val mimeType: String? = null,
        val fileSize: Long? = null)

data class PendingTransfer(val id: String? = null, val items: List<String> = emptyList())

data class TransferInfo(val status: TransferStatus? = null, val files: Map<String, FileStatus> = emptyMap())

enum class TransferStatus {
    PENDING, FINISHED
}

enum class FileStatus {
    PENDING, FAILED, FINISHED
}