package dropit.mobile.ui.camera.widget

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

@SuppressWarnings("deprecation")
class CameraPreview(
    context: Context,
    private val camera: Camera,
    private val cameraInfo: Camera.CameraInfo
) : SurfaceView(context), SurfaceHolder.Callback {

    init {
        holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (this.holder.surface == null) {
            return
        }

        try {
            camera.stopPreview()
        } catch (e: Exception) {
            // nop
        }

        val previewSize = getOptimalPreviewSize(camera.parameters.supportedPreviewSizes, measuredWidth, measuredHeight)
        val aspectRatio = previewSize.width.toDouble() / previewSize.height
        val parameters = camera.parameters
        parameters.setPreviewSize(previewSize.width, previewSize.height)
        val largestSize = camera.parameters.supportedPictureSizes.filter {
            val otherRatio = it.width.toDouble() / it.height
            Math.abs(otherRatio - aspectRatio) > 0.1
        }.sortedByDescending {
            it.width * it.height
        }.first()
        parameters.setPictureSize(largestSize.width, largestSize.height)
        parameters.setRotation(cameraRotationDegrees())
        camera.parameters = parameters
        camera.setDisplayOrientation(cameraRotationDegrees())
        camera.setPreviewDisplay(this.holder)
        camera.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera.setPreviewDisplay(holder)
        } catch(e: RuntimeException) {
            // nop
        }
    }

    /**
     * From some google example in github
     */
    private fun getOptimalPreviewSize(sizes: List<Camera.Size>, w: Int, h: Int): Camera.Size {
        val aspectTolerance = 0.1
        val targetRatio = w.toDouble() / h

        var optimalSize: Camera.Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > aspectTolerance) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize!!
    }

    fun cameraRotationDegrees(): Int {
        if (context !is Activity) {
            return 0
        }

        val activityRotation = (context as Activity).windowManager.defaultDisplay.rotation
        val rotationDegrees = when (activityRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - ((cameraInfo.orientation + rotationDegrees) % 360)) % 360 // idk about this :/
        } else {
            return (cameraInfo.orientation - rotationDegrees) % 360
        }
    }
}
