package dropit.domain.entity

import java.time.LocalDateTime
import java.util.UUID

interface IEntity {
    val id: UUID?
    val createdAt: LocalDateTime?
    val updatedAt: LocalDateTime?
}