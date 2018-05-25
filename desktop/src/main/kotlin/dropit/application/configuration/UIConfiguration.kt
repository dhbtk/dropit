package dropit.application.configuration

import dropit.APP_NAME
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import javax.imageio.ImageIO

@Configuration
@Profile("!test")
class UIConfiguration {
    @Bean
    fun trayIcon(): TrayIcon {
        Toolkit.getDefaultToolkit()
        val icon = TrayIcon(
                ImageIO.read(UIConfiguration::class.java.getResourceAsStream("/ui/icon.png")),
                APP_NAME
        )
        icon.isImageAutoSize = true
        SystemTray.getSystemTray().add(icon)
        return icon
    }
}