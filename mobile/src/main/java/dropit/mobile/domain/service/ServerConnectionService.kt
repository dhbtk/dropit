package dropit.mobile.domain.service

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.support.v4.app.JobIntentService
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.FileProvider
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dropit.application.client.Client
import dropit.application.client.ClientFactory
import dropit.application.dto.DownloadStatus
import dropit.application.dto.SentFileInfo
import dropit.mobile.R
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import java9.util.concurrent.CompletableFuture
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
        val notificationBuilder = NotificationCompat.Builder(this)
            .setContentTitle(String.format(getText(R.string.connected_to_computer).toString(), computer.name))
            .setContentText(getText(R.string.ready_to_receive_data))
            .setTicker(getText(R.string.ready_to_receive_data))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
        val fileDownloader = FileDownloader(this, LinkedBlockingQueue(), client)
        fileDownloader.start()

        client.connectWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                fileDownloader.webSocket = webSocket
                NotificationManagerCompat.from(this@ServerConnectionService)
                    .notify(NOTIFICATION_ID, notificationBuilder.build())
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
        })

        completableFuture.get()
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        fileDownloader.stop()
        Thread.sleep(5000)
        enqueueWork(this, Intent())
    }

    class FileDownloader(val context: Context, val queue: BlockingQueue<SentFileInfo>, val client: Client) {
        var running = true
        var downloadNotificationId = 1000
        val notificationManager = NotificationManagerCompat.from(context)
        lateinit var webSocket: WebSocket
        val runnable = Runnable {
            while (running) {
                val nextFileId = queue.poll(500, TimeUnit.MILLISECONDS)
                if (nextFileId != null) {
                    val (fileId, totalSize) = nextFileId
                    downloadNotificationId += 1
                    try {
                        val progressBuilder = downloadProgressBuilder()
                        notificationManager.notify(downloadNotificationId, progressBuilder.build())

                        var currentPercentage = 0
                        var nanoTime = 0L
                        val response = client.downloadFile(fileId) { read: Long, _: Long ->
                            val newPercentage = ((read.toDouble() / totalSize) * 100).roundToInt()
                            if (newPercentage > currentPercentage) {
                                currentPercentage = newPercentage
                                progressBuilder
                                    .setProgress(100, currentPercentage, false)
                                    .setContentText("$currentPercentage%")
                                notificationManager.notify(downloadNotificationId, progressBuilder.build())

                            }
                            nanoTime = notifyBytes(fileId, read, totalSize, nanoTime)
                        }.blockingFirst()

                        val fileName = response.header("X-File-Name")!!
                        val file = fileForDownload(fileName)

                        response.body()!!.byteStream().use { input ->
                            file.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var read = input.read(buffer)
                                while (read != -1) {
                                    output.write(buffer, 0, read)
                                    read = input.read(buffer)
                                }
                            }
                        }
                        downloadDoneNotification(file, response)
                            .apply { notificationManager.notify(downloadNotificationId, this) }
                        notifyBytes(fileId, totalSize, totalSize, 0L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        NotificationCompat.Builder(context)
                            .setContentTitle(context.getText(R.string.file_download_failed))
                            .setTicker(context.getText(R.string.receiving_file))
                            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .build()
                            .apply { notificationManager.notify(downloadNotificationId, this) }
                    }

                }
            }
        }

        fun start() {
            Thread(runnable).start()
        }

        fun stop() {
            running = false
        }

        private fun notifyBytes(id: UUID, bytes: Long, total: Long, nanoTime: Long): Long {
            val currentTime = System.nanoTime()
            if (currentTime - nanoTime >= (750 * 1000 * 1000) || bytes == total) {
                webSocket.send(ByteString.of(*jacksonObjectMapper().writeValueAsBytes(DownloadStatus(id, bytes))))
                return currentTime
            }
            return nanoTime
        }

        private fun downloadDoneNotification(file: File, response: Response): Notification {
            return NotificationCompat.Builder(context)
                .setContentTitle(String.format(context.getText(R.string.file_received).toString(), file.name))
                .setContentText(context.getText(R.string.click_to_open))
                .setTicker(String.format(context.getText(R.string.file_received).toString(), context.getText(R.string.computer_file)))
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(
                    Intent(Intent.ACTION_VIEW)
                        .apply {
                            setDataAndType(
                                uriFor(file),
                                response.body()?.contentType()?.let { "${it.type()}/${it.subtype()}" })
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        .let { PendingIntent.getActivity(context, 0, it, FLAG_ONE_SHOT) }
                )
                .build()
        }

        private fun uriFor(file: File) = FileProvider.getUriForFile(context, "dropit.mobile.fileprovider", file)

        private fun fileForDownload(fileName: String): File {
            return File(getDownloadDirectory().toString() + "/" + fileName)
                .let {
                    var file = it
                    val name = file.nameWithoutExtension
                    val extension = file.extension
                    var iteration = 0
                    while (file.exists()) {
                        iteration += 1
                        file = File("${getDownloadDirectory()}/$name ($iteration).$extension")
                    }
                    file.createNewFile()
                    file
                }
        }

        private fun downloadProgressBuilder(): NotificationCompat.Builder {
            return NotificationCompat.Builder(context)
                .setContentTitle(context.getText(R.string.receiving_file))
                .setContentText("0%")
                .setTicker(context.getText(R.string.receiving_file))
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setProgress(100, 0, false)
        }

        private fun getDownloadDirectory(): File {
            val directory = File(Environment.getExternalStorageDirectory().toString() + "/DropIt")
            directory.mkdirs()
            return directory
        }
    }

    companion object {
        const val JOB_ID = 1
        const val NOTIFICATION_ID = 1
        const val DOWNLOAD_NOTIFICATION_ID = 2

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ServerConnectionService::class.java, JOB_ID, work)
        }
    }
}