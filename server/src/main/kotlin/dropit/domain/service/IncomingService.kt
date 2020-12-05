package dropit.domain.service

import dropit.application.dto.FileStatus
import dropit.application.dto.TransferRequest
import dropit.application.dto.TransferStatus
import dropit.application.model.transfer
import dropit.application.settings.AppSettings
import dropit.domain.entity.TransferSource
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
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import kotlin.NoSuchElementException

const val UPLOAD_PROGRESS_INTERVAL = 500 * 1000 * 1000

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
     * creates a transfer from a transfer request.
     */
    fun createTransfer(phone: PhoneRecord, request: TransferRequest): String {
        return jooq.transactionResult { _ ->
            val transferId = UUID.randomUUID()
            val count = jooq.insertInto(TRANSFER)
                .set(jooq.newRecord(TRANSFER, dropit.jooq.tables.pojos.Transfer(
                    id = transferId,
                    name = request.name,
                    sendToClipboard = request.sendToClipboard,
                    phoneId = phone.id,
                    status = TransferStatus.PENDING
                ))).execute()
            count == 0 && throw IllegalStateException("Could not save transfer")
            request.files.forEach { fileRequest ->
                val record = jooq.newRecord(TRANSFER_FILE, dropit.jooq.tables.pojos.TransferFile(
                    id = UUID.fromString(fileRequest.id),
                    transferId = transferId,
                    fileName = fileRequest.fileName,
                    mimeType = fileRequest.mimeType,
                    fileSize = fileRequest.fileSize,
                    status = FileStatus.PENDING
                ))
                if (jooq.insertInto(TRANSFER_FILE).set(record).execute() == 0) {
                    throw IllegalStateException("Could not save transfer file")
                }
            }
            val transfer = jooq.fetchOne(TRANSFER, TRANSFER.ID.eq(transferId))!!
            bus.broadcast(NewTransferEvent(transfer))
            transferId.toString()
        }
    }

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
            var lastNanoTime = System.nanoTime()
            upload.progressListener = ProgressListener { pBytesRead, pContentLength, _ ->
                if (System.nanoTime() - lastNanoTime >= (UPLOAD_PROGRESS_INTERVAL) || pBytesRead == pContentLength) {
                    bus.broadcast(DownloadProgressEvent(Pair(transferFile, pBytesRead)))
                    transferList += Pair(LocalDateTime.now(), pBytesRead)
                    lastNanoTime = System.nanoTime()
                }
            }
            saveFileUpload(tempFile, upload, request)

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
     * Called from web
     *
     * receives clipboard text, logs it and notifies listeners
     */
    fun receiveClipboard(data: String) {
        if (appSettings.logClipboardTransfers) {
            jooq.newRecord(ClipboardLog.CLIPBOARD_LOG, dropit.jooq.tables.pojos.ClipboardLog(
                id = UUID.randomUUID(),
                content = data,
                source = TransferSource.PHONE
            )).insert()
        }
        bus.broadcast(ClipboardReceiveEvent(data))
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
    }
}
