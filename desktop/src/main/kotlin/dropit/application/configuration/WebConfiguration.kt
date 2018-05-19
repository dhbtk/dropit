package dropit.application.configuration

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.config.EnableWebFlux

@Configuration
@EnableWebFlux
class WebConfiguration {
    @Bean
    fun webHandler(applicationContext: ApplicationContext): DispatcherHandler {
        return DispatcherHandler(applicationContext)
    }
}