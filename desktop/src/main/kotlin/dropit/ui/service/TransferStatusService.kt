package dropit.ui.service

import dropit.application.OutgoingService
import dropit.domain.entity.TransferSource
import dropit.domain.service.IncomingService
import dropit.infrastructure.event.EventBus
import org.jooq.DSLContext
import java.nio.file.Files
import java.util.ArrayList
import java.util.UUID
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TransferStatusService(
    private val bus: EventBus,
    private val incomingService: IncomingService,
    private val outgoingService: OutgoingService,
    private val jooq: DSLContext
) {
    data class CurrentTransfer(
        val id: UUID,
        val name: String,
        val mimeType: String,
        val size: Long,
        var progress: Int,
        var speedBytes: Long,
        val source: TransferSource
    )

    val currentTransfers = ArrayList<CurrentTransfer>()

    private fun updateCurrentTransfers() {
        incomingService.transferTimes.forEach { transfer, times ->
            val displayTransfer = currentTransfers.find { it.id == transfer.id } ?: CurrentTransfer(
                transfer.id!!,
                transfer.fileName!!,
                transfer.mimeType!!,
                transfer.fileSize!!,
                0,
                0L,
                TransferSource.PHONE
            ).apply { currentTransfers.add(this) }
            displayTransfer.progress = percent(times.lastOrNull()?.second, displayTransfer.size)
            displayTransfer.speedBytes = incomingService.calculateTransferRate(times)
        }
        outgoingService.fileDownloadStatus.forEach { upload, times ->
            val displayTransfer = currentTransfers.find { it.id == upload.id } ?: CurrentTransfer(
                upload.id,
                upload.file.name,
                Files.probeContentType(upload.file.toPath()),
                upload.size,
                0,
                0L,
                TransferSource.COMPUTER
            ).apply { currentTransfers.add(this) }
            displayTransfer.progress = percent(times.lastOrNull()?.second, displayTransfer.size)
            displayTransfer.speedBytes = incomingService.calculateTransferRate(times)
        }
        val currentIds = incomingService.transferTimes.keys.map {
            it.id!!
        } union outgoingService.fileDownloadStatus.keys.map { it.id }
        currentTransfers.removeIf { it.id !in currentIds }
    }

    @Suppress("MagicNumber")
    private fun percent(first: Long?, second: Long): Int {
        return (100 * ((first?.toDouble() ?: 0.0) / second)).roundToInt()
    }
}

