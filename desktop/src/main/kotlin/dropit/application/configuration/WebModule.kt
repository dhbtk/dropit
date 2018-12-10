package dropit.application.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class WebModule {

    @Provides
    @Singleton
    fun objectMapper() = ObjectMapper()
}