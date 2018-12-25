package dropit.mobile.domain.service

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService

class ServerDiscoveryService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val JOB_ID = 1

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ServerDiscoveryService::class.java, JOB_ID, work)
        }
    }
}