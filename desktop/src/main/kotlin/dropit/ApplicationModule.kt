package dropit

import dagger.Module
import dagger.Provides
import dropit.application.WebServer
import dropit.application.configuration.DatabaseModule
import dropit.application.configuration.WebModule
import dropit.infrastructure.NeedsStart
import dropit.infrastructure.NeedsStop
import dropit.infrastructure.discovery.DiscoveryBroadcaster
import dropit.ui.GraphicalInterface
import dropit.ui.TransferNotifications
import dropit.ui.TrayIcon
import dropit.ui.UIModule
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module(includes = [
    DatabaseModule::class,
    WebModule::class,
    UIModule::class])
class ApplicationModule {
    @Provides
    @Singleton
    @Suppress("MagicNumber")
    fun executor(): Executor = ThreadPoolExecutor(1, 5, 1, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

    @Provides
    fun needsStart(
        graphicalInterface: GraphicalInterface,
        transferNotifications: TransferNotifications,
        discoveryBroadcaster: DiscoveryBroadcaster,
        webServer: WebServer
    ): List<NeedsStart> = listOf(graphicalInterface, transferNotifications, discoveryBroadcaster, webServer)

    @Provides
    fun needsStop(
        trayIcon: TrayIcon,
        graphicalInterface: GraphicalInterface,
        discoveryBroadcaster: DiscoveryBroadcaster,
        webServer: WebServer
    ): List<NeedsStop> = listOf(trayIcon, graphicalInterface, discoveryBroadcaster, webServer)
}
