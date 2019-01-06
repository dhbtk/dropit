package dropit.infrastructure

import java.io.File
import java.io.FileInputStream

class DownloadInputStream(file: File, val callback: (read: Int) -> Unit) : FileInputStream(file) {
    val totalSize = file.length()
    var totalRead = 0L
    override fun read(b: ByteArray?): Int {
        return wrapRead(super.read(b))
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return wrapRead(super.read(b, off, len))
    }

    private fun wrapRead(read: Int): Int {
        callback(read)
        return read
    }
}