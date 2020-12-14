package dropit.ui

import dropit.APP_NAME
import dropit.application.model.FileTransfers
import dropit.application.settings.AppSettings
import dropit.infrastructure.NeedsStart
import dropit.infrastructure.NeedsStop
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.GuiIntegrations
import dropit.ui.main.MainWindow
import dropit.ui.settings.SettingsWindow
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.ToolTip
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class GraphicalInterface @Inject constructor(
    eventBus: EventBus,
    private val appSettings: AppSettings,
    private val display: Display,
    mainWindowProvider: Provider<MainWindow>,
    settingsWindowProvider: Provider<SettingsWindow>,
    private val guiIntegrations: GuiIntegrations,
    private val trayIcon: TrayIcon
) : NeedsStart, NeedsStop {
    private val shell = Shell(display)
    val mainWindow: MainWindow by WindowDelegate(guiIntegrations, mainWindowProvider)
    val settingsWindow: SettingsWindow by WindowDelegate(guiIntegrations, settingsWindowProvider)

    init {
        eventBus.subscribe(FileTransfers.ClipboardReceiveEvent::class) { data ->
            display.asyncExec { receiveClipboardText(data) }
        }
    }

    fun confirmExit() {
        val dialog = MessageBox(shell, SWT.ICON_QUESTION or SWT.OK or SWT.CANCEL)
        dialog.text = t("graphicalInterface.confirmExit.title", APP_NAME)
        dialog.message = t("graphicalInterface.confirmExit.message", APP_NAME)
        if (dialog.open() == SWT.OK) {
            stop()
        }
    }

    private fun receiveClipboardText(data: String) {
        Clipboard(display)
            .apply { setContents(arrayOf(data), arrayOf(TextTransfer.getInstance())) }
            .dispose()
        val toolTip = ToolTip(shell, SWT.BALLOON or SWT.ICON_INFORMATION)
        toolTip.text = t("graphicalInterface.trayIcon.balloon.clipboardReceived.title")
        trayIcon.toolTip = toolTip
        toolTip.visible = true
    }

    override fun start() {
        if (appSettings.firstStart) mainWindow.open()
        guiIntegrations.onGuiInit(appSettings.firstStart)
    }

    override fun stop() {
        if (!display.isDisposed) display.asyncExec { display.dispose() }
    }
}
