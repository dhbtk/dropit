package dropit.application.controllers

import dropit.application.PhoneSessionService
import dropit.application.currentPhone
import io.javalin.http.Context
import java.nio.file.Files
import java.util.*
import javax.inject.Inject

class DownloadsController @Inject constructor(private val phoneSessionService: PhoneSessionService) : ApplicationController() {
    fun show(context: Context) {
        val file = phoneSessionService.getFileDownload(
            context.currentPhone()!!,
            context.pathParam<UUID>("id").get()
        )
        context.header("Content-Type", Files.probeContentType(file.toPath()))
        context.header("X-File-Name", file.name)
        context.result(file.inputStream())
    }
}
