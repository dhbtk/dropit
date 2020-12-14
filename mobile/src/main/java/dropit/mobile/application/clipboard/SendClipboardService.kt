package dropit.mobile.application.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import dagger.android.DaggerService
import dropit.application.client.Client
import dropit.mobile.R
import dropit.mobile.onMainThread
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Provider

class SendClipboardService : DaggerService() {
    @Inject
    lateinit var clientProvider: Provider<Client>
    var subscription: Disposable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val clipText = intentText(intent) ?: clipText()
        if (clipText == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        subscription?.dispose()
        subscription = clientProvider.get().sendToClipboard(clipText)
            .subscribeOn(Schedulers.newThread())
            .subscribe(::finishSuccessfully, ::onError)
        return START_NOT_STICKY
    }

    private fun intentText(intent: Intent?): String? {
        return intent?.getStringExtra(TEXT)
    }

    private fun clipText(): String? {
        return ContextCompat.getSystemService(this, ClipboardManager::class.java)
            ?.primaryClip?.let {
                if (it.itemCount == 0) {
                    null
                } else {
                    it.getItemAt(0).text.toString()
                }
            }
    }

    private fun onError(throwable: Throwable) {
        subscription?.dispose()
        subscription = null
        onMainThread {
            Toast.makeText(this, throwable.message, Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun finishSuccessfully() {
        subscription?.dispose()
        subscription = null
        onMainThread {
            Toast.makeText(this, R.string.text_sent_to_clipboard, Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TEXT = "text"

        fun sendText(context: Context, text: String) {
            Intent(context, SendClipboardService::class.java).also {
                it.putExtra(TEXT, text)
                context.startService(it)
            }
        }
    }
}