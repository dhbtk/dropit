package dropit

import dropit.ui.view.MainView
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import tornadofx.*
import kotlin.reflect.KClass

const val APP_NAME = "DropIt"

class Application : App(MainView::class) {
    init {
        FX.dicontainer = object : DIContainer {
            val context = AnnotationConfigApplicationContext("dropit")
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
        }
        FX.stylesheets += Application::class.java.getResource("/ui/application.css").toExternalForm()
    }
}
