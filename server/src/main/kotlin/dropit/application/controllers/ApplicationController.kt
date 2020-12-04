package dropit.application.controllers

import dropit.domain.entity.Phone
import dropit.logger
import io.javalin.http.Context
import org.jooq.DSLContext
import java.util.*

abstract class ApplicationController(val jooq: DSLContext) {
    fun Context.currentPhone(): Phone? = currentPhoneUuid().let { id ->
        jooq.selectFrom(dropit.jooq.tables.Phone.PHONE).where(dropit.jooq.tables.Phone.PHONE.TOKEN.eq(id)).fetchOptionalInto(
            Phone::class.java).orElse(null)
    }

    fun Context.currentPhoneUuid(): UUID? =
        header("Authorization")
            ?.split(" ")?.lastOrNull()?.let {
                try {
                    UUID.fromString(it)
                } catch (e: IllegalArgumentException) {
                    logger.debug("Invalid token provided: $it")
                    null
                }
            }
}
