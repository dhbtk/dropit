package dropit.mobile.application.connection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.android.DaggerService
import dropit.application.client.Client
import dropit.application.dto.SentFileInfo
import dropit.mobile.R
import dropit.mobile.TAG
import dropit.mobile.application.entity.Computer
import dropit.mobile.lib.db.SQLiteHelper
import dropit.mobile.lib.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject

class ServerConnection @Inject constructor(
    private val context: Context,
    private val computer: Computer,
    private val sqLiteHelper: SQLiteHelper,
    private val preferencesHelper: PreferencesHelper,
    private val client: Client,
    private val objectMapper: ObjectMapper
) : WebSocketListener() {
    private lateinit var fileDownloader: ServerFileDownloader
    private lateinit var startCallback: (computer: Computer) -> Unit
    private lateinit var stopCallback: () -> Unit
    private lateinit var restartCallback: () -> Unit

    fun doConnect(startCallback: (computer: Computer) -> Unit, stopCallback: () -> Unit, restartCallback: () -> Unit) {
        this.startCallback = startCallback
        this.stopCallback = stopCallback
        this.restartCallback = restartCallback
        Log.d(this.TAG, "doConnect: computer=$computer")
        fileDownloader = ServerFileDownloader(context, LinkedBlockingQueue(), client).apply { start() }
        fileDownloader.webSocket = client.connectWebSocket(this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        onMainThread { startCallback(computer) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(this.TAG, "onClosed: code=$code, reason=$reason")
        fileDownloader.stop()
        restartCallback()
    }

    private fun deleteComputerAndStop() {
        sqLiteHelper.deleteComputer(computer)
        preferencesHelper.currentComputerId = null
        fileDownloader.stop()
        onMainThread(stopCallback)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(this.TAG, "onFailure: t=${t.message} response=$response")
        t.printStackTrace()
        if (response != null) {
            onMainThread(::deleteComputerAndStop)
        } else {
            fileDownloader.stop()
            onMainThread(restartCallback)
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(this.TAG, "onStringMessage: text=$text")
        onMainThread {
            val clipboard = context.getSystemService(DaggerService.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Clipboard text from ${computer.name}", text))
            Toast.makeText(context, R.string.received_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d(this.TAG, "onByteMessage: bytes=${bytes.utf8()}")
        try {
            val data = objectMapper.readValue<SentFileInfo>(bytes.toByteArray())
            fileDownloader.queue.put(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}