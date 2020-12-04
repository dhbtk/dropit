package dropit.application.dto

import java.io.Serializable
import java.util.*

enum class TokenStatus {
    PENDING, DENIED, AUTHORIZED
}

data class TokenRequest(val id: UUID? = null, val name: String? = null) : Serializable

data class TokenResponse(val status: TokenStatus? = null, val computerSecret: UUID? = null) : Serializable
