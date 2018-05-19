package dropit.domain.service

class UnauthorizedException(message: String?) : RuntimeException(message) {
    constructor() : this(null)
}