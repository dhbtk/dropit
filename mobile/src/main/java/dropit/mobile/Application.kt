package dropit.mobile

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.StrictMode

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog().penaltyDeath().build())
    }
}

fun onMainThread(task: () -> Unit) {
    Handler(Looper.getMainLooper()).post { task.invoke() }
}