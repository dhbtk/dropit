package dropit

import dropit.ui.view.MainView
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import kotlin.reflect.KClass

const val APP_NAME = "DropIt"

class Application : App(MainView::class) {
    init {
        FX.dicontainer = object : DIContainer {
            val context = AnnotationConfigApplicationContext("dropit")
            override fun <T : Any> getInstance(type: KClass<T>): T = context.getBean(type.java)
        }
    }
}
