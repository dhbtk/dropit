package dropit.mobile.ui.sending

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import dagger.android.support.DaggerAppCompatActivity
import dropit.application.client.Client
import dropit.mobile.R
import dropit.mobile.application.clipboard.SendClipboardService
import dropit.mobile.application.fileupload.FileUpload
import dropit.mobile.application.fileupload.UploadNotifications
import dropit.mobile.databinding.ActivitySendFileBinding
import dropit.mobile.lib.db.SQLiteHelper
import dropit.mobile.lib.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import dropit.mobile.ui.main.MainActivity
import java9.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Provider

open class SendFileActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var sqliteHelper: SQLiteHelper

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var clientProvider: Provider<Client>

    @Inject
    lateinit var uploadNotifications: UploadNotifications
    lateinit var binding: ActivitySendFileBinding
    var activeTasks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.NoTopBar)
        binding = ActivitySendFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun redirectToPairing() {
        Toast.makeText(this, R.string.no_current_computer, Toast.LENGTH_LONG).show()
        val configIntent = Intent(this, MainActivity::class.java)
        startActivity(configIntent)
        finish()
    }

    private fun handleIntent(intent: Intent) {
        if (preferencesHelper.currentComputerId == null) {
            redirectToPairing()
            return
        }

        val computer = sqliteHelper.getComputer(preferencesHelper.currentComputerId!!)
        binding.connectionStatus.text =
            String.format(resources.getString(R.string.connecting_to_computer), computer.name)

        val action = intent.action

        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            handleSendIntent(intent, action)
        } else if (intent.getBooleanExtra(SEND_CLIPBOARD, false)) {
            handleClipboardIntent()
        }
    }

    private fun handleClipboardIntent() {
        val clipText = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .primaryClip!!.let { if (it.itemCount == 0) null else it.getItemAt(0).text.toString() }
        if (clipText != null) {
            SendClipboardService.sendText(this, clipText)
        } else {
            Toast.makeText(this, R.string.clipboard_is_empty, Toast.LENGTH_LONG).show()
        }
        finishIfPossible()
    }

    private fun handleSendIntent(intent: Intent, action: String?) {
        when {
            intent.type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)!!
                SendClipboardService.sendText(this, text)
                finishIfPossible()
            }
            action == Intent.ACTION_SEND -> {
                val uris = arrayListOf(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!)
                doUpload(uris)
            }
            else -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!
                doUpload(uris)
            }
        }
    }

    private fun doUpload(files: List<Uri>) {
        moveTaskToBack(true)
        activeTasks++
        NotificationManagerCompat.from(this).notify(
            UploadNotifications.START_NOTIFICATION_ID,
            uploadNotifications.startNotification.build()
        )
        FileUpload(this, clientProvider.get(), files, uploadNotifications.startNotification)
            .let { CompletableFuture.runAsync(it) }
            .exceptionally { t -> onMainThread { onError(t) }; null }
            .thenRun { onMainThread { onSuccess() } }
    }

    private fun onSuccess() {
        NotificationManagerCompat.from(this).cancel(
            UploadNotifications.START_NOTIFICATION_ID
        )
        uploadNotifications.showSuccessNotification()
        activeTasks--
        finishIfPossible()
    }

    private fun onError(t: Throwable) {
        NotificationManagerCompat.from(this).cancel(
            UploadNotifications.START_NOTIFICATION_ID
        )

        t.cause?.printStackTrace()

        Toast.makeText(this, "Upload failed: ${t.cause?.message}", Toast.LENGTH_LONG).show()
        activeTasks--
        finishIfPossible()
    }

    private fun finishIfPossible() {
        if (activeTasks == 0) {
            finish()
        } else {
            moveTaskToBack(false)
        }
    }

    companion object {
        private const val SEND_CLIPBOARD = "sendClipboard"

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SendFileActivity::class.java)
            intent.putExtra(SEND_CLIPBOARD, true)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
