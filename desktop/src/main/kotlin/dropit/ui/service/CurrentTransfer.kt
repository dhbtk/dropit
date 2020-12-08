package dropit.ui.service

import dropit.application.model.TransferSource
import java.util.*
import kotlin.math.roundToInt

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
