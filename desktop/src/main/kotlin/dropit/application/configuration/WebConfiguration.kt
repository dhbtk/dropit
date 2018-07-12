package dropit.application.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.config.EnableWebFlux

@Configuration
@EnableWebFlux
@PropertySource("classpath:/config/application.properties")
class WebConfiguration {
    companion object {
        @Bean
        @JvmStatic
        fun propertySourcesPlaceholderConfigurer() = PropertySourcesPlaceholderConfigurer()
    }

    @Bean
    fun webHandler(applicationContext: ApplicationContext): DispatcherHandler {
        return DispatcherHandler(applicationContext)
    }

    @Bean
    fun objectMapper() = ObjectMapper()
}