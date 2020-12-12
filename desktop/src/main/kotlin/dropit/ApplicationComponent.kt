package dropit

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Component
import dropit.application.WebServer
import dropit.application.model.ApplicationModel
import dropit.application.settings.AppSettings
import dropit.infrastructure.NeedsStart
import dropit.infrastructure.NeedsStop
import dropit.infrastructure.discovery.DiscoveryBroadcaster
import dropit.infrastructure.event.EventBus
import dropit.ui.GraphicalInterface
import org.eclipse.swt.widgets.Display
import org.jooq.DSLContext
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
    fun eventBus(): EventBus

    fun discoveryBroadcaster(): DiscoveryBroadcaster

    fun webServer(): WebServer

    fun jooq(): DSLContext

    fun appSettings(): AppSettings

    fun objectMapper(): ObjectMapper

    fun graphicalInterface(): GraphicalInterface

    fun display(): Display

    fun needsStart(): List<NeedsStart>

    fun needsStop(): List<NeedsStop>

    fun inject(model: ApplicationModel)
}
