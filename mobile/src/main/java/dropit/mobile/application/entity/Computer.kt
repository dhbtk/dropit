package dropit.mobile.application.entity

import dropit.application.dto.TokenStatus
import java.io.Serializable
import java.util.*

data class Computer(
    val id: UUID,
    val secret: UUID?,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val token: UUID?,
    val tokenStatus: TokenStatus?,
    val contacted: Boolean = false,
    val default: Boolean = false
) : Serializable {
    val url
        get() = "https://$ipAddress:$port"
}