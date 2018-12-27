package dropit.mobile.domain.service

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.app.JobIntentService
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import kotlin.math.roundToInt

const val FILE_LIST = "fileList"
const val COMPUTER = "computer"
const val TOKEN_REQUEST = "tokenRequest"

class FileUploadService : JobIntentService() {
    companion object {
        const val JOB_ID = 1
        const val NOTIFICATION_ID = 1

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, FileUploadService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val fileList = intent.getSerializableExtra(FILE_LIST) as List<Pair<FileRequest, String>>
        val notificationBuilder = NotificationCompat.Builder(this)
            .setContentTitle(getText(R.string.sending_file))
            .setContentText("0%")
            .setTicker(getText(R.string.sending_file))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setProgress(100, 0, false)
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        val computer = intent.getSerializableExtra(COMPUTER) as Computer
        val tokenRequest = intent.getSerializableExtra(TOKEN_REQUEST) as TokenRequest

        val client = ClientFactory(ObjectMapper().apply { findAndRegisterModules() }).create(
            computer.url,
            tokenRequest,
            computer.token?.toString()
        )

        val totalBytes = fileList.map { it.first.fileSize!! }.sum()
        var uploadedBytes = 0L
        var currentPercentage = 0

        fileList.forEach { (fileRequest, uri) ->
            client.uploadFile(fileRequest, contentResolver.openInputStream(Uri.parse(uri))) { uploaded ->
                uploadedBytes += uploaded
                val newPercentage = ((uploadedBytes.toDouble() / totalBytes) * 100).roundToInt()
                if (newPercentage > currentPercentage) {
                    currentPercentage = newPercentage
                    notificationBuilder
                        .setProgress(100, currentPercentage, false)
                        .setContentText("$currentPercentage%")
                    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
                }
            }.blockingFirst()
        }

        stopForeground(true)

        NotificationCompat.Builder(this)
            .setContentTitle(getText(R.string.file_sent))
            .setContentText(getText(R.string.file_sent_successfully))
            .setTicker(getText(R.string.file_sent_successfully))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply {
                NotificationManagerCompat.from(this@FileUploadService).notify(NOTIFICATION_ID, build())
            }
    }
}