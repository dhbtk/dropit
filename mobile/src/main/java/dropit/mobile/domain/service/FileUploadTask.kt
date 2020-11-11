package dropit.mobile.domain.service

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.domain.entity.Computer
import kotlin.math.roundToInt

class FileUploadTask(
        private val context: Context,
        private val notificationBuilder: NotificationCompat.Builder,
        private val fileList: List<Pair<FileRequest, String>>,
        computer: Computer,
        tokenRequest: TokenRequest
        ) : Runnable {

    val client = ClientFactory(ObjectMapper().apply { findAndRegisterModules() }).create(
            computer.url,
            tokenRequest,
            computer.token?.toString()
    )

    private val totalBytes = fileList.map { it.first.fileSize!! }.sum()


    override fun run() {
        var uploadedBytes = 0L
        var currentPercentage = 0

        fileList.forEach { (fileRequest, uri) ->
            client.uploadFile(fileRequest, context.contentResolver.openInputStream(Uri.parse(uri))!!) { uploaded ->
                uploadedBytes += uploaded
                val newPercentage = ((uploadedBytes.toDouble() / totalBytes) * 100).roundToInt()
                if (newPercentage > currentPercentage) {
                    currentPercentage = newPercentage
                    notificationBuilder
                            .setProgress(100, currentPercentage, false)
                            .setContentText("$currentPercentage%")
                    NotificationManagerCompat.from(context).notify(FileUploadService.NOTIFICATION_ID, notificationBuilder.build())
                }
            }.blockingFirst()
        }
    }
}
