package dropit.application.model

import dropit.jooq.tables.records.PhoneRecord
import io.javalin.core.security.Role

fun PhoneRecord.role(): PhoneRole = status?.let { PhoneRole.valueOf(it.name) } ?: PhoneRole.PENDING

enum class PhoneRole : Role {
    PENDING, DENIED, AUTHORIZED
}
