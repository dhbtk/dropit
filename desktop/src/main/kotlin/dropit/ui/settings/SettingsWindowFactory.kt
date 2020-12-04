package dropit.ui.settings

import arrow.core.Either
import dropit.application.settings.AppSettings
import dropit.infrastructure.ui.GuiIntegrations
import org.eclipse.swt.widgets.Display
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsWindowFactory @Inject constructor(
    private val appSettings: AppSettings,
    private val guiIntegrations: GuiIntegrations,
    private val display: Display
) {
    var settingsWindow: SettingsWindow? = null

    @Synchronized
    fun open() {
        val window = settingsWindow
        if (window != null) {
            val shell = window.window
            if (!shell.isDisposed) {
                shell.forceActive()
            } else {
                openWindow()
            }
        } else {
            openWindow()
        }
    }

    private fun openWindow() {
        guiIntegrations.beforeWindowOpen()
        settingsWindow = SettingsWindow(appSettings, guiIntegrations, display)
        display.asyncExec { settingsWindow?.window?.forceActive() }
    }
}
