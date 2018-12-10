package dropit

import dagger.Module
import dropit.application.configuration.DatabaseModule
import dropit.application.configuration.WebModule
import dropit.application.settings.SettingsModule
import dropit.infrastructure.InfrastructureModule

@Module(includes = [DatabaseModule::class, WebModule::class, SettingsModule::class, InfrastructureModule::class])
class ApplicationModule