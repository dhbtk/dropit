package dropit.application.security

import dropit.application.dto.TokenStatus
import dropit.domain.entity.Phone
import dropit.jooq.tables.Phone.PHONE
import io.javalin.Context
import io.javalin.ForbiddenResponse
import io.javalin.UnauthorizedResponse
import org.jooq.DSLContext
import javax.inject.Inject

class TokenService @Inject constructor(val jooq: DSLContext) {

    fun getApprovedPhone(context: Context): Phone {
        return jooq.select().from(PHONE).where(PHONE.TOKEN.eq(getToken(context)))
                .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED.name))
                .fetchOptionalInto(Phone::class.java)
            .orElseThrow { ForbiddenResponse() }
    }

    fun getPendingPhone(context: Context): Phone {
        return jooq.select().from(PHONE).where(PHONE.TOKEN.eq(getToken(context)))
                .and(PHONE.STATUS.`in`(listOf(TokenStatus.AUTHORIZED.name, TokenStatus.PENDING.name)))
                .fetchOptionalInto(Phone::class.java)
            .orElseThrow { ForbiddenResponse() }
    }

    private fun getToken(context: Context): String {
        try {
            return context.header("Authorization")
                ?.split(" ")?.get(1) ?: throw UnauthorizedResponse()
        } catch (e: Exception) {
            throw UnauthorizedResponse()
        }
    }
}