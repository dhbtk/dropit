package dropit

import org.slf4j.bridge.SLF4JBridgeHandler

const val APP_NAME = "DropIt"

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val component = DaggerApplicationComponent.create()
    component.webServer()
    component.graphicalInterface()
    component.discoveryBroadcaster()
    val display = component.display()
    while (!display.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
    component.webServer().javalin.stop()
    component.discoveryBroadcaster().stop()
}