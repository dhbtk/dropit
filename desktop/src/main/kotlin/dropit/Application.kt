package dropit

import org.slf4j.bridge.SLF4JBridgeHandler

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val component = DaggerApplicationComponent.create()
    component.webServer()
    component.graphicalInterface()
    component.discoveryBroadcaster()
    val display = component.display()
    while (!display.isDisposed) {
        try {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        } catch(e: NullPointerException) {
            // Cocoa Command-Q
            break
        }
    }
    component.webServer().javalin.stop()
    component.discoveryBroadcaster().stop()
}
