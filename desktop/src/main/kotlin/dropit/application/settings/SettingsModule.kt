package dropit.application.settings

import dagger.Module
import dagger.Provides
import org.jooq.DSLContext
import javax.inject.Singleton

@Module
class SettingsModule {
    @Provides
    @Singleton
    fun appSettings(jooq: DSLContext) = AppSettings(jooq)
}