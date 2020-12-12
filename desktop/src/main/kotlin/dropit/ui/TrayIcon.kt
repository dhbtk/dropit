package dropit.ui

import dagger.Lazy
import dropit.APP_NAME
import dropit.application.PhoneSessions
import dropit.application.model.Phones
import dropit.infrastructure.NeedsStop
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.logger
import dropit.ui.service.ClipboardService
import dropit.ui.service.TransferStatusMonitor
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrayIcon @Inject constructor(
    private val display: Display,
    private val graphicalInterface: Lazy<GraphicalInterface>,
    private val eventBus: EventBus,
    private val clipboardService: ClipboardService,
    private val transferStatusMonitor: TransferStatusMonitor,
    private val phoneSessions: PhoneSessions
) : NeedsStop {
    private val shell = Shell(display)
    private val trayImage = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
    private val trayImageConnected = Image(display, javaClass.getResourceAsStream("/ui/icon-connected.png"))
    private val trayImageDisconnected = Image(display, javaClass.getResourceAsStream("/ui/icon-disconnected.png"))
    private val trayIcon = TrayItem(display.systemTray, SWT.NONE)
    var toolTip: ToolTip
        get() = trayIcon.toolTip
        set(value) {
            trayIcon.toolTip = value
        }

    init {
        val menu = buildTrayMenu()
        trayIcon.addListener(SWT.MenuDetect) { menu.visible = true }
        trayIcon.addListener(SWT.Selection) { showMemoryStats() }
        trayIcon.addListener(SWT.DefaultSelection) { graphicalInterface.get().mainWindow.open() }

        listOf(
            Phones.NewPhoneRequestEvent::class,
            Phones.PhoneChangedEvent::class,
            TransferStatusMonitor.TransferUpdatedEvent::class
        ).forEach { event ->
            eventBus.subscribe(event) {
                display.asyncExec { refresh() }
            }
        }

        refresh()
    }

    private fun buildTrayMenu(): Menu {
        val menu = Menu(shell, SWT.POP_UP)

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.sendClipboard")
                addListener(SWT.Selection) {
                    clipboardService.sendClipboardData(shell)
                }
            }

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.show")
                menu.defaultItem = this
                addListener(SWT.Selection) {
                    graphicalInterface.get().mainWindow.open()
                }
            }

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.settings")
                addListener(SWT.Selection) {
                    graphicalInterface.get().settingsWindow.open()
                }
            }

        MenuItem(menu, SWT.SEPARATOR)

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.exit")
                addListener(SWT.Selection) {
                    graphicalInterface.get().confirmExit()
                }
            }

        return menu
    }

    private fun showMemoryStats() {
        val runtime = Runtime.getRuntime()
        System.gc()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024)
        logger.debug("Heap stats: $usedMemory MB used, $totalMemory MB total")
    }

    private fun refresh() {
        val pendingPhone = Phones.pending().firstOrNull()
        val transferringFile = transferStatusMonitor.currentTransfers.firstOrNull()
        when {
            pendingPhone != null -> trayIcon.toolTipText = t(
                "graphicalInterface.trayIcon.tooltip.pendingPhone",
                APP_NAME, pendingPhone.name!!
            )
            transferringFile != null -> trayIcon.toolTipText = t(
                "graphicalInterface.trayIcon.tooltip.downloadingFile",
                APP_NAME,
                "${transferringFile.progress}%",
                transferringFile.humanSpeed()
            )
            else -> trayIcon.toolTipText = APP_NAME
        }
        when {
            Phones.current() == null -> {
                trayIcon.image = trayImage
            }
            phoneSessions.defaultPhoneConnected() -> {
                trayIcon.image = trayImageConnected
            }
            else -> {
                trayIcon.image = trayImageDisconnected
            }
        }
    }

    override fun stop() {
        if (display.isDisposed) return

        display.syncExec { trayIcon.visible = false }
    }
}
