package dropit.ui

import org.eclipse.swt.widgets.Shell

abstract class ShellContainer {
    abstract val window: Shell

    fun open() {
        window.display.asyncExec { window.forceActive() }
    }
}
