package dropit.mobile.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dropit.mobile.CHANNEL_ID
import dropit.mobile.R
import dropit.mobile.application.connection.ServerConnectionService

class BootBroadcastReceiver : BroadcastReceiver() {
    private val events = arrayOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_USER_PRESENT,
        WifiManager.NETWORK_STATE_CHANGED_ACTION
    )

    override fun onReceive(context: Context?, intent: Intent?) {
        StringBuilder().apply {
            append("Action: ${intent!!.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d("BootBroadcastReceiver", log)
                Toast.makeText(context, log, Toast.LENGTH_LONG).show()
            }
        }

        NotificationCompat.Builder(context!!, CHANNEL_ID)
                .setContentTitle("Received broadcast event")
                .setContentText(intent?.action)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
                .setSmallIcon(R.drawable.ic_notification).let { notification ->
                    NotificationManagerCompat.from(context).notify(1011, notification.build())
                }
        ServerConnectionService.start(context)
    }
}
