package dropit.application.controllers

import dropit.domain.service.IncomingService
import io.javalin.http.Context
import org.eclipse.jetty.http.HttpStatus
import java.util.*
import javax.inject.Inject

class FilesController @Inject constructor(private val incomingService: IncomingService) : ApplicationController() {
    fun update(context: Context) {
        incomingService.receiveFile(
            context.pathParam<UUID>("id").get(),
            context.req
        )
        context.status(HttpStatus.CREATED_201)
    }
}
