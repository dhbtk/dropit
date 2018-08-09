package dropit.domain.service

import dropit.application.dto.FileStatus
import dropit.application.dto.TokenStatus
import dropit.application.dto.TransferRequest
import dropit.application.dto.TransferStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.Phone
import dropit.domain.entity.Transfer
import dropit.domain.entity.TransferFile
import dropit.jooq.tables.Phone.PHONE
import dropit.jooq.tables.Transfer.TRANSFER
import dropit.jooq.tables.TransferFile.TRANSFER_FILE
import dropit.ui.AppTrayIcon
import org.jooq.DSLContext
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.file.Paths
import java.util.*

@Service
class TransferService(val create: DSLContext, val settings: AppSettings) {
    /**
     * Called from web
     *
     * creates a transfer from a transfer request.
     */
    fun createTransfer(token: String, request: TransferRequest): String {
        return create.transactionResult { _ ->
            val phone = create.fetchOne(PHONE, PHONE.TOKEN.eq(token))?.into(Phone::class.java)
                    ?: throw UnauthorizedException()
            val transferId = UUID.randomUUID()
            val count = create.insertInto(TRANSFER)
                    .set(create.newRecord(TRANSFER, Transfer(
                            id = transferId,
                            name = request.name,
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
            AppTrayIcon.notifyTransferStart(create.fetchOne(TRANSFER, TRANSFER.ID.eq(transferId.toString())).into(Transfer::class.java).copy(phone = phone))
            transferId.toString()
        }
    }

    /**
     * Called from web
     *
     * uploads a file, notifying the UI of progress.
     */
    fun uploadFile(token: String, fileId: String, body: Mono<FilePart>): Mono<Void> {
        val phone = create.fetchOne(PHONE, PHONE.TOKEN.eq(token)).into(Phone::class.java)
                ?: throw UnauthorizedException()
        val transferFile = create.select().from(TRANSFER_FILE)
                .join(TRANSFER).on(TRANSFER_FILE.TRANSFER_ID.eq(TRANSFER.ID))
                .join(PHONE).on(TRANSFER.PHONE_ID.eq(PHONE.ID))
                .where(PHONE.TOKEN.eq(token))
                .and(PHONE.STATUS.eq(TokenStatus.AUTHORIZED.name))
                .and(TRANSFER.STATUS.eq(TransferStatus.PENDING.name))
                .and(TRANSFER_FILE.STATUS.ne(FileStatus.FINISHED.name))
                .and(TRANSFER_FILE.ID.eq(fileId))
                .fetchOneInto(TRANSFER_FILE).into(TransferFile::class.java)
        val record = create.newRecord(TRANSFER_FILE)
        val transfer = create.fetchOne(TRANSFER, TRANSFER.ID.eq(transferFile.transferId.toString())).into(Transfer::class.java)
        val transferFolder = Paths.get(settings.settings.rootTransferFolder, settings.settings.transferFolderName.replaceFirst("%transfer%", transfer.name!!)).toFile()
        transferFolder.exists() || transferFolder.mkdirs()
        val tempFile = transferFolder.toPath().resolve(transferFile.fileName + ".part").toFile()
        !tempFile.exists() || (tempFile.delete() && tempFile.createNewFile())
        // TODO notify UI of start
        return body.flatMapMany { it.content() }.reduceWith({ tempFile.outputStream() }, { stream, buffer ->
            val bytes = ByteArray(buffer.readableByteCount())
            buffer.read(bytes)
            stream.write(bytes)
            // TODO notify UI of progress
            stream
        }).doOnSuccess { stream ->
            stream.close()
            val actualFile = transferFolder.toPath().resolve(transferFile.fileName).toFile()
            actualFile.exists() && actualFile.delete()
            tempFile.renameTo(actualFile)
            record.from(transferFile.copy(status = FileStatus.FINISHED))
            record.update()
            val (count) = create.selectCount().from(TRANSFER_FILE).where(TRANSFER_FILE.TRANSFER_ID.eq(transferFile.transferId.toString()))
                    .and(TRANSFER_FILE.STATUS.ne(FileStatus.FINISHED.name)).fetchOne()
            if (count == 0) {
                val transferRecord = create.newRecord(TRANSFER)
                transferRecord.from(transfer.copy(status = TransferStatus.FINISHED));
                transferRecord.update()
                // TODO notify UI of end
            }
        }.doOnError {
            // TODO notify UI of error
            it.printStackTrace()
            record.from(transferFile.copy(status = FileStatus.PENDING))
            record.update()
        }.then()
    }
}