package dropit.mobile.domain.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.app.JobIntentService
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.mobile.R
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import java9.util.concurrent.CompletableFuture
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ServerConnectionService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        val preferencesHelper = PreferencesHelper(this)
        val sqLiteHelper = SQLiteHelper(this)
        val computerId = preferencesHelper.currentComputerId ?: return
        val computer = sqLiteHelper.getComputer(computerId)
        val tokenRequest = preferencesHelper.tokenRequest
        val client = ClientFactory(ObjectMapper().apply { findAndRegisterModules() })
            .create(computer.url, tokenRequest, computer.token?.toString())
        val completableFuture = CompletableFuture<Boolean>()
        val notificationBuilder = NotificationCompat.Builder(this)
            .setContentTitle(String.format(getText(R.string.connected_to_computer).toString(), computer.name))
            .setContentText(getText(R.string.ready_to_receive_data))
            .setTicker(getText(R.string.ready_to_receive_data))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)

        client.connectWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                startForeground(NOTIFICATION_ID, notificationBuilder.build())
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
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip = ClipData.newPlainText("Clipboard text from ${computer.name}", text)
                    Toast.makeText(this@ServerConnectionService, R.string.received_clipboard, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                completableFuture.complete(true)
                super.onClosed(webSocket, code, reason)
            }
        })

        completableFuture.get()
        stopForeground(true)
        Thread.sleep(5000)
        enqueueWork(this, Intent())
    }

    companion object {
        const val JOB_ID = 1
        const val NOTIFICATION_ID = 1

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ServerConnectionService::class.java, JOB_ID, work)
        }
    }
}