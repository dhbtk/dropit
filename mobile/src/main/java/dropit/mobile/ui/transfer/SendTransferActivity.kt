package dropit.mobile.ui.transfer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.MimeTypeMap
import dropit.application.dto.FileRequest
import dropit.mobile.R
import dropit.mobile.ui.transfer.model.ListFile
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList

class SendTransferActivity : AppCompatActivity() {
    val items = ArrayList<ListFile>()
    val fileListFragment = FileListFragment.newInstance(items)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transfer)

        val savedItems = savedInstanceState?.getSerializable("items")
        if (savedItems != null && savedItems is ArrayList<*>) {
            items.clear()
            savedItems.forEach { items.add(it as ListFile) }
        }
        handleShares(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        handleShares(intent!!)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putSerializable("items", items)
        super.onSaveInstanceState(outState)
    }

    private fun handleShares(intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_SEND) {
            if (intent.type == "text/plain") {
                // nada a fazer?
            } else {
                AddFileTask().execute(intent.getParcelableExtra(Intent.EXTRA_STREAM))
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).forEach { AddFileTask().execute(it) }
        }
        showFileList()
    }

    private fun showFileList(backwards: Boolean = false) {
        if (supportFragmentManager.findFragmentByTag("tag") is FileListFragment) {
            Log.i("SendTransferActivity", "Already showing file list")
        } else {
            val trans = supportFragmentManager.beginTransaction()
            if (backwards) {
                trans.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            }
            trans.replace(R.id.fragmentContainer, fileListFragment, "tag").commit()
        }
    }

    fun showServerList(): Boolean {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, ServerListFragment(), "tag").commit()
        return true
    }

    override fun onBackPressed() {
        when (supportFragmentManager.findFragmentByTag("tag")) {
            is ServerListFragment -> showFileList(true)
            is FileListFragment -> super.onBackPressed()
            null -> super.onBackPressed()
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class AddFileTask : AsyncTask<Uri, Void, ListFile>() {
        override fun doInBackground(vararg params: Uri?): ListFile {
            val uri = params[0]!!
            val cursor = contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            return cursor.use {
                it.moveToFirst()
                val fileNameAccessible = it.getColumnIndex(MediaStore.MediaColumns.DATA) != -1
                val fileName = if (fileNameAccessible) {
                    File(cursor.getString(it.getColumnIndex(MediaStore.MediaColumns.DATA))).name
                } else if (it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME) != -1) {
                    cursor.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                } else {
                    ""
                }
                val fileSize = if (fileNameAccessible) {
                    File(cursor.getString(it.getColumnIndex(MediaStore.MediaColumns.DATA))).length()
                } else {
                    FileInputStream(contentResolver.openFileDescriptor(uri, "r").fileDescriptor).use {
                        it.channel.size()
                    }
                }
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                        ?: contentResolver.getType(uri)
                val fileRequest = FileRequest(
                        id = UUID.randomUUID().toString(),
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType
                )
                Log.i("SendTransferActivity", fileRequest.toString())
                ListFile(uri, fileRequest)
            }
        }

        override fun onPostExecute(result: ListFile?) {
            if (items.find { it.uri == result!!.uri } == null) {
                items.add(result!!)
                fileListFragment.listFileAdapter.notifyItemInserted(items.size - 1)
            }
        }
    }
}
