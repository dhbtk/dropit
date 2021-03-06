package dropit.mobile.application.connection

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import dropit.application.client.Client
import dropit.application.dto.SentFileInfo
import dropit.mobile.CHANNEL_ID
import dropit.mobile.R
import dropit.mobile.TAG
import dropit.mobile.onMainThread
import java9.util.concurrent.CompletableFuture
import okhttp3.Response
import okhttp3.WebSocket
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ServerFileDownloader(val context: Context, val queue: BlockingQueue<SentFileInfo>, val client: Client) {
    private var running = true
    private val downloadNotificationId = 1000
    private val notificationManager = NotificationManagerCompat.from(context)
    lateinit var webSocket: WebSocket
    private val seenFiles = ArrayList<SentFileInfo>()
    private val attempts = ConcurrentHashMap<SentFileInfo, Int>()

    fun start() {
        CompletableFuture.runAsync { doRun() }
    }

    fun stop() {
        running = false
    }

    private fun doRun() {
        while (running) {
            val nextFileId = queue.poll(500, TimeUnit.MILLISECONDS)
            if (nextFileId != null && attempts.getOrDefault(nextFileId, 0) < 2) {
                if(!seenFiles.contains(nextFileId)) seenFiles.add(nextFileId)
                val currentNotificationId =
                    downloadNotificationId + 3 * seenFiles.indexOf(nextFileId)
                val (fileId, totalSize) = nextFileId
                try {
                    val progressBuilder = downloadProgressBuilder()
                    onMainThread {
                        notificationManager.notify(currentNotificationId, progressBuilder.build())
                    }
                    Log.i(this.TAG, "Downloading file $fileId")
                    var currentPercentage = 0
                    val response = client.downloadFile(fileId) { read: Long, _: Long ->
                        val newPercentage = ((read.toDouble() / totalSize) * 100).roundToInt()
                        if (newPercentage > currentPercentage) {
                            Log.i(this.TAG, "Download at $currentPercentage%")
                            currentPercentage = newPercentage
                            progressBuilder
                                .setProgress(100, currentPercentage, false)
                                .setContentText("$currentPercentage%")
                            onMainThread {
                                notificationManager.notify(
                                    currentNotificationId,
                                    progressBuilder.build()
                                )
                            }
                        }
                    }.blockingFirst()

                    val fileName = response.header("X-File-Name")!!
                    val file = fileForDownload(fileName)

                    Log.i(this.TAG, "Saving filename: $fileName")
                    response.body!!.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read = input.read(buffer)
                            while (read != -1) {
                                output.write(buffer, 0, read)
                                read = input.read(buffer)
                            }
                        }
                    }
                    Log.i(this.TAG, "Saved $fileName")
                    onMainThread {
                        notificationManager.cancel(currentNotificationId)
                        downloadDoneNotification(file, response)
                            .apply { notificationManager.notify(currentNotificationId + 2, this) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onMainThread {
                        notificationManager.cancel(currentNotificationId)
                        NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(context.getText(R.string.file_download_failed))
                            .setTicker(context.getText(R.string.receiving_file))
                            .setLargeIcon(
                                BitmapFactory.decodeResource(
                                    context.resources,
                                    R.drawable.ic_notification
                                )
                            )
                            .setSmallIcon(R.drawable.ic_notification)
                            .build()
                            .apply { notificationManager.notify(currentNotificationId + 1, this) }
                    }
                    attempts.compute(nextFileId) { _, current ->
                        if (current == null) {
                            1
                        } else {
                            current + 1
                        }
                    }
                    queue.add(nextFileId)
                }

            }
        }
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
                            response.body?.contentType()?.let { "${it.type}/${it.subtype}" })
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
