package dropit.mobile.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.webkit.MimeTypeMap
import dropit.application.dto.FileRequest
import dropit.mobile.R
import dropit.mobile.ui.RecyclerItemTouchHelper
import dropit.mobile.ui.adapter.ListFileAdapter
import dropit.mobile.ui.model.ListFile
import kotlinx.android.synthetic.main.activity_send_transfer.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList

class SendTransferActivity : AppCompatActivity(), RecyclerItemTouchHelper.SwipeListener {
    val items = ArrayList<ListFile>()
    lateinit var listFileAdapter: ListFileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transfer)

        // list
        listFileAdapter = ListFileAdapter(this, items)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = listFileAdapter
        val swipeCallback = RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this)
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)

        val savedItems = savedInstanceState?.getSerializable("items")
        if (savedItems != null && savedItems is ArrayList<*>) {
            items.clear()
            savedItems.forEach { items.add(it as ListFile) }
            listFileAdapter.notifyDataSetChanged()
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
    }

    override fun onSwiped(viewHolder: ListFileAdapter.ListViewHolder, direction: Int) {
        val index = viewHolder.adapterPosition
        val listFile = items.get(index)
        listFileAdapter.remove(index)
        val snackbar = Snackbar.make(coordinatorLayout, getString(R.string.removed_from_transfer), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.undo), {
            listFileAdapter.add(listFile, index)
        })
        snackbar.show()
    }

    // TODO: pegar também a coluna TITLE para pegar o nome do arquivo, e também verificar se existe a coluna DATA. caso
    // não exista, abrir o InputStream, copiar para um arquivo temporário
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
                listFileAdapter.notifyItemInserted(items.size - 1)
            }
        }
    }
}
