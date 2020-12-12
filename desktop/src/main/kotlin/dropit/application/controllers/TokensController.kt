package dropit.application.controllers

import dropit.application.currentPhone
import dropit.application.dto.TokenRequest
import dropit.application.model.Phones
import dropit.application.model.tokenResponse
import io.javalin.http.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokensController @Inject constructor(
) : ApplicationController() {
    fun create(context: Context) {
        context.bodyAsClass(TokenRequest::class.java)
            .let { Phones.findOrCreate(it) }
            .also { context.json(it.token.toString()) }
    }

    fun show(context: Context) {
        context.json(context.currentPhone()!!.tokenResponse())
    }
}
