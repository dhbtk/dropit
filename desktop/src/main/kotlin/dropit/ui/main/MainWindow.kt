package dropit.ui.main

import dagger.Lazy
import dropit.APP_NAME
import dropit.application.PhoneSessions
import dropit.application.settings.AppSettings
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.GuiIntegrations
import dropit.infrastructure.ui.MenuBuilder
import dropit.ui.GraphicalInterface
import dropit.ui.ShellContainer
import dropit.ui.service.ClipboardService
import dropit.ui.service.TransferStatusService
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.*
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import javax.inject.Inject

@Suppress("MagicNumber")
class MainWindow @Inject constructor(
    eventBus: EventBus,
    phoneService: PhoneService,
    phoneSessions: PhoneSessions,
    appSettings: AppSettings,
    private val clipboardService: ClipboardService,
    transferStatusService: TransferStatusService,
    private val guiIntegrations: GuiIntegrations,
    private val display: Display,
    private val graphicalInterface: Lazy<GraphicalInterface>
): ShellContainer() {
    private val windowOpts = if (appSettings.keepWindowOnTop) {
        SWT.SHELL_TRIM or SWT.ON_TOP
    } else {
        SWT.SHELL_TRIM
    }
    override val window: Shell = Shell(display, windowOpts)
    val transferTable = TransferTable(eventBus, transferStatusService, display)
    val phoneTable = PhoneTable(window, eventBus, phoneService, display, appSettings, phoneSessions)

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
        PairingQrCode(appSettings, window)
        window.pack()
        window.minimumSize = window.size.apply { x = 440 }
        window.open()
    }

    private fun buildWindowMenu() {
        MenuBuilder(display)
                .menu(
                        t("mainWindow.menus.application"),
                        Triple(t("mainWindow.menus.application.sendClipboard"), {
                            clipboardService.sendClipboardToPhone(window)
                        }, null),
                        Triple(t("mainWindow.menus.application.settings"), {
                            graphicalInterface.get().settingsWindow.open()
                        }, SWT.ID_PREFERENCES),
                        Triple(null, null, null),
                        Triple(t("mainWindow.menus.application.exit"), null, SWT.ID_QUIT)
                )
                .menu(
                        t("mainWindow.menus.view"),
                        Triple(t("mainWindow.menus.view.transferLog"), null, null),
                        Triple(t("mainWindow.menus.view.clipboardLog"), null, null)
                )
                .menu(
                        t("mainWindow.menus.help"),
                        Triple(t("mainWindow.menus.help.onlineManual"), null, null),
                        Triple(t("mainWindow.menus.help.googlePlayLink"), null, null),
                        Triple(t("mainWindow.menus.help.about"), null, SWT.ID_ABOUT)
                )
                .build(window)
    }

    private fun buildDropZone(parent: Composite) {
        val group = Group(parent, SWT.SHADOW_ETCHED_OUT)
        group.text = t("mainWindow.dropZone.title")
        group.layoutData = GridData(GridData.FILL_HORIZONTAL)
                .apply {
                    minimumHeight = 128
                }
        GridLayout(1, false)
                .apply {
                    marginTop = 32
                    marginBottom = 32
                    group.layout = this
                }

        Label(group, SWT.CENTER)
                .apply {
                    text = t("mainWindow.dropZone.dndLabel")
                    layoutData = GridData(GridData.FILL_HORIZONTAL)
                    pack()
                }

        Label(group, SWT.CENTER)
                .apply {
                    text = t("mainWindow.dropZone.or")
                    layoutData = GridData(GridData.FILL_HORIZONTAL)
                    pack()
                }

        val clipboardComposite = Composite(group, 0)
        val clipboardLayout = GridLayout(2, false)
        clipboardComposite.layout = clipboardLayout
        clipboardComposite.layoutData = GridData(GridData.HORIZONTAL_ALIGN_CENTER)

        Button(clipboardComposite, SWT.PUSH)
                .apply {
                    text = t("mainWindow.dropZone.clickHere")
                    pack()
                    addListener(SWT.Selection) {
                        clipboardService.sendClipboardToPhone(window)
                    }
                }
        Label(clipboardComposite, SWT.LEFT)
                .apply {
                    text = t("mainWindow.dropZone.sendClipboard")
                    pack()
                }
        clipboardComposite.pack()

        group.pack()

        val target = DropTarget(group, DND.DROP_MOVE or DND.DROP_COPY or DND.DROP_DEFAULT)
        val transferType = FileTransfer.getInstance()
        target.setTransfer(transferType)

        target.addDropListener(FileDropListener(transferType, window, clipboardService))
    }

    private fun buildCurrentTransfers(parent: Composite) {
        val group = Group(parent, SWT.SHADOW_ETCHED_OUT)
        group.text = t("mainWindow.currentTransfers.title")
        group.layoutData = GridData(GridData.FILL_HORIZONTAL)
                .apply {
                    minimumHeight = 128
                }
        GridLayout(1, false)
                .apply {
                    group.layout = this
                }

        transferTable.init(group)
    }

    private fun buildPhoneDetails(parent: Composite) {
        val group = Group(parent, SWT.SHADOW_ETCHED_OUT)
        group.text = t("mainWindow.phoneDetails.title")
        group.layoutData = GridData(GridData.FILL_HORIZONTAL)
                .apply {
                    minimumHeight = 128
                }
        GridLayout(1, false)
                .apply {
                    group.layout = this
                }

        phoneTable.init(group)
    }
}
