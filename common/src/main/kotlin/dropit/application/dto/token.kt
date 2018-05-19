package dropit.application.dto

enum class TokenStatus {
    PENDING, DENIED, AUTHORIZED
}

data class TokenRequest(val id: String? = null, val name: String? = null)
