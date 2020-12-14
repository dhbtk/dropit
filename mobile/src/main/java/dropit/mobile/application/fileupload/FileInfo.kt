package dropit.mobile.application.fileupload

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import dropit.application.dto.FileRequest
import java.io.FileInputStream
import java.util.*

data class FileInfo(
    val id: UUID,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uri: Uri
) {
    val fileRequest = FileRequest(id.toString(), fileName, mimeType, fileSize)

    companion object {
        fun fromUri(contentResolver: ContentResolver, uri: Uri): FileInfo {
            return contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null
            ).use { cursor ->
                val displayColumn = cursor!!.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                val fileName = when {
                    displayColumn != -1 -> cursor.getString(displayColumn)
                    else -> UUID.randomUUID().toString()
                }
                val fileSize = contentResolver
                    .openFileDescriptor(uri, "r").use { parcelDescriptor ->
                        FileInputStream(parcelDescriptor!!.fileDescriptor).use {
                            it.channel.size()
                        }
                    }
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: contentResolver.getType(uri) ?: "application/octet-stream"
                FileInfo(
                    id = UUID.randomUUID(),
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    uri = uri
                )
            }
        }
    }
}