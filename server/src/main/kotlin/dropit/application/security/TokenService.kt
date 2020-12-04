package dropit.application.security

import dropit.application.dto.TokenStatus
import dropit.domain.entity.Phone
import dropit.jooq.tables.Phone.Companion.PHONE
import dropit.logger
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.UnauthorizedResponse
import org.jooq.DSLContext
import java.lang.IllegalArgumentException
import java.util.*
import javax.inject.Inject

class TokenService @Inject constructor(val jooq: DSLContext) {

    fun getApprovedPhone(context: Context): Phone {
        return jooq.select().from(PHONE).where(PHONE.TOKEN.eq(getToken(context)))
            .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED))
            .fetchOptionalInto(Phone::class.java)
            .orElseThrow { ForbiddenResponse() }
    }

    fun getPendingPhone(context: Context): Phone {
        return jooq.select().from(PHONE).where(PHONE.TOKEN.eq(getToken(context)))
            .and(PHONE.STATUS.`in`(listOf(TokenStatus.AUTHORIZED, TokenStatus.PENDING)))
            .fetchOptionalInto(Phone::class.java)
            .orElseThrow { ForbiddenResponse() }
    }

    private fun getToken(context: Context): UUID {
        return context.header("Authorization")
            ?.split(" ")?.let { list ->
                if (list.size > 1) {
                    UUID.fromString(list[1])
                } else {
                    null
                }
            } ?: throw UnauthorizedResponse()
    }


}
