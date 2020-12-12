package dropit.mobile.ui.camera

import android.Manifest
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.R
import dropit.mobile.databinding.ActivityCameraBinding
import dropit.mobile.domain.service.COMPUTER
import dropit.mobile.domain.service.FILE_LIST
import dropit.mobile.domain.service.FileUploadService
import dropit.mobile.domain.service.TOKEN_REQUEST
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.ui.configuration.ConfigurationActivity
import dropit.mobile.ui.sending.CreateTransferTask
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class CameraActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    lateinit var sqliteHelper: SQLiteHelper
    lateinit var preferencesHelper: PreferencesHelper

    private lateinit var binding: ActivityCameraBinding
    var backCamera: Boolean = true
    var toClipboard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sqliteHelper = SQLiteHelper(this)
        preferencesHelper = PreferencesHelper(this)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        binding.switchCameras.setOnClickListener {
            backCamera = !backCamera
            startCamera()
        }
        binding.sendToComputer.setOnClickListener {
            toClipboard = false
            takePicture()
        }
        binding.sendToClipboard.setOnClickListener {
            toClipboard = true
            takePicture()
        }

        binding.pairedComputerLabel.text = resources.getString(
            R.string.paired_to_computer,
            sqliteHelper.getComputer(preferencesHelper.currentComputerId!!).name
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
            startCamera()

//            if (preferencesHelper.currentComputerId == null) {
//                Toast.makeText(this, R.string.no_current_computer, Toast.LENGTH_LONG).show()
//                val configIntent = Intent(this, ConfigurationActivity::class.java)
//                startActivity(configIntent)
//                finish()
//                return
//            }
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
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
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
                        ), toClipboard
                    )
                }
            })
    }

    private fun sendFile(uri: Uri, toClipboard: Boolean) {
        val computer = sqliteHelper.getComputer(preferencesHelper.currentComputerId!!)

        CreateTransferTask(
            contentResolver,
            computer,
            preferencesHelper.tokenRequest,
            toClipboard,
            {},
            this::onError,
            this::startTransfer
        ).execute(uri)
        val message = if (toClipboard) {
            R.string.sent_to_clipboard
        } else {
            R.string.sent_to_computer
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startTransfer(data: List<Pair<FileRequest, String>>) {
        val intent = Intent(this, FileUploadService::class.java)
            .putExtra(FILE_LIST, ArrayList(data))
            .putExtra(COMPUTER, sqliteHelper.getComputer(preferencesHelper.currentComputerId!!))
            .putExtra(
                TOKEN_REQUEST, TokenRequest(
                    preferencesHelper.phoneId,
                    preferencesHelper.phoneName
                )
            )
        ContextCompat.startForegroundService(this, intent)
    }

    private fun onError() {
        Toast.makeText(this, R.string.connect_to_computer_failed, Toast.LENGTH_LONG).show()
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent)
        finish()
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

    companion object {
        private const val TAG = "CameraActivity"
    }
}
