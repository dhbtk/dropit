package dropit.mobile.ui.sending

import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import java.util.*

class PairingDialogFragment : DialogFragment() {
    lateinit var sqLiteHelper: SQLiteHelper
    lateinit var preferencesHelper: PreferencesHelper
    lateinit var pairingTask: PairingTask

    companion object {
        fun create(computer: Computer): PairingDialogFragment {
            return PairingDialogFragment()
                .apply {
                    val bundle = Bundle()
                        .apply { putSerializable("computer", computer) }
                    arguments = bundle
                }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        sqLiteHelper = SQLiteHelper(context!!)
        preferencesHelper = PreferencesHelper(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val computer = arguments?.getSerializable("computer") as Computer
        val tokenRequest = TokenRequest(
            preferencesHelper.phoneId,
            preferencesHelper.phoneName
        )
        pairingTask = PairingTask(
            sqLiteHelper,
            computer,
            tokenRequest,
            this::showProgress,
            this::showError,
            this::showSuccess
        )
        pairingTask.execute()
        return activity?.let {
            AlertDialog.Builder(it)
                .setView(R.layout.fragment_pairing_dialog)
                .setTitle(String.format(resources.getString(R.string.pairing_dialog_title), computer.name))
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onPause() {
        super.onPause()
        pairingTask.cancel(true)
    }

    fun showProgress(message: String) {
        onMainThread {
            //            pairingMessage.text = message /*resources.getString(R.string.pairing_dialog_message_2)*/
        }
    }

    fun showError(messageId: Int) {
        onMainThread {
            Toast.makeText(activity, messageId, Toast.LENGTH_LONG).show()
            dismissAllowingStateLoss()
        }
    }

    fun showSuccess(computer: Computer) {
        onMainThread {
            preferencesHelper.currentComputerId = computer.id.toString()
            Toast.makeText(activity, R.string.paired_and_set_as_default, Toast.LENGTH_LONG).show()
            dismissAllowingStateLoss()
        }
    }

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
                e.printStackTrace()
            }
        }
    }
}