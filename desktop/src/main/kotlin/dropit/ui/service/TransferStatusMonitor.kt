package dropit.ui.service

import dropit.application.PhoneSessions
import dropit.application.model.FileTransfers
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap

@Singleton
class TransferStatusMonitor @Inject constructor(
    private val bus: EventBus,
    private val phoneSessions: PhoneSessions
) {

    data class TransferUpdatedEvent(override val payload: Unit) : AppEvent<Unit>

    private val transferMap = HashMap<UUID, CurrentTransfer>()
    val currentTransfers
        get() = transferMap.values

    init {
        arrayOf(
            FileTransfers.DownloadStartedEvent::class,
            FileTransfers.DownloadProgressEvent::class,
            FileTransfers.DownloadFinishEvent::class,
            FileTransfers.TransferCompleteEvent::class,
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
        FileTransfers.transferTimes.forEach { (transfer, times) ->
            transferMap.computeIfAbsent(transfer.id!!) { CurrentTransfer(transfer, times) }.also {
                it.times = times
            }
        }
        phoneSessions.fileDownloadStatus.forEach { (upload, times) ->
            transferMap.computeIfAbsent(upload.id) { CurrentTransfer(upload, times) }.also {
                it.times = times
            }
        }
        val currentIds = FileTransfers.transferTimes.keys.map { it.id!! } union
                phoneSessions.fileDownloadStatus.keys.map { it.id }
        transferMap.keys.filterNot { it in currentIds }.forEach { transferMap.remove(it) }
    }
}
