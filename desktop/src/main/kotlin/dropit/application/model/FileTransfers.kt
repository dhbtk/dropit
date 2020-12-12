package dropit.application.model

import dropit.Application.component
import dropit.application.dto.FileStatus
import dropit.application.dto.TransferStatus
import dropit.infrastructure.event.AppEvent
import dropit.jooq.tables.records.TransferFileRecord
import dropit.jooq.tables.records.TransferRecord
import dropit.jooq.tables.references.TRANSFER
import dropit.jooq.tables.references.TRANSFER_FILE
import dropit.logger
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.ProgressListener
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.util.*
import javax.servlet.http.HttpServletRequest

object FileTransfers : ApplicationModel() {
    val transferTimes = HashMap<TransferFileRecord, MutableList<Pair<LocalDateTime, Long>>>()
    private const val TRANSFER_CALC_INTERVAL = 5

    init {
        component.inject(this)

        jooq.transaction { _ ->
            jooq.deleteFrom(TRANSFER_FILE).where(TRANSFER_FILE.STATUS.eq(FileStatus.PENDING)).execute()
            jooq.deleteFrom(TRANSFER).where(TRANSFER.STATUS.eq(TransferStatus.PENDING)).execute()
        }
    }

    data class CompletedFileTransfer(
        val transferFile: TransferFileRecord,
        val file: File
    )

    data class CompletedTransfer(
        val transfer: TransferRecord,
        val locations: Map<UUID, File>
    )

    data class NewTransferEvent(override val payload: TransferRecord) : AppEvent<TransferRecord>
    data class DownloadStartedEvent(override val payload: TransferFileRecord) : AppEvent<TransferFileRecord>
    data class DownloadProgressEvent(override val payload: Pair<TransferFileRecord, Long>) :
        AppEvent<Pair<TransferFileRecord, Long>>

    data class DownloadFinishEvent(override val payload: CompletedFileTransfer) : AppEvent<CompletedFileTransfer>
    data class TransferCompleteEvent(override val payload: CompletedTransfer) : AppEvent<CompletedTransfer>
    data class ClipboardReceiveEvent(override val payload: String) : AppEvent<String>

    fun receive(fileId: UUID, request: HttpServletRequest) {
        val transferFile = jooq.fetchOne(TRANSFER_FILE, TRANSFER_FILE.ID.eq(fileId))!!
        bus.broadcast(DownloadStartedEvent(transferFile))
        try {
            val receivedFile = saveTransferFile(transferFile, receiveWithProgress(transferFile, request))
            transferFile.status = FileStatus.FINISHED
            transferFile.update()

            DownloadFinishEvent(CompletedFileTransfer(transferFile, receivedFile)).let(bus::broadcast)
            notifyTransferFinished(transferFile)
        } catch (exception: IOException) {
            logger.error("Failure receiving file: ${exception.message}", exception)
            transferFile.status = FileStatus.PENDING
            transferFile.update()
        }
    }

    private fun receiveWithProgress(transferFile: TransferFileRecord, request: HttpServletRequest): File {
        val updater = ProgressUpdater(transferFile).also { Thread(it).start() }
        val upload = ServletFileUpload().apply { progressListener = FileProgressListener(updater) }
        return tempFile(transferFile).also { saveFileUpload(it, upload, request) }
    }

    private fun tempFile(transferFile: TransferFileRecord): File {
        return File.createTempFile(
            transferFile.fileName!!,
            ".part",
            transferFolderProvider.getForTransfer(transferFile.transfer).toFile()
        )
    }

    private fun saveTransferFile(transferFile: TransferFileRecord, tempFile: File): File {
        return transferFile.transfer.folder.resolve(transferFile.fileName!!).toFile().apply {
            if (exists()) delete()
            tempFile.renameTo(this)
        }
    }

    class ProgressUpdater(private val transferFile: TransferFileRecord) : Runnable {
        var finished = false
        var currentBytesRead = 0L

        override fun run() {
            while (!finished) {
                Thread.sleep(250)
                notifyProgress(currentBytesRead)
            }
            transferTimes.remove(transferFile)
        }

        fun notifyProgress(read: Long) {
            bus.broadcast(DownloadProgressEvent(Pair(transferFile, read)))
            transferTimes[transferFile]?.add(Pair(LocalDateTime.now(), read))
        }
    }

    class FileProgressListener(private val updater: ProgressUpdater) : ProgressListener {
        override fun update(pBytesRead: Long, pContentLength: Long, pItems: Int) {
            if (pBytesRead == pContentLength) {
                updater.notifyProgress(pBytesRead)
                updater.finished = true
            } else {
                updater.currentBytesRead = pBytesRead
            }
        }
    }

    private fun notifyTransferFinished(transferFile: TransferFileRecord) {
        val transfer = transferFile.transfer
        val transferFolder = transfer.folder.toFile()
        if (!transfer.filesFinished) return

        transfer.status = TransferStatus.FINISHED
        transfer.update()
        val locations = transfer.files.map { it.id!! to transferFolder.resolve(it.fileName!!) }.toMap()
        TransferCompleteEvent(CompletedTransfer(transfer, locations)).let(bus::broadcast)
    }

    private fun saveFileUpload(tempFile: File, upload: ServletFileUpload, request: HttpServletRequest) {
        tempFile.outputStream().use { outputStream ->
            uploadInputStream(upload.getItemIterator(request)).use { inputStream ->
                IOUtils.copy(inputStream, outputStream)
            }
        }
    }

    private fun uploadInputStream(iterator: FileItemIterator): InputStream {
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.fieldName == "file") return item.openStream()
        }
        throw IllegalArgumentException("No file upload")
    }
}
