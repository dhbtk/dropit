package dropit.mobile.ui.sending

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.R
import dropit.mobile.databinding.ActivitySendFileBinding
import dropit.mobile.domain.service.*
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.ui.configuration.ConfigurationActivity
import java.util.*

open class SendFileActivity : AppCompatActivity() {
    lateinit var sqliteHelper: SQLiteHelper
    lateinit var preferencesHelper: PreferencesHelper
    lateinit var binding: ActivitySendFileBinding
    var activeTasks: Int = 0
    open val sendToClipboard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.NoTopBar)
        binding = ActivitySendFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqliteHelper = SQLiteHelper(this)
        preferencesHelper = PreferencesHelper(this)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            UploadFinishedReceiver(this::onUploadFinished),
            IntentFilter(UPLOAD_FINISHED)
        )

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (preferencesHelper.currentComputerId == null) {
            Toast.makeText(this, R.string.no_current_computer, Toast.LENGTH_LONG).show()
            val configIntent = Intent(this, ConfigurationActivity::class.java)
            startActivity(configIntent)
            finish()
            return
        }

        val computer = sqliteHelper.getComputer(preferencesHelper.currentComputerId!!)
        binding.connectionStatus.text = String.format(resources.getString(R.string.connecting_to_computer), computer.name)

        val action = intent.action

        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            if (intent.type == "text/plain") {
                SendClipboardTask(
                    computer,
                    preferencesHelper.tokenRequest,
                    this::onStartTransfer,
                    this::showTransferError,
                    this::showClipboardSuccess
                ).execute(intent.getStringExtra(Intent.EXTRA_TEXT))
            } else {
                val uris = if (action == Intent.ACTION_SEND) {
                    listOf(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                } else {
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!
                }

                CreateTransferTask(
                    contentResolver,
                    computer,
                    preferencesHelper.tokenRequest,
                    sendToClipboard,
                    this::onStartTransfer,
                    this::showTransferError,
                    this::startTransfer
                ).execute(*uris.toTypedArray())
            }
        } else if (intent.getBooleanExtra("sendClipboard", false)) {
            val clipText = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .primaryClip!!.let { if (it.itemCount == 0) null else it.getItemAt(0).text.toString() }
            if (clipText != null) {
                SendClipboardTask(
                    computer,
                    preferencesHelper.tokenRequest,
                    this::onStartTransfer,
                    this::showTransferError,
                    this::showClipboardSuccess
                ).execute(clipText)
            } else {
                Toast.makeText(this, R.string.clipboard_is_empty, Toast.LENGTH_LONG).show()
                finishIfPossible()
            }

        } else {
            finishIfPossible()
        }
    }

    private fun onStartTransfer() {
        activeTasks++
    }

    private fun finishIfPossible() {
        if (activeTasks == 0) {
            finish()
        } else {
            moveTaskToBack(true)
        }
    }

    private fun onUploadFinished() {
        activeTasks--
        finishIfPossible()
    }

    private fun showClipboardSuccess() {
        Toast.makeText(this, R.string.text_sent_to_clipboard, Toast.LENGTH_LONG).show()
        activeTasks--
        finishIfPossible()
    }

    private fun showTransferError() {
        Toast.makeText(this, R.string.connect_to_computer_failed, Toast.LENGTH_LONG).show()
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent)
        activeTasks--
        finishIfPossible()
    }

    private fun startTransfer(data: List<Pair<FileRequest, String>>) {
        val intent = Intent(this, FileUploadService::class.java)
            .putExtra(FILE_LIST, ArrayList(data))
            .putExtra(COMPUTER, sqliteHelper.getComputer(preferencesHelper.currentComputerId!!))
            .putExtra(TOKEN_REQUEST, TokenRequest(
                preferencesHelper.phoneId,
                preferencesHelper.phoneName
            ))
        ContextCompat.startForegroundService(this, intent)
        binding.connectionStatus.text = getString(R.string.keep_upload_activity_open_notice)
        moveTaskToBack(true)
    }

    class UploadFinishedReceiver(val onReceive: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onReceive()
        }
    }
}
