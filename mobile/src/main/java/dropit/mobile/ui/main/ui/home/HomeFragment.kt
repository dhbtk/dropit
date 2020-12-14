package dropit.mobile.ui.main.ui.home

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.android.support.DaggerFragment
import dropit.mobile.databinding.FragmentHomeBinding
import dropit.mobile.ui.configuration.PairingDialogFragment
import dropit.mobile.ui.pairing.PairingQrCodeActivity
import javax.inject.Inject

class HomeFragment : DaggerFragment() {
    @Inject
    lateinit var homeViewModelFactory: HomeViewModel.Factory

    private val homeViewModel: HomeViewModel by viewModels { homeViewModelFactory }
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel.text.observe(viewLifecycleOwner, {
            binding.textHome.text = it
        })
        binding.vm = homeViewModel
        binding.scanQrCode.setOnClickListener { scanQrCode() }
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val qrCode = requireActivity().intent?.dataString
        if (qrCode != null) {
            openPairingDialog(qrCode)
        }
    }

    private fun scanQrCode() {
        Intent(activity, PairingQrCodeActivity::class.java).also {
            startActivityForResult(it, QR_CODE_REQUEST)
        }
    }

    private fun openPairingDialog(qrCode: String) {
        PairingDialogFragment.create(qrCode, ::refreshInfo)
            .show(requireActivity().supportFragmentManager, "pair")
    }

    private fun refreshInfo() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!(requestCode == QR_CODE_REQUEST && resultCode == RESULT_OK && data != null)) return
        val qrCode = data.extras?.getString("qrCode")!!
        openPairingDialog(qrCode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val QR_CODE_REQUEST = 1
    }
}
