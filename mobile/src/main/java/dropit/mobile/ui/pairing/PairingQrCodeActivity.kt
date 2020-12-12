package dropit.mobile.ui.pairing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import dropit.mobile.databinding.ActivityPairingQrCodeBinding
import dropit.mobile.onMainThread
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PairingQrCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPairingQrCodeBinding
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingQrCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    private fun handleResult(qrCode: Result) {
        onMainThread {
            Intent().also { intent ->
                intent.putExtra("qrCode", qrCode.text)
                setResult(RESULT_OK, intent)
            }
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(binding.viewFinder.width, binding.viewFinder.height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { it.setAnalyzer(cameraExecutor, QrCodeAnalyzer(::handleResult)) }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)

            } catch (exc: Exception) {
                Log.e(this::class.java.simpleName, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}