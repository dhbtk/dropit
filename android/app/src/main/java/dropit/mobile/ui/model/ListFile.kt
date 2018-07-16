package dropit.mobile.ui.model

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import dropit.application.dto.FileRequest
import java.io.File
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
}