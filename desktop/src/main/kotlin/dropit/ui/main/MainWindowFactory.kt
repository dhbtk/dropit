package dropit.ui.main

import dropit.APP_NAME
import dropit.application.OutgoingService
import dropit.application.settings.AppSettings
import dropit.domain.service.IncomingService
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.infrastructure.ui.GuiIntegrations
import dropit.infrastructure.ui.MenuBuilder
import dropit.ui.DesktopIntegrations
import dropit.ui.service.ClipboardService
import dropit.ui.service.TransferStatusService
import dropit.ui.settings.SettingsWindowFactory
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.*
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainWindowFactory @Inject constructor(
    private val eventBus: EventBus,
    private val phoneService: PhoneService,
    private val incomingService: IncomingService,
    private val outgoingService: OutgoingService,
    private val executor: Executor,
    private val appSettings: AppSettings,
    private val desktopIntegrations: DesktopIntegrations,
    private val clipboardService: ClipboardService,
    private val transferStatusService: TransferStatusService,
    private val guiIntegrations: GuiIntegrations,
    private val settingsWindowFactory: SettingsWindowFactory,
    private val display: Display
) {
    var mainWindow: MainWindow? = null

    @Synchronized
    fun open() {
        val window = mainWindow
        if (window != null) {
            val shell = window.window
            if (!shell.isDisposed) {
                shell.forceActive()
            } else {
                createMainWindow()
            }
        } else {
            createMainWindow()
        }
    }

    private fun createMainWindow() {
        guiIntegrations.beforeWindowOpen()
        mainWindow = MainWindow(
            eventBus,
            phoneService,
            incomingService,
            outgoingService,
            executor,
            appSettings,
            desktopIntegrations,
            clipboardService,
            transferStatusService,
            guiIntegrations,
            settingsWindowFactory,
            display
        )
        display.asyncExec { mainWindow?.window?.forceActive() }
    }
}

@Suppress("MagicNumber")
class MainWindow(
    private val eventBus: EventBus,
    private val phoneService: PhoneService,
    private val incomingService: IncomingService,
    private val outgoingService: OutgoingService,
    private val executor: Executor,
    private val appSettings: AppSettings,
    private val desktopIntegrations: DesktopIntegrations,
    private val clipboardService: ClipboardService,
    private val transferStatusService: TransferStatusService,
    private val guiIntegrations: GuiIntegrations,
    private val settingsWindowFactory: SettingsWindowFactory,
    private val display: Display
) {
    private val windowOpts = if(appSettings.settings.keepWindowOnTop) {
        SWT.SHELL_TRIM or SWT.ON_TOP
    } else {
        SWT.SHELL_TRIM
    }
    val window: Shell = Shell(display, windowOpts)
    val transferTable = TransferTable(eventBus, transferStatusService, display)
    val phoneTable = PhoneTable(window, eventBus, phoneService, display, appSettings, outgoingService)
    val logger = LoggerFactory.getLogger(javaClass)

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
        MenuBuilder(display)
            .menu(
                t("mainWindow.menus.application"),
                Triple(t("mainWindow.menus.application.sendClipboard"), {
                    clipboardService.sendClipboardToPhone(window)
                }, null),
                Triple(t("mainWindow.menus.application.settings"), {
                    settingsWindowFactory.open()
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

        target.addDropListener(dropTargetListener(transferType))
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

    @Suppress("ComplexMethod")
    private fun dropTargetListener(transferType: FileTransfer): DropTargetListener {
        return object : DropTargetListener {
            override fun dragEnter(event: DropTargetEvent) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if (event.operations and DND.DROP_COPY != 0) {
                        event.detail = DND.DROP_COPY
                    } else {
                        event.detail = DND.DROP_NONE
                    }
                }
                // will accept text but prefer to have files dropped
                for (i in 0 until event.dataTypes.size) {
                    if (transferType.isSupportedType(event.dataTypes[i])) {
                        event.currentDataType = event.dataTypes[i]
                        // files should only be copied
                        if (event.detail != DND.DROP_COPY) {
                            event.detail = DND.DROP_NONE
                        }
                        break
                    }
                }
            }

            override fun dragOver(event: DropTargetEvent) {
                event.feedback = DND.FEEDBACK_INSERT_AFTER
            }

            override fun dragOperationChanged(event: DropTargetEvent) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if (event.operations and DND.DROP_COPY != 0) {
                        event.detail = DND.DROP_COPY
                    } else {
                        event.detail = DND.DROP_NONE
                    }
                }
                // allow text to be moved but files should only be copied
                if (transferType.isSupportedType(event.currentDataType)) {
                    if (event.detail != DND.DROP_COPY) {
                        event.detail = DND.DROP_NONE
                    }
                }
            }

            override fun dragLeave(event: DropTargetEvent) {}
            override fun dropAccept(event: DropTargetEvent) {}
            override fun drop(event: DropTargetEvent) {
                if (transferType.isSupportedType(event.currentDataType)) {
                    val files = transferType.nativeToJava(event.currentDataType) as Array<String>?
                    if (files != null) {
                        clipboardService.sendFilesToPhone(window, files)
                    }
                }
            }
        }
    }
}
