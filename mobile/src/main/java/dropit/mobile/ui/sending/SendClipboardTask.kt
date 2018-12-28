package dropit.mobile.ui.sending

import android.os.AsyncTask
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.mobile.domain.entity.Computer
import dropit.mobile.onMainThread

class SendClipboardTask(
    val computer: Computer,
    val tokenRequest: TokenRequest,
    val onError: () -> Unit,
    val onSuccess: () -> Unit
) : AsyncTask<String, Unit, Boolean>() {
    override fun doInBackground(vararg params: String?): Boolean {
        val client = ClientFactory(ObjectMapper().apply { this.findAndRegisterModules() })
            .create(computer.url, tokenRequest, null)

        return try {
            client.sendToClipboard(params[0]!!).blockingFirst()
            true
        } catch (e: Exception) {
            onMainThread {
                onError()
            }
            false
        }
    }

    override fun onPostExecute(result: Boolean) {
        if (result) {
            onSuccess()
        }
    }
}