package dropit.application.controllers

import dropit.application.model.FileTransfers
import io.javalin.http.Context
import org.eclipse.jetty.http.HttpStatus
import java.util.*
import javax.inject.Inject

class FilesController @Inject constructor() : ApplicationController() {
    fun update(context: Context) {
        FileTransfers.receive(
            context.pathParam<UUID>("id").get(),
            context.req
        )
        context.status(HttpStatus.CREATED_201)
    }
}
