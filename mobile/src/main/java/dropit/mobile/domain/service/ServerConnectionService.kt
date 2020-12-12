package dropit.mobile.domain.service

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.stopForeground
import dagger.android.DaggerService
import dropit.mobile.CONNECTION_CHANNEL_ID
import dropit.mobile.R
import dropit.mobile.TAG
import dropit.mobile.domain.entity.Computer
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Provider

class ServerConnectionService : DaggerService() {
    @Inject
    lateinit var preferencesHelper: PreferencesHelper
    @Inject
    lateinit var executorService: ExecutorService
    @Inject
    lateinit var serverConnectionProvider: Provider<ServerConnection>

    private var alreadyRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(this.TAG, "onStartCommand: currentComputerId=${preferencesHelper.currentComputerId} alreadyRunning=$alreadyRunning")
        if (preferencesHelper.currentComputerId == null || alreadyRunning) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        alreadyRunning = true
        doConnect()
        return START_STICKY
    }

    private fun doConnect() {
        serverConnectionProvider.get().doConnect(::onStart, ::onStop, ::onRestart)
    }

    private fun onStop() {
        alreadyRunning = false
        stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onRestart() {
        executorService.submit {
            Thread.sleep(5000)
            onMainThread {
                stopForeground(false)
                doConnect()
            }
        }
    }

    private fun onStart(computer: Computer) {
        val notificationBuilder = NotificationCompat.Builder(this, CONNECTION_CHANNEL_ID)
            .setContentTitle(String.format(getText(R.string.connected_to_computer).toString(), computer.name))
            .setContentText(getText(R.string.ready_to_receive_data))
            .setTicker(getText(R.string.ready_to_receive_data))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    companion object {
        const val JOB_ID = 1
        const val NOTIFICATION_ID = 7
        const val DOWNLOAD_NOTIFICATION_ID = 2

        fun start(context: Context) {
            context.startService(Intent(context, ServerConnectionService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
