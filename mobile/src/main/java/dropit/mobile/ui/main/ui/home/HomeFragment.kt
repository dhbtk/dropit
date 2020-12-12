package dropit.mobile.ui.main.ui.home

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dropit.mobile.databinding.FragmentHomeBinding
import dropit.mobile.ui.configuration.PairingDialogFragment
import dropit.mobile.ui.pairing.PairingQrCodeActivity

class HomeFragment : Fragment() {
    private val homeViewModel: HomeViewModel by viewModels()
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
        binding.scanQrCode.setOnClickListener { scanQrCode() }
        return binding.root
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
