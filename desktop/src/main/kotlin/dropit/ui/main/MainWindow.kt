package dropit.ui.main

import dagger.Lazy
import dropit.APP_NAME
import dropit.application.PhoneSessions
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
import org.eclipse.swt.widgets.*
import javax.inject.Inject

@Suppress("MagicNumber")
class MainWindow @Inject constructor(
    eventBus: EventBus,
    phoneSessions: PhoneSessions,
    appSettings: AppSettings,
    private val clipboardService: ClipboardService,
    transferStatusMonitor: TransferStatusMonitor,
    private val guiIntegrations: GuiIntegrations,
    private val display: Display,
    private val graphicalInterface: Lazy<GraphicalInterface>,
    private val pairingDialog: PairingDialog
) : ShellContainer() {
    private val windowOpts = if (appSettings.keepWindowOnTop) {
        SWT.SHELL_TRIM or SWT.ON_TOP
    } else {
        SWT.SHELL_TRIM
    }
    override val window: Shell = Shell(display, windowOpts)
    private val transferTable = TransferTable(eventBus, transferStatusMonitor, display)
    private val phoneTable = PhoneTable(eventBus, display, appSettings, phoneSessions)

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
            phoneTable.dispose()
            display.asyncExec { guiIntegrations.afterWindowClose() }
        }

        buildWindowMenu()
        buildDropZone(window)
        buildCurrentTransfers(window)
        buildPhoneDetails(window)
        window.pack()
        window.minimumSize = window.size.apply { x = 440 }
        window.open()
    }

    private fun buildWindowMenu() {
        windowMenu(window) {
            menu(t("mainWindow.menus.application.title")) {
                item(t("mainWindow.menus.application.startPairing"), ::startPairing)
                item(t("mainWindow.menus.application.sendClipboard"), ::sendClipboard)
                item(t("mainWindow.menus.application.settings"), ::openSettings, SWT.ID_PREFERENCES)
                separator()
                item(t("mainWindow.menus.application.exit"), {}, SWT.ID_QUIT)
            }
            menu(t("mainWindow.menus.view.title")) {
                item(t("mainWindow.menus.view.transferLog"))
                item(t("mainWindow.menus.view.clipboardLog"))
            }
            menu(t("mainWindow.menus.help.title")) {
                item(t("mainWindow.menus.help.onlineManual"))
                item(t("mainWindow.menus.help.googlePlayLink"))
                item(t("mainWindow.menus.help.about"), {}, SWT.ID_ABOUT)
            }
        }
    }

    private fun startPairing() {
        pairingDialog.open()
    }

    private fun buildDropZone(parent: Composite) {
        val group = Group(parent, SWT.SHADOW_ETCHED_OUT)
        group.text = t("mainWindow.dropZone.title")
        group.layoutData = GridData(GridData.FILL_HORIZONTAL).apply { minimumHeight = 128 }
        group.layout = GridLayout(1, false).apply {
            marginTop = 32
            marginBottom = 32
        }

        Label(group, SWT.CENTER).apply {
            text = t("mainWindow.dropZone.dndLabel")
            layoutData = GridData(GridData.FILL_HORIZONTAL)
            pack()
        }

        Label(group, SWT.CENTER).apply {
            text = t("mainWindow.dropZone.or")
            layoutData = GridData(GridData.FILL_HORIZONTAL)
            pack()
        }

        val clipboardComposite = Composite(group, 0)
        val clipboardLayout = GridLayout(2, false)
        clipboardComposite.layout = clipboardLayout
        clipboardComposite.layoutData = GridData(GridData.HORIZONTAL_ALIGN_CENTER)

        Button(clipboardComposite, SWT.PUSH).apply {
            text = t("mainWindow.dropZone.clickHere")
            pack()
            addListener(SWT.Selection) { sendClipboard() }
        }
        Label(clipboardComposite, SWT.LEFT).apply {
            text = t("mainWindow.dropZone.sendClipboard")
            pack()
        }
        clipboardComposite.pack()

        group.pack()

        DropTarget(group, DND.DROP_MOVE or DND.DROP_COPY or DND.DROP_DEFAULT).apply {
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

    private fun buildPhoneDetails(parent: Composite) {
        Group(parent, SWT.SHADOW_ETCHED_OUT).apply {
            text = t("mainWindow.phoneDetails.title")
            layoutData = GridData(GridData.FILL_HORIZONTAL).apply { minimumHeight = 128 }
            layout = GridLayout(1, false)
            phoneTable.init(this)
        }
    }
}
