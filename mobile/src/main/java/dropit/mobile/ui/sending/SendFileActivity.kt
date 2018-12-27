package dropit.mobile.ui.sending

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.application.dto.TransferRequest
import dropit.mobile.R
import dropit.mobile.domain.service.COMPUTER
import dropit.mobile.domain.service.FILE_LIST
import dropit.mobile.domain.service.FileUploadService
import dropit.mobile.domain.service.TOKEN_REQUEST
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import dropit.mobile.ui.configuration.ConfigurationActivity
import kotlinx.android.synthetic.main.activity_send_file.*
import java.io.File
import java.io.FileInputStream
import java.util.*

class SendFileActivity : AppCompatActivity() {
    lateinit var sqliteHelper: SQLiteHelper
    lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.NoTopBar)
        setContentView(R.layout.activity_send_file)
        sqliteHelper = SQLiteHelper(this)
        preferencesHelper = PreferencesHelper(this)

        if (preferencesHelper.currentComputerId == null) {
            Toast.makeText(this, R.string.no_current_computer, Toast.LENGTH_LONG).show()
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val name = sqliteHelper.getComputer(UUID.fromString(preferencesHelper.currentComputerId)).name
        connectionStatus.text = String.format(resources.getString(R.string.connecting_to_computer), name)
        CreateTransferTask().execute()
    }

    inner class CreateTransferTask : AsyncTask<Unit, Unit, List<Pair<FileRequest, String>>>() {
        override fun doInBackground(vararg params: Unit?): List<Pair<FileRequest, String>> {
            val action = intent.action
            val fileRequests = if (action == Intent.ACTION_SEND) {
                if (intent.type == "text/plain") {
                    // TODO implement clipboard sharing
                    val uri = plainTextToUri(intent.getStringExtra(Intent.EXTRA_TEXT))
                    listOf(Pair(extractUriData(uri).copy(fileName = "clipboard.txt"), uri.toString()))
                } else {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    listOf(Pair(extractUriData(uri), uri.toString()))
                }
            } else if (action == Intent.ACTION_SEND_MULTIPLE) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).map {
                    Pair(extractUriData(it), it.toString())
                }
            } else {
                emptyList()
            }

            if (fileRequests.isEmpty()) {
                return emptyList()
            }

            val tokenRequest = TokenRequest(
                preferencesHelper.phoneId,
                preferencesHelper.phoneName
            )
            val computer = sqliteHelper.getComputer(UUID.fromString(preferencesHelper.currentComputerId))
            val client = ClientFactory(ObjectMapper().apply { this.findAndRegisterModules() })
                .create("https://${computer.ipAddress}:${computer.port}", tokenRequest, null)

            try {
                val transferId = client.createTransfer(TransferRequest(
                    "Transfer",
                    fileRequests.map { it.first }
                )).blockingFirst()

                return fileRequests
            } catch (e: Exception) {
                onMainThread {
                    Toast.makeText(this@SendFileActivity, R.string.connect_to_computer_failed, Toast.LENGTH_LONG).show()
                    val intent = Intent(this@SendFileActivity, ConfigurationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                return emptyList()
            }

        }

        override fun onPostExecute(result: List<Pair<FileRequest, String>>?) {
            if (result!!.isNotEmpty()) {
                val intent = Intent(this@SendFileActivity, FileUploadService::class.java)
                    .putExtra(FILE_LIST, ArrayList(result))
                    .putExtra(COMPUTER, sqliteHelper.getComputer(UUID.fromString(preferencesHelper.currentComputerId)))
                    .putExtra(TOKEN_REQUEST, TokenRequest(
                        preferencesHelper.phoneId,
                        preferencesHelper.phoneName
                    ))
                FileUploadService.enqueueWork(this@SendFileActivity, intent)
                this@SendFileActivity.finish()
            }
        }

        private fun extractUriData(uri: Uri): FileRequest {
            val cursor = contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null)
            return cursor.use {
                it.moveToFirst()
                val fileNameAccessible = it.getColumnIndex(MediaStore.MediaColumns.DATA) != -1
                val fileName = when {
                    fileNameAccessible -> File(cursor.getString(it.getColumnIndex(MediaStore.MediaColumns.DATA))).name
                    it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME) != -1 -> cursor.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    else -> ""
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
                FileRequest(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType
                )
            }
        }

        private fun plainTextToUri(text: String): Uri {
            val dir = File(cacheDir, "clipboard")
            !dir.exists() && dir.mkdir()
            val tempFile = File.createTempFile("clipboard", ".txt", dir)
            tempFile.outputStream().use { it.writer().use { it.write(text) } }
            return FileProvider.getUriForFile(this@SendFileActivity, "dropit.mobile.fileprovider", tempFile)
        }

    }
}