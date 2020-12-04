package dropit.application.controllers

import io.javalin.http.Context
import org.jooq.DSLContext
import javax.inject.Inject

class VersionController @Inject constructor(jooq: DSLContext) : ApplicationController(jooq) {
    fun show(context: Context) {
        context.result("0.1")
    }
}
