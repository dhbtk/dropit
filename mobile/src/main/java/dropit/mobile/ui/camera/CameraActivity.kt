package dropit.mobile.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.android.support.DaggerAppCompatActivity
import dropit.mobile.R
import dropit.mobile.TAG
import dropit.mobile.application.entity.Computer
import dropit.mobile.application.fileupload.FileUploadService
import dropit.mobile.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.roundToInt


class CameraActivity : DaggerAppCompatActivity() {
    @Inject
    lateinit var computerProvider: Provider<Computer>

    private var imageCapture: ImageCapture? = null

    private lateinit var binding: ActivityCameraBinding
    var backCamera: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        binding.switchCameras.setOnClickListener {
            backCamera = !backCamera
            startCamera()
        }
        binding.sendToComputer.setOnClickListener {
            takePicture()
        }

        binding.pairedComputerLabel.text = resources.getString(
            R.string.paired_to_computer,
            computerProvider.get().name
        )
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector =
                if (backCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

            } catch (exc: Exception) {
                Log.e(this::class.java.simpleName, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return

        flash()
        val timestamp =
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val folder = File(Environment.getExternalStorageDirectory(), "DropIt Camera Images")
        folder.mkdirs()
        val pictureFile = File(folder, "$timestamp.jpeg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(pictureFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(this.TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(pictureFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    sendFile(
                        FileProvider.getUriForFile(
                            this@CameraActivity,
                            "dropit.mobile.fileprovider",
                            pictureFile
                        )
                    )
                }
            })
    }

    private fun sendFile(uri: Uri) {
        FileUploadService.start(this, arrayListOf(uri))
        Toast.makeText(this, R.string.sent_to_computer, Toast.LENGTH_LONG).show()
    }

    private fun flash() {
        val flashOverlay = binding.flashOverlay
        flashOverlay.imageAlpha = 255
        flashOverlay.visibility = View.VISIBLE

        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1f) {
                    flashOverlay.imageAlpha = 0
                    flashOverlay.visibility = View.GONE
                } else {
                    flashOverlay.imageAlpha = ((1 - interpolatedTime) * 255).roundToInt()
                    flashOverlay.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean = true
        }

        animation.interpolator = AccelerateInterpolator()
        animation.duration = 300
        flashOverlay.startAnimation(animation)
    }
}
