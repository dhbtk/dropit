package dropit.mobile.ui.sending

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.R
import dropit.mobile.domain.service.COMPUTER
import dropit.mobile.domain.service.FILE_LIST
import dropit.mobile.domain.service.FileUploadService
import dropit.mobile.domain.service.TOKEN_REQUEST
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.ui.configuration.ConfigurationActivity
import kotlinx.android.synthetic.main.activity_send_file.*
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

        val computer = sqliteHelper.getComputer(preferencesHelper.currentComputerId!!)
        connectionStatus.text = String.format(resources.getString(R.string.connecting_to_computer), computer.name)

        val action = intent.action

        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            if (intent.type == "text/plain") {
                SendClipboardTask(
                    computer,
                    preferencesHelper.tokenRequest,
                    this::showTransferError,
                    this::showClipboardSuccess
                ).execute(intent.getStringExtra(Intent.EXTRA_TEXT))
            } else {
                val uris = if (action == Intent.ACTION_SEND) {
                    listOf(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                } else {
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }

                CreateTransferTask(
                    contentResolver,
                    computer,
                    preferencesHelper.tokenRequest,
                    this::showTransferError,
                    this::startTransfer
                ).execute(*uris.toTypedArray())
            }
        } else {
            finish()
        }
    }

    private fun showClipboardSuccess() {
        Toast.makeText(this, R.string.text_sent_to_clipboard, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun showTransferError() {
        Toast.makeText(this, R.string.connect_to_computer_failed, Toast.LENGTH_LONG).show()
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startTransfer(data: List<Pair<FileRequest, String>>) {
        val intent = Intent(this, FileUploadService::class.java)
            .putExtra(FILE_LIST, ArrayList(data))
            .putExtra(COMPUTER, sqliteHelper.getComputer(preferencesHelper.currentComputerId!!))
            .putExtra(TOKEN_REQUEST, TokenRequest(
                preferencesHelper.phoneId,
                preferencesHelper.phoneName
            ))
        FileUploadService.enqueueWork(this, intent)
        this.finish()
    }

}