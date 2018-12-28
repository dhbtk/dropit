package dropit.mobile.ui.sending

import android.content.ContentResolver
import android.net.Uri
import android.os.AsyncTask
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.application.dto.TransferRequest
import dropit.mobile.domain.entity.Computer
import dropit.mobile.onMainThread
import java.io.File
import java.io.FileInputStream
import java.util.*

class CreateTransferTask(
    val contentResolver: ContentResolver,
    val computer: Computer,
    val tokenRequest: TokenRequest,
    val onError: () -> Unit,
    val onSuccess: (list: List<Pair<FileRequest, String>>) -> Unit
) : AsyncTask<Uri, Unit, List<Pair<FileRequest, String>>>() {
    override fun doInBackground(vararg params: Uri): List<Pair<FileRequest, String>> {
        val fileRequests = params.map { Pair(extractUriData(it), it.toString()) }

        val client = ClientFactory(ObjectMapper().apply { this.findAndRegisterModules() })
            .create(computer.url, tokenRequest, null)

        try {
            client.createTransfer(TransferRequest(
                "Transfer",
                fileRequests.map { it.first }
            )).blockingFirst()

            return fileRequests
        } catch (e: Exception) {
            onMainThread {
                onError()
            }
            return emptyList()
        }

    }

    override fun onPostExecute(result: List<Pair<FileRequest, String>>) {
        if (result.isNotEmpty()) {
            onSuccess(result)
        }
    }

    private fun extractUriData(uri: Uri): FileRequest {
        return contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME),
            null, null, null)
            .use { cursor ->
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val displayColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                val fileNameAccessible = dataColumn != -1
                val fileName = when {
                    fileNameAccessible -> File(cursor.getString(dataColumn)).name
                    displayColumn != -1 -> cursor.getString(displayColumn)
                    else -> ""
                }
                val fileSize = if (fileNameAccessible) {
                    File(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))).length()
                } else {
                    FileInputStream(contentResolver.openFileDescriptor(uri, "r").fileDescriptor).use {
                        it.channel.size()
                    }
                }
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: contentResolver.getType(uri)
                FileRequest(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType
                )
            }
    }

//        private fun plainTextToUri(text: String): Uri {
//            val dir = File(cacheDir, "clipboard")
//            !dir.exists() && dir.mkdir()
//            val tempFile = File.createTempFile("clipboard", ".txt", dir)
//            tempFile.outputStream().use { it.writer().use { it.write(text) } }
//            return FileProvider.getUriForFile(this@SendFileActivity, "dropit.mobile.fileprovider", tempFile)
//        }

}