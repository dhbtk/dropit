package dropit.mobile.ui.transfer.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import dropit.application.dto.FileRequest
import java.io.Serializable

class ListFile(uri: Uri, val fileRequest: FileRequest) : Serializable {
    val uri = uri.toString()

    fun getIcon(context: Context): Drawable? {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(uri)
        intent.type = fileRequest.mimeType
        val resolves = context.packageManager.queryIntentActivities(intent, 0)
        return if (resolves.isEmpty()) {
            null
        } else {
            resolves.first()?.loadIcon(context.packageManager)
        }
    }

    fun getThumbnail(context: Context): Bitmap? {
        if (fileRequest.mimeType!!.startsWith("image/")) {
            context.contentResolver.openInputStream(Uri.parse(uri)).use {
                val largeBitmap = BitmapFactory.decodeStream(it)
                return ThumbnailUtils.extractThumbnail(largeBitmap, 96, 96)
            }
        } else if (fileRequest.mimeType!!.startsWith("video/")) {
            return context.contentResolver.openFileDescriptor(Uri.parse(uri), "r").use { fd ->
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(fd.fileDescriptor)
                val largeBitmap = retriever.getFrameAtTime(-1)
                ThumbnailUtils.extractThumbnail(largeBitmap, 96, 96)
            }
        } else {
            return null
        }
    }
}