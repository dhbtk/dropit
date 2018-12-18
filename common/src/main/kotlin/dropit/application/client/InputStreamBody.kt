package dropit.application.client

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.InputStream

class InputStreamBody(val inputStream: InputStream, val size: Long) : RequestBody() {
    override fun writeTo(sink: BufferedSink) {
        sink.writeAll(Okio.source(inputStream))
    }

    override fun contentLength(): Long {
        return size
    }

    override fun contentType(): MediaType? {
        return MediaType.get("application/octet-stream")
    }
}