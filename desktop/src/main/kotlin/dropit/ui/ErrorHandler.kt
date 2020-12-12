package dropit.ui

import dropit.APP_NAME
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import javax.inject.Inject

class ErrorHandler @Inject constructor(
    private val display: Display,
    private val desktopIntegrations: DesktopIntegrations
    ) {
    val shell = Shell(display, SWT.DIALOG_TRIM).apply {
        text = APP_NAME
        image = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
        size = Point(640, 320)
        layout = GridLayout(1, false)
    }


}
