package dropit.application.controllers

import dropit.application.PhoneSessions
import dropit.application.currentPhone
import io.javalin.http.Context
import java.nio.file.Files
import java.util.*
import javax.inject.Inject

class DownloadsController @Inject constructor(private val phoneSessions: PhoneSessions) :
    ApplicationController() {
    fun beforeShow(context: Context) {

    }

    fun afterShow(context: Context) {
        phoneSessions.recordUploadFinished(
            context.currentPhone()!!,
            context.pathParam<UUID>("id").get()
        )
    }

    fun show(context: Context) {
        val file = phoneSessions.getFileDownload(
            context.currentPhone()!!,
            context.pathParam<UUID>("id").get()
        )
        context.header("Content-Type", Files.probeContentType(file.toPath()))
        context.header("X-File-Name", file.name)
        context.result(file.inputStream())
    }
}
