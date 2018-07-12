package dropit

import dropit.application.settings.AppSettings
import dropit.ui.AppTrayIcon.trayIcon
import dropit.ui.view.MainView
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import tornadofx.*
import java.awt.SystemTray
import kotlin.reflect.KClass

const val APP_NAME = "DropIt"

class Application : App(Image(Application::class.java.getResourceAsStream("/ui/icon.png")), MainView::class) {
    companion object {
        val context = AnnotationConfigApplicationContext("dropit")
        var primaryStage: Stage? = null

        fun exit() {
            SystemTray.getSystemTray().remove(trayIcon)
            Platform.exit()
            context.close()
        }
    }

    init {
        Platform.setImplicitExit(false)
        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
            override fun <T : Any> getInstance(type: KClass<T>, name: String) = context.getBean(name, type.java)
        }
        FX.stylesheets += Application::class.java.getResource("/ui/application.css").toExternalForm()
    }

    override fun start(stage: Stage) {
        primaryStage = stage
        trayIcon?.toolTip = APP_NAME
        stage.setOnCloseRequest {
            it.consume()
            stage.hide()
        }
        super.start(stage)
    }

    override fun shouldShowPrimaryStage(): Boolean {
        return FX.dicontainer!!.getInstance(AppSettings::class).firstStart
    }
}

fun main(args: Array<String>) {
    javafx.application.Application.launch(Application::class.java, *args)
}