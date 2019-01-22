package dropit.domain.entity

import dropit.application.dto.TokenStatus
import java.time.LocalDateTime
import java.util.UUID

data class Phone(
    override val id: UUID? = null,
    override val createdAt: LocalDateTime? = null,
    override val updatedAt: LocalDateTime? = null,
    val name: String? = null,
    val status: TokenStatus? = null,
    val token: UUID? = null,
    val lastConnected: LocalDateTime? = null
) : IEntity