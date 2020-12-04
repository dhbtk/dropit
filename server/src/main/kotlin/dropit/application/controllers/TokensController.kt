package dropit.application.controllers

import dropit.application.dto.TokenRequest
import dropit.domain.service.PhoneService
import io.javalin.http.Context
import org.jooq.DSLContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokensController @Inject constructor(
    jooq: DSLContext,
    private val phoneService: PhoneService
) : ApplicationController(jooq) {
    fun create(context: Context) {
        context.bodyAsClass(TokenRequest::class.java)
            .let { phoneService.requestToken(it) }
            .also { context.json(it) }
    }

    fun show(context: Context) {
        phoneService.getTokenStatus(context.currentPhoneUuid()!!)
            .also { context.json(it) }
    }
}
