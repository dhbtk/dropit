package dropit.application.client

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

class InputStreamBody(private val inputStream: InputStream, val size: Long, private val callback: (Long) -> Unit) : RequestBody() {
    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(8 * 1024)

        inputStream.use {
            var read = inputStream.read(buffer)
            while (read != -1) {
                sink.write(buffer, 0, read)
                callback.invoke(read.toLong())
                read = inputStream.read(buffer)
            }
        }
    }

    override fun contentLength(): Long {
        return size
    }

    override fun contentType(): MediaType? {
        return "application/octet-stream".toMediaType()
    }
}
