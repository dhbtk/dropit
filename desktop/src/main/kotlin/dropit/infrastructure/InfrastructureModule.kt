package dropit.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import dropit.application.settings.AppSettings
import dropit.infrastructure.discovery.DiscoveryBroadcaster
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.fs.ConfigFolderProvider
import dropit.infrastructure.fs.TransferFolderProvider
import javax.inject.Singleton

@Module
class InfrastructureModule {

    @Provides
    @Singleton
    fun discoveryBroadcaster(appSettings: AppSettings, objectMapper: ObjectMapper) =
        DiscoveryBroadcaster(appSettings, objectMapper)

    @Provides
    @Singleton
    fun eventBus() = EventBus()

    @Provides
    @Singleton
    fun configFolderProvider() = ConfigFolderProvider()

    @Provides
    @Singleton
    fun transferFolderProvider(appSettings: AppSettings) = TransferFolderProvider(appSettings)
}