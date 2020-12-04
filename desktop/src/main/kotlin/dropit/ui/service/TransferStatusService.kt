package dropit.ui.service

import dropit.application.PhoneSessionService
import dropit.domain.entity.TransferSource
import dropit.domain.service.IncomingService
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import org.jooq.DSLContext
import java.nio.file.Files
import java.util.ArrayList
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TransferStatusService @Inject constructor(
        private val bus: EventBus,
        private val incomingService: IncomingService,
        private val phoneSessionService: PhoneSessionService
) {
    data class CurrentTransfer(
        val id: UUID,
        val name: String,
        val mimeType: String,
        val size: Long,
        var progress: Int,
        var speedBytes: Long,
        val source: TransferSource
    ) {
        fun humanSize() = bytesToHuman(size)

        fun humanSpeed() = bytesToHuman(speedBytes) + "/s"

        @Suppress("MagicNumber")
        fun humanEta(): String {
            if (speedBytes == 0L) {
                return "-"
            }
            val totalSeconds = ((size * (progress.toDouble() / 100)) / speedBytes).roundToInt()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds - hours * 3600) / 60
            val seconds = totalSeconds - hours * 3600 - minutes * 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        @Suppress("MagicNumber")
        private fun bytesToHuman(bytes: Long): String {
            return when {
                bytes > (1024 * 1024 * 1024) -> bytes.let { it.toDouble() / (1024 * 1024 * 1024) }.let {
                    "${String.format(Locale.getDefault(), "%.1f", it)} GB"
                }
                bytes > (1024 * 1024) -> bytes.let { it.toDouble() / (1024 * 1024) }.let {
                    "${String.format(Locale.getDefault(), "%.1f", it)} MB"
                }
                bytes > 1024 -> "${bytes / 1024} kB"
                else -> "$bytes B"
            }
        }
    }

    data class TransferUpdatedEvent(override val payload: Unit) : AppEvent<Unit>

    val currentTransfers = ArrayList<CurrentTransfer>()

    init {
        arrayOf(
            IncomingService.DownloadStartedEvent::class,
            IncomingService.DownloadProgressEvent::class,
            IncomingService.DownloadFinishEvent::class,
            IncomingService.TransferCompleteEvent::class,
            PhoneSessionService.UploadStartedEvent::class,
            PhoneSessionService.UploadProgressEvent::class,
            PhoneSessionService.UploadFinishedEvent::class
        ).forEach { eventClass ->
            bus.subscribe(eventClass) {
                updateCurrentTransfers()
                bus.broadcast(TransferUpdatedEvent(Unit))
            }
        }
    }

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
        phoneSessionService.fileDownloadStatus.forEach { (upload, times) ->
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
        } union phoneSessionService.fileDownloadStatus.keys.map { it.id }
        currentTransfers.removeIf { it.id !in currentIds }
    }

    @Suppress("MagicNumber")
    private fun percent(first: Long?, second: Long): Int {
        return (100 * ((first?.toDouble() ?: 0.0) / second)).roundToInt()
    }
}
