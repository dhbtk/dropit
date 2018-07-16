package dropit.application.dto

import java.io.Serializable

enum class TokenStatus {
    PENDING, DENIED, AUTHORIZED
}

data class TokenRequest(val id: String? = null, val name: String? = null) : Serializable
