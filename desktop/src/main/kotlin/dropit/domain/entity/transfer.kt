package dropit.domain.entity

import dropit.application.dto.FileStatus
import dropit.application.dto.TransferStatus
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class Transfer(
        override val id: UUID? = null,
        override val createdAt: LocalDateTime? = null,
        override val updatedAt: LocalDateTime? = null,
        val name: String? = null,
        val status: TransferStatus? = null,
        val phoneId: UUID? = null,
        val phone: Phone? = null,
        val files: List<TransferFile> = emptyList()
) : IEntity {
    fun transferFolderName(template: String): String {
        return MessageFormat(template).format(
            arrayOf(Date.from(createdAt!!.toInstant(ZoneOffset.UTC)), name!!)
        )
    }
}

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
