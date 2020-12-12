package dropit.mobile.domain.service

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.CHANNEL_ID
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import java9.util.concurrent.CompletableFuture

const val FILE_LIST = "fileList"
const val COMPUTER = "computer"
const val TOKEN_REQUEST = "tokenRequest"
const val UPLOAD_FINISHED = "dropit.mobile.FILE_UPLOAD_FINISHED"

class FileUploadService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.sending_file))
                .setContentText("0%")
                .setTicker(getText(R.string.sending_file))
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setProgress(100, 0, false)
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        val fileList = intent!!.getSerializableExtra(FILE_LIST) as List<Pair<FileRequest, String>>
        val computer = intent.getSerializableExtra(COMPUTER) as Computer
        val tokenRequest = intent.getSerializableExtra(TOKEN_REQUEST) as TokenRequest

        val task = FileUploadTask(this, notificationBuilder, fileList, computer, tokenRequest)

        CompletableFuture.runAsync(task)
                .exceptionally { t -> this.onError(t); null }
                .thenRun { this.onSuccess() }

        return START_STICKY
    }

    private fun onSuccess() {
        sendBroadcast(Intent(UPLOAD_FINISHED))
        stopForeground(true)

        NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.file_sent))
                .setContentText(getText(R.string.file_sent_successfully))
                .setTicker(getText(R.string.file_sent_successfully))
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification))
                .setSmallIcon(R.drawable.ic_notification)
                .apply {
                    NotificationManagerCompat.from(this@FileUploadService).notify(NOTIFICATION_ID, build())
                }
    }

    private fun onError(t: Throwable) {
        sendBroadcast(Intent(UPLOAD_FINISHED))
        stopForeground(true)

        t.cause?.printStackTrace()

        Toast.makeText(this, "Upload failed: ${t.cause?.message}", Toast.LENGTH_LONG).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
