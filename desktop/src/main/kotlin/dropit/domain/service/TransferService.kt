package dropit.domain.service

import dropit.application.dto.FileStatus
import dropit.application.dto.TokenStatus
import dropit.application.dto.TransferRequest
import dropit.application.dto.TransferStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.Phone
import dropit.domain.entity.Transfer
import dropit.domain.entity.TransferFile
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.fs.TransferFolderProvider
import dropit.jooq.tables.Phone.PHONE
import dropit.jooq.tables.Transfer.TRANSFER
import dropit.jooq.tables.TransferFile.TRANSFER_FILE
import org.apache.commons.fileupload.ProgressListener
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.IOUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap

@Singleton
class TransferService @Inject constructor(
    val create: DSLContext,
    val settings: AppSettings,
    val bus: EventBus,
    val transferFolderProvider: TransferFolderProvider) {

    init {
        create.transaction { _ ->
            create.deleteFrom(TRANSFER_FILE).where(TRANSFER_FILE.STATUS.eq(FileStatus.PENDING.toString())).execute()
            create.deleteFrom(TRANSFER).where(TRANSFER.STATUS.eq(TransferStatus.PENDING.toString())).execute()
        }
    }

    data class CompletedFileTransfer(
        val transferFile: TransferFile,
        val transferFolder: File,
        val file: File
    )

    data class CompletedTransfer(
        val transfer: Transfer,
        val transferFolder: File,
        val locations: Map<UUID, File>
    )

    data class NewTransferEvent(override val payload: Transfer) : AppEvent<Transfer>
    data class FileTransferBeginEvent(override val payload: TransferFile) : AppEvent<TransferFile>
    data class FileTransferReceiveEvent(override val payload: Pair<TransferFile, Long>) : AppEvent<Pair<TransferFile, Long>>
    data class FileTransferFinishEvent(override val payload: CompletedFileTransfer) : AppEvent<CompletedFileTransfer>
    data class TransferCompleteEvent(override val payload: CompletedTransfer) : AppEvent<CompletedTransfer>

    val logger = LoggerFactory.getLogger(this::class.java)

    val transferTimes = HashMap<TransferFile, List<Pair<LocalDateTime, Long>>>()

    /**
     * Called from web
     *
     * creates a transfer from a transfer request.
     */
    fun createTransfer(phone: Phone, request: TransferRequest): String {
        return create.transactionResult { _ ->
            val transferId = UUID.randomUUID()
            val count = create.insertInto(TRANSFER)
                .set(create.newRecord(TRANSFER, Transfer(
                    id = transferId,
                    name = request.name,
                    sendToClipboard = request.sendToClipboard,
                    phoneId = phone.id,
                    status = TransferStatus.PENDING
                ))).execute()
            count == 0 && throw RuntimeException("Could not save transfer")
            request.files.forEach {
                val record = create.newRecord(TRANSFER_FILE, TransferFile(
                    id = UUID.fromString(it.id),
                    transferId = transferId,
                    fileName = it.fileName,
                    mimeType = it.mimeType,
                    fileSize = it.fileSize,
                    status = FileStatus.PENDING
                ))
                create.insertInto(TRANSFER_FILE).set(record).execute() == 0 && throw RuntimeException("Could not save transfer file")
            }
            val transfer = create.fetchOne(TRANSFER, TRANSFER.ID.eq(transferId.toString())).into(Transfer::class.java).copy(phone = phone)
            bus.broadcast(NewTransferEvent(transfer))
            transferId.toString()
        }
    }

    /**
     * Called from web
     *
     * uploads a file, notifying the UI of progress.
     */
    fun receiveFile(phone: Phone, fileId: String, request: HttpServletRequest) {
        val transferFile = create.select().from(TRANSFER_FILE)
            .join(TRANSFER).on(TRANSFER_FILE.TRANSFER_ID.eq(TRANSFER.ID))
            .join(PHONE).on(TRANSFER.PHONE_ID.eq(PHONE.ID))
            .where(PHONE.ID.eq(phone.id.toString()))
            .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED.name))
            .and(TRANSFER.STATUS.eq(TransferStatus.PENDING.name))
            .and(TRANSFER_FILE.STATUS.ne(FileStatus.FINISHED.name))
            .and(TRANSFER_FILE.ID.eq(fileId))
            .fetchOneInto(TRANSFER_FILE).into(TransferFile::class.java)
        val record = create.newRecord(TRANSFER_FILE)
        val transfer = create.fetchOne(TRANSFER, TRANSFER.ID.eq(transferFile.transferId.toString())).into(Transfer::class.java)
        val transferFolder = transferFolderProvider.getForTransfer(transfer).toFile()
        val tempFile = transferFolder.toPath().resolve(transferFile.fileName + ".part").toFile()
        !tempFile.exists() || (tempFile.delete() && tempFile.createNewFile())
        val displayTransferFile = transferFile.copy(transfer = transfer)
        bus.broadcast(FileTransferBeginEvent(displayTransferFile))
        val transferList = mutableListOf(Pair(LocalDateTime.now(), 0L))
        transferTimes[displayTransferFile] = transferList
        try {
            val upload = ServletFileUpload()
            var lastNanoTime = System.nanoTime()
            upload.progressListener = ProgressListener { pBytesRead, pContentLength, pItems ->
                if (System.nanoTime() - lastNanoTime >= (500 * 1000 * 1000) || pBytesRead == pContentLength) {
                    bus.broadcast(FileTransferReceiveEvent(Pair(displayTransferFile, pBytesRead)))
                    transferList += Pair(LocalDateTime.now(), pBytesRead)
                    lastNanoTime = System.nanoTime()
                }
            }
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

            val actualFile = transferFolder.toPath().resolve(transferFile.fileName).toFile()
            actualFile.exists() && actualFile.delete()
            tempFile.renameTo(actualFile)
            record.from(transferFile.copy(status = FileStatus.FINISHED))
            record.update()
            bus.broadcast(FileTransferFinishEvent(CompletedFileTransfer(
                record.into(TransferFile::class.java).copy(transfer = transfer),
                transferFolder,
                actualFile
            )))
            transferTimes.remove(displayTransferFile)

            val (count) = create.selectCount().from(TRANSFER_FILE).where(TRANSFER_FILE.TRANSFER_ID.eq(transferFile.transferId.toString()))
                .and(TRANSFER_FILE.STATUS.ne(FileStatus.FINISHED.name)).fetchOne()
            if (count == 0) {
                val transferRecord = create.newRecord(TRANSFER)
                transferRecord.from(transfer.copy(status = TransferStatus.FINISHED))
                transferRecord.update()
                val files = create.selectFrom(TRANSFER_FILE).where(TRANSFER_FILE.TRANSFER_ID.eq(transfer.id.toString()))
                    .fetchInto(TransferFile::class.java)
                val locations = files.map {
                    it.id!! to transferFolder.resolve(it.fileName!!)
                }.toMap()
                bus.broadcast(TransferCompleteEvent(CompletedTransfer(
                    transferRecord.into(Transfer::class.java).copy(files = files),
                    transferFolder,
                    locations
                )))
            }
        } catch (exception: Exception) {
            logger.error("Failure receiving file: ${exception.message}", exception)
            record.from(transferFile.copy(status = FileStatus.PENDING))
            record.update()
        }
    }

    /**
     * Returns in B/s
     */
    fun calculateTransferRate(points: List<Pair<LocalDateTime, Long>>): Long {
        val interval = 5 // 5 seconds to calc
        try {
            val currentData = points.last()
            val filteredData = points.filter { ChronoUnit.SECONDS.between(it.first, currentData.first) < interval }
            val secondsDiff = ChronoUnit.SECONDS.between(filteredData.first().first, currentData.first)
            val dataDiff = currentData.second - filteredData.first().second
            return (dataDiff.toDouble() / secondsDiff).toLong()
        } catch (e: Exception) {
            return 0L
        }
    }
}