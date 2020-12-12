package dropit.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DaggerApplication
import dropit.application.client.Client
import dropit.application.client.ClientFactory
import dropit.mobile.domain.entity.Computer
import dropit.mobile.domain.service.ServerConnectionService
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.ui.main.MainActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

const val CHANNEL_ID = "main"
const val CONNECTION_CHANNEL_ID = "connection"

@Singleton
@Component(modules = [AndroidInjectionModule::class, ApplicationModule::class, ActivityModule::class])
interface ApplicationComponent : AndroidInjector<Application>

@Module
class ApplicationModule(private val application: Application) {
    @Singleton
    @Provides
    fun context(): Context = application

    @Singleton
    @Provides
    fun executorService(): ExecutorService = Executors.newCachedThreadPool()

    @Singleton
    @Provides
    fun objectMapper(): ObjectMapper = ObjectMapper().apply { this.findAndRegisterModules() }

    @Provides
    fun computer(sqLiteHelper: SQLiteHelper, preferencesHelper: PreferencesHelper): Computer {
        return sqLiteHelper.getComputer(preferencesHelper.currentComputerId!!)
    }

    @Provides
    fun client(
        computer: Computer,
        preferencesHelper: PreferencesHelper,
        clientFactory: ClientFactory
    ): Client {
        return clientFactory.create(
            computer.url,
            preferencesHelper.tokenRequest,
            computer.token?.toString()
        )
    }
}

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun injectMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun injectServerConnectionService(): ServerConnectionService
}

val Any.TAG: String
    get() {
        return this::class.java.simpleName
    }

class Application : DaggerApplication() {
    override fun onCreate() {
        super.onCreate()
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

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.builder().applicationModule(ApplicationModule(this))
            .build()
    }
}

fun onMainThread(task: () -> Unit) {
    Handler(Looper.getMainLooper()).post { task.invoke() }
}
