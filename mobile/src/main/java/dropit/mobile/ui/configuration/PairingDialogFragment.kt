package dropit.mobile.ui.configuration

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.DaggerDialogFragment
import dropit.application.dto.TokenRequest
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import dropit.mobile.domain.service.ServerConnectionService
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import java.util.*
import java.util.concurrent.ExecutorService
import javax.inject.Inject

class PairingDialogFragment(val onSuccess: () -> Unit) : DaggerDialogFragment() {
    data class QrCodeInformation(
        val computerName: String,
        val computerId: UUID,
        val serverPort: Int,
        val ipAddresses: List<String>
    ) {
        constructor(uri: Uri) : this(
            uri.getQueryParameter("computerName")!!,
            UUID.fromString(uri.getQueryParameter("computerId")),
            uri.getQueryParameter("serverPort")!!.toInt(),
            uri.getQueryParameter("ipAddress")!!.split(",")
        )
    }

    lateinit var qrCodeInformation: QrCodeInformation

    @Inject
    lateinit var sqLiteHelper: SQLiteHelper

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var executorService: ExecutorService

    @Inject
    lateinit var pairTaskFactory: PairTask.Factory
    lateinit var pairTask: PairTask

    companion object {
        fun create(qrCode: String, onSuccess: () -> Unit): PairingDialogFragment {
            return PairingDialogFragment(onSuccess).apply {
                arguments = Bundle().apply { putString("qrCode", qrCode) }
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        qrCodeInformation = QrCodeInformation(Uri.parse(requireArguments().getString("qrCode")!!))
        pairTask = pairTaskFactory.create(
            qrCodeInformation,
            TokenRequest(preferencesHelper.phoneId, preferencesHelper.phoneName),
            ::showSuccess,
            ::showError
        )
        executorService.submit(pairTask)
        return AlertDialog.Builder(requireActivity())
            .setView(R.layout.fragment_pairing_dialog)
            .setTitle(
                String.format(
                    resources.getString(R.string.pairing_dialog_title),
                    qrCodeInformation.computerName
                )
            )
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                pairTask.cancel()
                dialog.dismiss()
            }.create()
    }

    override fun onPause() {
        super.onPause()
        pairTask.cancel()
    }

    private fun showError(throwable: Throwable) {
        onMainThread {
            throwable.printStackTrace()
            Toast.makeText(activity, throwable.localizedMessage, Toast.LENGTH_LONG).show()
            dismissAllowingStateLoss()
        }
    }

    private fun showSuccess(computer: Computer) {
        onMainThread {
            preferencesHelper.currentComputerId = computer.id
            Toast.makeText(activity, R.string.paired_and_set_as_default, Toast.LENGTH_LONG).show()
            ServerConnectionService.start(requireActivity())
            dismissAllowingStateLoss()
            onSuccess()
        }
    }
}
