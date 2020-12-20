package dropit

import dropit.ui.DesktopIntegrations
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.CharArrayWriter
import java.io.PrintWriter
import kotlin.system.exitProcess

object Application {
    val component: ApplicationComponent = DaggerApplicationComponent.create()
}

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    rootLogger.info(
        "$APP_NAME $VERSION starting: dropit.exefile=${System.getProperty("dropit.exefile")} args=${
            args.joinToString(
                ", ",
                "\"",
                "\""
            )
        }"
    )

    val component = Application.component
    val shutdownHandler = Runnable(::shutdownHandler)

    Runtime.getRuntime().addShutdownHook(Thread(shutdownHandler))

    for (needsStart in component.needsStart()) {
        try {
            needsStart.start()
        } catch(e: Throwable) {
            reportError(e)
        }
    }

    val display = component.display()
    display.setRuntimeExceptionHandler { e -> reportError(e) }
    while (!display.isDisposed) {
        try {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        } catch (e: NullPointerException) {
            // Cocoa Command-Q
            break
        }
    }
    shutdownHandler.run()
}

private fun tryThis(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        rootLogger.error("Error running shutdownHandler: ${e.message}", e)
    }
}

private fun shutdownHandler() {
    Application.component.needsStop().forEach { tryThis { it.stop() } }
}

fun reportError(exception: Throwable) {
    rootLogger.error(exception.message, exception)

    val stackTraceText = CharArrayWriter()
        .apply { exception.printStackTrace(PrintWriter(this)) }
        .toString()

    val display = Display.getCurrent()

    val shell = Shell(display, SWT.DIALOG_TRIM).apply {
        text = APP_NAME
        image = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
        size = Point(640, 320)
        layout = GridLayout(1, false)
    }

    val githubUrl = "https://github.com/dhbtk/DropIt/issues"

    Link(shell, SWT.LEFT or SWT.WRAP).apply {
        text = "DropIt could not be started. The following stack trace could be useful in figuring out why. " +
                "Please report this at <a href=\"$githubUrl\">our GitHub page</a>."
        layoutData = GridData(GridData.FILL_HORIZONTAL)
        pack()
        addListener(SWT.Selection) {
            DesktopIntegrations().openUrl(githubUrl)
        }
    }

    Text(shell, SWT.MULTI or SWT.READ_ONLY or SWT.WRAP or SWT.LEFT or SWT.BORDER or SWT.V_SCROLL).apply {
        text = stackTraceText
        layoutData = GridData(GridData.FILL_HORIZONTAL)
        pack()
    }

    shell.addListener(SWT.Close) {
        display.dispose()
    }

    shell.open()

    while (!display.isDisposed) {
        try {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        } catch (e: NullPointerException) {
            break
        }
    }

    exitProcess(1)
}
