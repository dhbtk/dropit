package dropit.application

import dropit.jooq.tables.records.PhoneRecord
import dropit.logger
import io.javalin.http.Context
import java.util.*

fun Context.currentPhoneUuid(): UUID? {
    return header("Authorization")
        ?.split(" ")?.lastOrNull()?.let {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                logger.debug("Invalid token provided: $it")
                null
            }
        }
}

fun Context.currentPhone(): PhoneRecord? = attribute("currentPhone")
