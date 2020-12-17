package dropit.ui.main

import dagger.Lazy
import dropit.APP_NAME
import dropit.application.settings.AppSettings
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.GuiIntegrations
import dropit.infrastructure.ui.windowMenu
import dropit.ui.GraphicalInterface
import dropit.ui.ShellContainer
import dropit.ui.pairing.PairingDialog
import dropit.ui.service.ClipboardService
import dropit.ui.service.TransferStatusMonitor
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.DND
import org.eclipse.swt.dnd.DropTarget
import org.eclipse.swt.dnd.FileTransfer
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Shell
import javax.inject.Inject

@Suppress("MagicNumber")
class MainWindow @Inject constructor(
    eventBus: EventBus,
    appSettings: AppSettings,
    private val clipboardService: ClipboardService,
    transferStatusMonitor: TransferStatusMonitor,
    private val guiIntegrations: GuiIntegrations,
    private val display: Display,
    private val graphicalInterface: Lazy<GraphicalInterface>,
    private val pairingDialog: PairingDialog,
    pairingStatusWidget: PairingStatusWidget
) : ShellContainer() {
    private val windowOpts = if (appSettings.keepWindowOnTop) {
        SWT.SHELL_TRIM or SWT.ON_TOP
    } else {
        SWT.SHELL_TRIM
    }
    override val window: Shell = Shell(display, windowOpts)
    private val transferTable = TransferTable(eventBus, transferStatusMonitor, display)

    init {
        window.text = APP_NAME
        window.image = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
        window.layout = GridLayout(1, false)
            .apply {
                this.marginHeight = 8
                this.marginWidth = 8
            }
        window.addListener(SWT.Close) {
            transferTable.dispose()
            display.asyncExec { guiIntegrations.afterWindowClose() }
        }

        buildWindowMenu()
        pairingStatusWidget.init(window)
        buildDropTarget()
        buildCurrentTransfers(window)
        window.pack()
        window.minimumSize = window.size.apply { x = 600 }
        window.open()
    }

    private fun buildWindowMenu() {
        windowMenu(window) {
            menu(t("mainWindow.menus.application.title")) {
                item(t("mainWindow.menus.application.startPairing"), ::startPairing)
                item(
                    t("mainWindow.menus.application.sendClipboard"),
                    ::sendClipboard,
                    SWT.MOD1 or 'V'.toInt()
                )
                item(
                    t("mainWindow.menus.application.settings"),
                    ::openSettings,
                    SWT.MOD1 or ','.toInt(),
                    SWT.ID_PREFERENCES
                )
                separator()
                item(
                    t("mainWindow.menus.application.exit"),
                    ::exitApp,
                    SWT.MOD1 or 'Q'.toInt(),
                    SWT.ID_QUIT
                )
            }
            menu(t("mainWindow.menus.view.title")) {
                item(t("mainWindow.menus.view.transferLog"))
                item(t("mainWindow.menus.view.clipboardLog"))
            }
            menu(t("mainWindow.menus.help.title")) {
                item(t("mainWindow.menus.help.onlineManual"), {}, SWT.F1)
                item(t("mainWindow.menus.help.googlePlayLink"))
                item(t("mainWindow.menus.help.about"), {}, null, SWT.ID_ABOUT)
            }
        }
    }

    private fun exitApp() {
        graphicalInterface.get().confirmExit()
    }

    private fun startPairing() {
        pairingDialog.open()
    }

    private fun buildDropTarget() {
        DropTarget(window, DND.DROP_MOVE or DND.DROP_COPY or DND.DROP_DEFAULT).apply {
            setTransfer(FileTransfer.getInstance())
            addDropListener(FileDropListener(::sendFiles))
        }
    }

    private fun sendFiles(files: Array<String>): Unit = clipboardService.sendFiles(window, files)

    private fun sendClipboard(): Unit = clipboardService.sendClipboardData(window)

    private fun openSettings(): Unit = graphicalInterface.get().settingsWindow.open()

    private fun buildCurrentTransfers(parent: Composite) {
        Group(parent, SWT.SHADOW_ETCHED_OUT).apply {
            text = t("mainWindow.currentTransfers.title")
            layoutData = GridData(GridData.FILL_HORIZONTAL).apply { minimumHeight = 128 }
            layout = GridLayout(1, false)
            transferTable.init(this)
        }
    }
}
