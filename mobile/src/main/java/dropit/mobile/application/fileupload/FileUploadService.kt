package dropit.mobile.application.fileupload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import dagger.android.DaggerService
import dropit.application.client.Client
import dropit.mobile.onMainThread
import java9.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Provider

class FileUploadService : DaggerService() {
    @Inject
    lateinit var clientProvider: Provider<Client>

    @Inject
    lateinit var uploadNotifications: UploadNotifications

    companion object {
        const val NOTIFICATION_ID = 1
        const val FILE_LIST = "fileList"
        const val COMPUTER = "computer"
        const val TOKEN_REQUEST = "tokenRequest"
        const val URI = "uri"
        const val UPLOAD_FINISHED = "dropit.mobile.FILE_UPLOAD_FINISHED"

        fun start(context: Context, uris: ArrayList<Uri>) {
            Intent(context, FileUploadService::class.java).also { intent ->
                intent.putParcelableArrayListExtra(URI, uris)
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            UploadNotifications.START_NOTIFICATION_ID,
            uploadNotifications.startNotification.build()
        )
        val files = requireNotNull(requireNotNull(intent).getParcelableArrayListExtra<Uri>(URI))
        FileUpload(this, clientProvider.get(), files, uploadNotifications.startNotification)
            .let { CompletableFuture.runAsync(it) }
            .exceptionally { t -> onMainThread { onError(t) }; null }
            .thenRun { onMainThread { onSuccess() } }

        return START_STICKY
    }

    private fun onSuccess() {
        sendBroadcast(Intent(UPLOAD_FINISHED))
        stopForeground(true)

        uploadNotifications.showSuccessNotification()
    }

    private fun onError(t: Throwable) {
        sendBroadcast(Intent(UPLOAD_FINISHED))
        stopForeground(true)

        t.cause?.printStackTrace()

        Toast.makeText(this, "Upload failed: ${t.cause?.message}", Toast.LENGTH_LONG).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}