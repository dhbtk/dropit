package dropit.application.controllers

import dropit.application.model.Clipboard
import io.javalin.http.Context
import org.eclipse.jetty.http.HttpStatus
import javax.inject.Inject

class ClipboardsController @Inject constructor() : ApplicationController() {
    fun create(context: Context) {
        Clipboard.receive(context.bodyAsClass(String::class.java))
        context.status(HttpStatus.CREATED_201)
    }
}
