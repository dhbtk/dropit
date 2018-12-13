package dropit

import dagger.Component
import dropit.application.WebServer
import dropit.domain.service.PhoneService
import dropit.domain.service.TransferService
import dropit.infrastructure.discovery.DiscoveryBroadcaster
import dropit.infrastructure.event.EventBus
import org.jooq.DSLContext
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
    fun phoneService(): PhoneService

    fun transferService(): TransferService

    fun eventBus(): EventBus

    fun discoveryBroadcaster(): DiscoveryBroadcaster

    fun webServer(): WebServer

    fun jooq(): DSLContext
}