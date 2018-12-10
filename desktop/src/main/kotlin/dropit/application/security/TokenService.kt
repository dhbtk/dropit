package dropit.application.security

import dropit.application.dto.TokenStatus
import dropit.domain.entity.Phone
import dropit.domain.service.UnauthorizedException
import dropit.jooq.tables.Phone.PHONE
import io.javalin.Context
import org.jooq.DSLContext
import javax.inject.Inject

class TokenService @Inject constructor(val jooq: DSLContext) {

    fun getApprovedPhone(context: Context): Phone {
        return jooq.select().from(PHONE).where(PHONE.TOKEN.eq(getToken(context)))
                .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED.name))
                .fetchOptionalInto(Phone::class.java)
                .orElseThrow { UnauthorizedException() }
    }

    fun getPendingPhone(context: Context): Phone {
        return jooq.select().from(PHONE).where(PHONE.TOKEN.eq(getToken(context)))
                .and(PHONE.STATUS.`in`(listOf(TokenStatus.AUTHORIZED.name, TokenStatus.PENDING.name)))
                .fetchOptionalInto(Phone::class.java)
                .orElseThrow { UnauthorizedException() }
    }

    private fun getToken(context: Context): String {
        return context.header("Authorization")
                ?.split("\\s+")?.get(1) ?: throw UnauthorizedException()
    }
}