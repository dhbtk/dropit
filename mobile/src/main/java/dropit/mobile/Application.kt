package dropit.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StrictMode

const val CHANNEL_ID = "main"

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog().build())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

    }
}

fun onMainThread(task: () -> Unit) {
    Handler(Looper.getMainLooper()).post { task.invoke() }
}
