package dropit

import dagger.Module
import dagger.Provides
import dropit.application.configuration.DatabaseModule
import dropit.application.configuration.WebModule
import dropit.application.settings.SettingsModule
import dropit.infrastructure.InfrastructureModule
import dropit.ui.UIModule
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module(includes = [
    DatabaseModule::class,
    WebModule::class,
    SettingsModule::class,
    InfrastructureModule::class,
    UIModule::class])
class ApplicationModule {
    @Provides
    @Singleton
    @Suppress("MagicNumber")
    fun executor(): Executor = ThreadPoolExecutor(1, 5, 1, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
}