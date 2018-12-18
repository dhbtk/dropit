package dropit.application.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class WebModule {

    @Provides
    @Singleton
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.registerKotlinModule()
        return objectMapper
    }
}