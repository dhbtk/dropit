package dropit.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StrictMode

const val CHANNEL_ID = "main"
const val CONNECTION_CHANNEL_ID = "connection"

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog().build())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_description)
                notificationManager.createNotificationChannel(this)
            }
            NotificationChannel(
                    CONNECTION_CHANNEL_ID,
                    getString(R.string.connection_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.connection_channel_description)
                notificationManager.createNotificationChannel(this)
            }
        }
    }
}

fun onMainThread(task: () -> Unit) {
    Handler(Looper.getMainLooper()).post { task.invoke() }
}
