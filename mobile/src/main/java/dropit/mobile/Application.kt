package dropit.mobile

import android.os.Handler
import android.os.Looper

class Application

fun onMainThread(task: () -> Unit) {
    Handler(Looper.getMainLooper()).post { task.invoke() }
}