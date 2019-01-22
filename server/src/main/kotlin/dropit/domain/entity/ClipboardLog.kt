package dropit.domain.entity

import java.time.LocalDateTime
import java.util.UUID

data class ClipboardLog(
    override val id: UUID? = null,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
    val content: String? = null,
    val source: TransferSource? = null
) : IEntity

enum class TransferSource {
    PHONE, COMPUTER
}