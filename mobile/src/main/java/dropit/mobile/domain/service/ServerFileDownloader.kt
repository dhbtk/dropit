package dropit.mobile.domain.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dropit.application.client.Client
import dropit.application.dto.DownloadStatus
import dropit.application.dto.SentFileInfo
import dropit.mobile.CHANNEL_ID
import dropit.mobile.R
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import java.io.File
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ServerFileDownloader(val context: Context, val queue: BlockingQueue<SentFileInfo>, val client: Client) {
    private var running = true
    private var downloadNotificationId = 1000
    private val notificationManager = NotificationManagerCompat.from(context)
    lateinit var webSocket: WebSocket

    fun start() {
        CompletableFuture.runAsync { doRun() }
    }

    fun stop() {
        running = false
    }

    private fun doRun() {
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
                    notificationManager.cancel(downloadNotificationId)
                    NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(context.getText(R.string.file_download_failed))
                            .setTicker(context.getText(R.string.receiving_file))
                            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
                            .setSmallIcon(R.drawable.ic_notification)
                            .build()
                            .apply { notificationManager.notify(downloadNotificationId, this) }
                }

            }
        }
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
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(String.format(context.getText(R.string.file_received).toString(), file.name))
            .setContentText(context.getText(R.string.click_to_open))
            .setTicker(String.format(context.getText(R.string.file_received).toString(), context.getText(R.string.computer_file)))
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(
                Intent(Intent.ACTION_VIEW)
                    .apply {
                        setDataAndType(
                            uriFor(file),
                            response.body()?.contentType()?.let { "${it.type()}/${it.subtype()}" })
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    .let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_ONE_SHOT) }
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
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getText(R.string.receiving_file))
            .setContentText("0%")
            .setTicker(context.getText(R.string.receiving_file))
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setProgress(100, 0, false)
    }

    private fun getDownloadDirectory(): File {
        val directory = File(Environment.getExternalStorageDirectory().toString() + "/DropIt")
        directory.mkdirs()
        return directory
    }
}
