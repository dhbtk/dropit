package dropit.ui.service

import dropit.application.PhoneSessions
import dropit.application.model.TransferSource
import dropit.domain.service.IncomingService
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import java.nio.file.Files
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TransferStatusService @Inject constructor(
        private val bus: EventBus,
        private val incomingService: IncomingService,
        private val phoneSessions: PhoneSessions
) {

    data class TransferUpdatedEvent(override val payload: Unit) : AppEvent<Unit>

    val currentTransfers = ArrayList<CurrentTransfer>()

    init {
        arrayOf(
            IncomingService.DownloadStartedEvent::class,
            IncomingService.DownloadProgressEvent::class,
            IncomingService.DownloadFinishEvent::class,
            IncomingService.TransferCompleteEvent::class,
            PhoneSessions.UploadStartedEvent::class,
            PhoneSessions.UploadProgressEvent::class,
            PhoneSessions.UploadFinishedEvent::class
        ).forEach { eventClass ->
            bus.subscribe(eventClass) {
                updateCurrentTransfers()
                bus.broadcast(TransferUpdatedEvent(Unit))
            }
        }
    }

    private fun updateCurrentTransfers() {
        incomingService.transferTimes.forEach { (transfer, times) ->
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
        phoneSessions.fileDownloadStatus.forEach { (upload, times) ->
            val displayTransfer = currentTransfers.find { it.id == upload.id } ?: CurrentTransfer(
                upload.id,
                upload.file.name,
                Files.probeContentType(upload.file.toPath()) ?: "application/octet-stream",
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
        } union phoneSessions.fileDownloadStatus.keys.map { it.id }
        currentTransfers.removeIf { it.id !in currentIds }
    }

    @Suppress("MagicNumber")
    private fun percent(first: Long?, second: Long): Int {
        return (100 * ((first?.toDouble() ?: 0.0) / second)).roundToInt()
    }
}
