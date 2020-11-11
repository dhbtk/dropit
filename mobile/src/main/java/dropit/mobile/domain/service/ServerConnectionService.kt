package dropit.mobile.domain.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dropit.application.client.ClientFactory
import dropit.application.dto.SentFileInfo
import dropit.mobile.CHANNEL_ID
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import java9.util.concurrent.CompletableFuture
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.LinkedBlockingQueue

class ServerConnectionService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        val preferencesHelper = PreferencesHelper(this)
        val sqLiteHelper = SQLiteHelper(this)
        val computerId = preferencesHelper.currentComputerId ?: return
        val computer = sqLiteHelper.getComputer(computerId)
        val tokenRequest = preferencesHelper.tokenRequest
        val objectMapper = ObjectMapper().apply { findAndRegisterModules() }
        val client = ClientFactory(objectMapper)
            .create(computer.url, tokenRequest, computer.token?.toString())
        val completableFuture = CompletableFuture<Boolean>()
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(String.format(getText(R.string.connected_to_computer).toString(), computer.name))
            .setContentText(getText(R.string.ready_to_receive_data))
            .setTicker(getText(R.string.ready_to_receive_data))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        val fileDownloader = ServerFileDownloader(this, LinkedBlockingQueue(), client)
        fileDownloader.start()

        client.connectWebSocket(ServerWebSocketListener(this, fileDownloader, completableFuture, computer, objectMapper))

        completableFuture.get()
        stopForeground(NOTIFICATION_ID)
        fileDownloader.stop()
    }

    class ServerWebSocketListener(private val context: Context, private val fileDownloader: ServerFileDownloader, private val completableFuture: CompletableFuture<Boolean>, private val computer: Computer, private val objectMapper: ObjectMapper) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            fileDownloader.webSocket = webSocket
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            t.printStackTrace()
            completableFuture.complete(false)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            completableFuture.complete(true)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            onMainThread {
                val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Clipboard text from ${computer.name}", text))
                Toast.makeText(context, R.string.received_clipboard, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            try {
                val data = objectMapper.readValue<SentFileInfo>(bytes.toByteArray())
                fileDownloader.queue.put(data)
            } catch (e: Exception) {

            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            completableFuture.complete(true)
            super.onClosed(webSocket, code, reason)
        }
    }

    companion object {
        const val JOB_ID = 1
        const val NOTIFICATION_ID = 7
        const val DOWNLOAD_NOTIFICATION_ID = 2

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ServerConnectionService::class.java, JOB_ID, work)
        }
    }
}
