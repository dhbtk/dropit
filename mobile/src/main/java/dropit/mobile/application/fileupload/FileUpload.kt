package dropit.mobile.application.fileupload

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dropit.application.client.Client
import dropit.application.dto.TransferRequest
import dropit.mobile.TAG
import dropit.mobile.onMainThread
import kotlin.math.roundToInt

class FileUpload(
    private val context: Context,
    private val client: Client,
    uris: List<Uri>,
    private val notificationBuilder: NotificationCompat.Builder
) : Runnable {
    private val fileInfo = uris.map { FileInfo.fromUri(context.contentResolver, it) }
    private val totalBytes = fileInfo.map { it.fileSize }.sum()
    private var uploadedBytes = 0L
    private var currentPercentage = 0

    override fun run() {
        val transferId = client.createTransfer(
            TransferRequest(
                "Transfer",
                false,
                fileInfo.map { it.fileRequest })
        ).blockingFirst()
        Log.d(this.TAG, "transferId: $transferId")

        fileInfo.forEach(::uploadFile)
    }

    private fun uploadFile(fileInfo: FileInfo) {
        val inputStream = context.contentResolver.openInputStream(fileInfo.uri)!!
        client.uploadFile(fileInfo.fileRequest, inputStream, ::notifyProgress).blockingAwait()
    }

    private fun notifyProgress(uploaded: Long) {
        uploadedBytes += uploaded
        val newPercentage = ((uploadedBytes.toDouble() / totalBytes) * 100).roundToInt()
        if (newPercentage > currentPercentage) {
            currentPercentage = newPercentage
            updateNotification()
        }
    }

    private fun updateNotification() {
        onMainThread {
            notificationBuilder
                .setProgress(100, currentPercentage, false)
                .setContentText("$currentPercentage%")
            NotificationManagerCompat.from(context)
                .notify(UploadNotifications.START_NOTIFICATION_ID, notificationBuilder.build())
        }
    }
}