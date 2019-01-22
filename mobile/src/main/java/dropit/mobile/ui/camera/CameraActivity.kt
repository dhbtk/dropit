package dropit.mobile.ui.camera

import android.Manifest
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import dropit.mobile.R


class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
    }
}