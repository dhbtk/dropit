package dropit.application.model

import io.javalin.core.security.Role

enum class PhoneRole : Role {
    PENDING, DENIED, AUTHORIZED
}
