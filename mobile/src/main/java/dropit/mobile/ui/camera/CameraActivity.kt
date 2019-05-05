package dropit.mobile.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Toast
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.mobile.R
import dropit.mobile.domain.service.COMPUTER
import dropit.mobile.domain.service.FILE_LIST
import dropit.mobile.domain.service.FileUploadService
import dropit.mobile.domain.service.TOKEN_REQUEST
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.ui.camera.widget.CameraPreview
import dropit.mobile.ui.configuration.ConfigurationActivity
import dropit.mobile.ui.sending.CreateTransferTask
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


@SuppressWarnings("deprecation")
class CameraActivity : AppCompatActivity(), Camera.PictureCallback {
    lateinit var sqliteHelper: SQLiteHelper
    lateinit var preferencesHelper: PreferencesHelper

    private var camera: Camera? = null
    lateinit var cameraPreview: CameraPreview
    var backCamera: Boolean = true
    var toClipboard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        sqliteHelper = SQLiteHelper(this)
        preferencesHelper = PreferencesHelper(this)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        switchCameras.setOnClickListener {
            backCamera = !backCamera
            camera?.release()
            frameLayout.removeAllViews()
            startCamera()
        }
        sendToComputer.setOnClickListener {
            toClipboard = false
            camera?.takePicture(null, null, this)
        }
        sendToClipboard.setOnClickListener {
            toClipboard = true
            camera?.takePicture(null, null, this)
        }

        pairedComputerLabel.text = resources.getString(R.string.paired_to_computer,
            sqliteHelper.getComputer(preferencesHelper.currentComputerId!!).name)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
            startCamera()

            if (preferencesHelper.currentComputerId == null) {
                Toast.makeText(this, R.string.no_current_computer, Toast.LENGTH_LONG).show()
                val configIntent = Intent(this, ConfigurationActivity::class.java)
                startActivity(configIntent)
                finish()
                return
            }
        }
    }

    override fun onPause() {
        super.onPause()
        camera?.release()
        camera = null
    }

    private fun startCamera() {
        val desiredFacing = if (backCamera) {
            Camera.CameraInfo.CAMERA_FACING_BACK
        } else {
            Camera.CameraInfo.CAMERA_FACING_FRONT
        }
        val cameraInfo = Camera.CameraInfo()
        try {
            for (cameraId in 0..Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(cameraId, cameraInfo)
                if (cameraInfo.facing == desiredFacing) {
                    camera = Camera.open(cameraId)
                    cameraPreview = CameraPreview(this, camera!!, cameraInfo)
                    frameLayout.addView(cameraPreview)
                    return
                }
            }
        } catch (e: RuntimeException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPictureTaken(data: ByteArray?, camera: Camera?) {
        flash()
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val folder = File(Environment.getExternalStorageDirectory(), "DropIt Camera Images")
        folder.mkdirs()
        val pictureFile = File(folder, "$timestamp.jpeg")
        pictureFile.outputStream().use { it.write(data) }

        sendFile(FileProvider.getUriForFile(this, "dropit.mobile.fileprovider", pictureFile), toClipboard)
        camera?.startPreview()
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
            .putExtra(TOKEN_REQUEST, TokenRequest(
                preferencesHelper.phoneId,
                preferencesHelper.phoneName
            ))
        FileUploadService.enqueueWork(this, intent)
    }

    private fun onError() {
        Toast.makeText(this, R.string.connect_to_computer_failed, Toast.LENGTH_LONG).show()
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun flash() {
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
