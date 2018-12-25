package dropit.mobile.domain.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager

class BootBroadcastReceiver : BroadcastReceiver() {
    private val events = arrayOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_USER_PRESENT,
        WifiManager.NETWORK_STATE_CHANGED_ACTION
    )

    override fun onReceive(context: Context?, intent: Intent?) {
        if (events.any { it == intent?.action }) {
            ServerDiscoveryService.enqueueWork(context!!, intent!!)
        }
    }
}