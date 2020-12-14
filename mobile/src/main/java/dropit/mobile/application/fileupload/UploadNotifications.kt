package dropit.mobile.application.fileupload

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dropit.mobile.CHANNEL_ID
import dropit.mobile.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadNotifications @Inject constructor(val context: Context) {
    val startNotification by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getText(R.string.sending_file))
            .setContentText("0%")
            .setTicker(context.getText(R.string.sending_file))
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_notification
                )
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setProgress(100, 0, false)
    }
    val successNotification by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getText(R.string.file_sent))
            .setContentText(context.getText(R.string.file_sent_successfully))
            .setTicker(context.getText(R.string.file_sent_successfully))
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_notification
                )
            )
            .setSmallIcon(R.drawable.ic_notification)
    }

    fun showSuccessNotification() {
        NotificationManagerCompat.from(context)
            .notify(SUCCESS_NOTIFICATION_ID, successNotification.build())
    }

    companion object {
        const val START_NOTIFICATION_ID = 209
        const val SUCCESS_NOTIFICATION_ID = 559
    }
}