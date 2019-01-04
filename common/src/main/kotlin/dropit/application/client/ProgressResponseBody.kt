package dropit.application.client

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import okio.Source

typealias ProgressListener = (read: Long, total: Long) -> Unit

class ProgressResponseBody(
    private val responseBody: ResponseBody,
    val progressListener: ProgressListener
) : ResponseBody() {
    private val bufferedSource = Okio.buffer(createSource(responseBody.source()))

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun source(): BufferedSource = bufferedSource

    private fun createSource(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                totalRead += if (read != -1L) read else 0
                progressListener(totalRead, contentLength())
                return read
            }
        }
    }
}