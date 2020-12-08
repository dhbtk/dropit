package dropit.domain.service

import dropit.application.dto.FileStatus
import dropit.application.dto.TransferRequest
import dropit.application.dto.TransferStatus
import dropit.application.model.transfer
import dropit.application.settings.AppSettings
import dropit.application.model.TransferSource
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.fs.TransferFolderProvider
import dropit.jooq.tables.ClipboardLog
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.records.TransferFileRecord
import dropit.jooq.tables.records.TransferRecord
import dropit.jooq.tables.references.TRANSFER
import dropit.jooq.tables.references.TRANSFER_FILE
import dropit.logger
import org.apache.commons.fileupload.ProgressListener
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.IOUtils
import org.jooq.DSLContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import kotlin.NoSuchElementException

@Singleton
class IncomingService @Inject constructor(
    val jooq: DSLContext,
    val settings: AppSettings,
    val bus: EventBus,
    val appSettings: AppSettings,
    val transferFolderProvider: TransferFolderProvider
) {
    val transferTimes = HashMap<TransferFileRecord, List<Pair<LocalDateTime, Long>>>()

    init {
        jooq.transaction { _ ->
            jooq.deleteFrom(TRANSFER_FILE).where(TRANSFER_FILE.STATUS.eq(FileStatus.PENDING)).execute()
            jooq.deleteFrom(TRANSFER).where(TRANSFER.STATUS.eq(TransferStatus.PENDING)).execute()
        }
    }

    data class CompletedFileTransfer(
        val transferFile: TransferFileRecord,
        val transferFolder: File,
        val file: File
    )

    data class CompletedTransfer(
        val transfer: TransferRecord,
        val transferFolder: File,
        val locations: Map<UUID, File>
    )

    data class NewTransferEvent(override val payload: TransferRecord) : AppEvent<TransferRecord>
    data class DownloadStartedEvent(override val payload: TransferFileRecord) : AppEvent<TransferFileRecord>
    data class DownloadProgressEvent(override val payload: Pair<TransferFileRecord, Long>)
        : AppEvent<Pair<TransferFileRecord, Long>>

    data class DownloadFinishEvent(override val payload: CompletedFileTransfer) : AppEvent<CompletedFileTransfer>
    data class TransferCompleteEvent(override val payload: CompletedTransfer) : AppEvent<CompletedTransfer>
    data class ClipboardReceiveEvent(override val payload: String) : AppEvent<String>

    /**
     * Called from web
     *
     * uploads a file, notifying the UI of progress.
     */
    fun receiveFile(fileId: UUID, request: HttpServletRequest) {
        val transferFile = jooq.fetchOne(TRANSFER_FILE, TRANSFER_FILE.ID.eq(fileId))!!
        val record = jooq.newRecord(TRANSFER_FILE)
        val transfer = transferFile.transfer
        val transferFolder = transferFolderProvider.getForTransfer(transfer).toFile()
        val tempFile = transferFolder.toPath().resolve(transferFile.fileName + ".part").toFile()
        !tempFile.exists() || (tempFile.delete() && tempFile.createNewFile())
        bus.broadcast(DownloadStartedEvent(transferFile))
        val transferList = mutableListOf(Pair(LocalDateTime.now(), 0L))
        transferTimes[transferFile] = transferList
        try {
            val upload = ServletFileUpload()
            var currentBytesRead: Long = 0
            var finished = false
            Runnable {
                while (!finished) {
                    Thread.sleep(250)
                    val read = currentBytesRead
                    bus.broadcast(DownloadProgressEvent(Pair(transferFile, read)))
                    transferList += Pair(LocalDateTime.now(), read)
                }
            }.let { Thread(it) }.apply { start() }
            upload.progressListener = ProgressListener { pBytesRead, pContentLength, _ ->
                if (pBytesRead == pContentLength) {
                    bus.broadcast(DownloadProgressEvent(Pair(transferFile, pBytesRead)))
                    transferList += Pair(LocalDateTime.now(), pBytesRead)
                } else {
                    currentBytesRead = pBytesRead
                }
            }
            saveFileUpload(tempFile, upload, request)
            finished = true

            val actualFile = transferFolder.toPath().resolve(transferFile.fileName!!).toFile()
            actualFile.exists() && actualFile.delete()
            tempFile.renameTo(actualFile)
            transferFile.status = FileStatus.FINISHED
            transferFile.update()
            transferTimes.remove(transferFile)

            bus.broadcast(DownloadFinishEvent(CompletedFileTransfer(
                transferFile,
                transferFolder,
                actualFile
            )))
            notifyTransferFinished(transferFile, transfer, transferFolder)
        } catch (exception: IOException) {
            logger.error("Failure receiving file: ${exception.message}", exception)
            record.status = FileStatus.PENDING
            record.update()
        }
    }

    private fun notifyTransferFinished(transferFile: TransferFileRecord, transfer: TransferRecord, transferFolder: File) {
        val (count) = jooq.selectCount().from(TRANSFER_FILE)
            .where(TRANSFER_FILE.TRANSFER_ID.eq(transferFile.transferId))
            .and(TRANSFER_FILE.STATUS.ne(FileStatus.FINISHED)).fetchOne()!!
        if (count == 0) {
            transfer.status = TransferStatus.FINISHED
            transfer.update()
            val files = jooq.selectFrom(TRANSFER_FILE).where(TRANSFER_FILE.TRANSFER_ID.eq(transfer.id)).fetch()
            val locations = files.map {
                it.id!! to transferFolder.resolve(it.fileName!!)
            }.toMap()
            bus.broadcast(TransferCompleteEvent(CompletedTransfer(
                transfer,
                transferFolder,
                locations
            )))
        }
    }

    private fun saveFileUpload(tempFile: File, upload: ServletFileUpload, request: HttpServletRequest) {
        tempFile.outputStream().use { outputStream ->
            val iterator = upload.getItemIterator(request)
            var foundBody = false
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.fieldName == "file") {
                    IOUtils.copy(item.openStream(), outputStream)
                    foundBody = true
                }
            }
            !foundBody && throw IllegalArgumentException("No file upload")
        }
    }

    /**
     * Returns in B/s
     */
    fun calculateTransferRate(points: List<Pair<LocalDateTime, Long>>): Long {
        return try {
            val currentData = points.last()
            val filteredData = points.filter {
                ChronoUnit.SECONDS.between(it.first, currentData.first) < TRANSFER_CALC_INTERVAL
            }
            val secondsDiff = ChronoUnit.SECONDS.between(filteredData.first().first, currentData.first)
            val dataDiff = currentData.second - filteredData.first().second
            (dataDiff.toDouble() / secondsDiff).toLong()
        } catch (e: NoSuchElementException) {
            0L
        }
    }

    companion object {
        const val TRANSFER_CALC_INTERVAL = 5
        const val UPLOAD_PROGRESS_INTERVAL = 500 * 1000 * 1000
    }
}
