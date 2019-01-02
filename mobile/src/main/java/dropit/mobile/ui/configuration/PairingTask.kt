package dropit.mobile.ui.configuration

import android.os.AsyncTask
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import dropit.mobile.infrastructure.db.SQLiteHelper
import java.util.*

class PairingTask(
    val sqLiteHelper: SQLiteHelper,
    val computer: Computer,
    val tokenRequest: TokenRequest,
    val onProgress: (s: String) -> Unit,
    val onError: (m: Int) -> Unit,
    val onSuccess: (c: Computer) -> Unit
) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg params: Unit?) {
        try {
            val client = ClientFactory(ObjectMapper().apply { this.findAndRegisterModules() })
                .create("https://${computer.ipAddress}:${computer.port}", tokenRequest, null)
            onProgress("Requesting token...")
            client.requestToken().blockingFirst()
            onProgress("Waiting for confirmation...")
            var tokenResponse = client.getTokenStatus().blockingFirst()
            while (tokenResponse.status == TokenStatus.PENDING) {
                Thread.sleep(500)
                tokenResponse = client.getTokenStatus().blockingFirst()
            }
            // TODO: handle secret changing?
            if (tokenResponse.status == TokenStatus.DENIED) {
                onError(R.string.pairing_request_denied)
                return
            }
            if (computer.secret != null && computer.secret != tokenResponse.computerSecret) {
                onError(R.string.computer_secret_changed)
                return
            }
            val updatedComputer = sqLiteHelper.updateComputer(
                computer.copy(
                    secret = tokenResponse.computerSecret,
                    tokenStatus = tokenResponse.status,
                    token = UUID.fromString(client.token)
                )
            )
            onSuccess(updatedComputer)
        } catch (e: Exception) {
            onError(R.string.pairing_failed)
            e.printStackTrace()
        }
    }
}