package dropit.ui.service

import dropit.application.PhoneSessions
import dropit.application.model.TransferSource
import dropit.jooq.tables.records.TransferFileRecord
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.roundToInt

data class CurrentTransfer(
    val id: UUID,
    val name: String,
    val mimeType: String,
    val size: Long,
    var times: List<Pair<LocalDateTime, Long>>,
    val source: TransferSource
) {
    constructor(upload: PhoneSessions.FileUpload, times: List<Pair<LocalDateTime, Long>>) : this(
        upload.id,
        upload.file.name,
        Files.probeContentType(upload.file.toPath()) ?: "application/octet-stream",
        upload.size,
        times,
        TransferSource.COMPUTER
    )

    constructor(transfer: TransferFileRecord, times: List<Pair<LocalDateTime, Long>>) : this(
        transfer.id!!,
        transfer.fileName!!,
        transfer.mimeType!!,
        transfer.fileSize!!,
        times,
        TransferSource.PHONE
    )

    val progress
    get() = percent(times.lastOrNull()?.second, size)
    val speedBytes
    get() = calculateTransferRate(times)


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

    @Suppress("MagicNumber")
    private fun percent(first: Long?, second: Long): Int {
        return (100 * ((first?.toDouble() ?: 0.0) / second)).roundToInt()
    }

    private fun calculateTransferRate(points: List<Pair<LocalDateTime, Long>>): Long {
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
        private const val TRANSFER_CALC_INTERVAL = 5
    }
}
