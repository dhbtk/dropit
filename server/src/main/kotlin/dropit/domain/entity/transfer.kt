package dropit.domain.entity

import dropit.application.dto.FileStatus
import dropit.application.dto.TransferStatus
import java.time.LocalDateTime
import java.util.UUID

data class Transfer(
    override val id: UUID? = null,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
    val name: String? = null,
    val status: TransferStatus? = null,
    val phoneId: UUID? = null,
    val sendToClipboard: Boolean? = false,
    val phone: Phone? = null,
    val files: List<TransferFile> = emptyList()
) : IEntity

data class TransferFile(
    override val id: UUID? = null,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
    val transferId: UUID? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val status: FileStatus? = null,
    val transfer: Transfer? = null
) : IEntity

data class SentFile(
    override val id: UUID? = null,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
    val phoneId: UUID? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSize: Long? = null
) : IEntity

data class FileTransferLog(
    override val id: UUID? = null,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
    val phoneId: UUID? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val source: TransferSource? = null
) : IEntity