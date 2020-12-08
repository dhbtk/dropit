package dropit.application.controllers

import io.javalin.http.Context
import org.jooq.DSLContext
import javax.inject.Inject

class VersionController @Inject constructor() : ApplicationController() {
    fun show(context: Context) {
        context.result("0.1")
    }
}
