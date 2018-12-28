package dropit.ui

import dropit.APP_NAME
import dropit.application.dto.TokenStatus
import dropit.domain.service.ClipboardService
import dropit.domain.service.PhoneService
import dropit.domain.service.TransferService
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class GraphicalInterface @Inject constructor(
    private val eventBus: EventBus,
    private val phoneService: PhoneService,
    private val transferService: TransferService,
    private val display: Display
) {
    val log = LoggerFactory.getLogger(javaClass)
    private val shell = Shell(display)
    private val trayImage = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
    private val trayIcon = setupTrayIcon()

    init {
        eventBus.subscribe(PhoneService.NewPhoneRequestEvent::class) { (phone) ->
            log.info("Auto approving phone $phone")
            phoneService.authorizePhone(phone.id!!)
        }

        eventBus.subscribe(ClipboardService.ClipboardReceiveEvent::class) { (data) ->
            display.asyncExec {
                Clipboard(display)
                    .setContents(arrayOf(data), arrayOf(TextTransfer.getInstance()))
                if (trayIcon != null) {
                    val toolTip = ToolTip(shell, SWT.BALLOON or SWT.ICON_INFORMATION)
                    toolTip.text = t("graphicalInterface.trayIcon.balloon.clipboardReceived.title")
                    trayIcon.toolTip = toolTip
                    toolTip.visible = true
                }
            }
        }
    }

    fun confirmExit() {
        val dialog = MessageBox(shell, SWT.ICON_QUESTION or SWT.OK or SWT.CANCEL)
        dialog.text = t("graphicalInterface.confirmExit.title", APP_NAME)
        dialog.message = t("graphicalInterface.confirmExit.message", APP_NAME)
        if (dialog.open() == SWT.OK) {
            display.dispose()
        }
    }

    private fun setupTrayIcon(): TrayItem? {
        val tray = display.systemTray
        if (tray != null) {
            val trayIcon = TrayItem(tray, SWT.NONE)
            trayIcon.toolTipText = APP_NAME
            trayIcon.image = trayImage

            val menu = buildTrayMenu(trayIcon)
            trayIcon.addListener(SWT.MenuDetect) {
                menu.visible = true
            }

            trayIcon.addListener(SWT.Selection) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                log.debug("Heap stats: ${usedMemory / (1024 * 1024)} MB used, ${Runtime.getRuntime().totalMemory() / (1024 * 1024)} MB total")
            }

            trayIcon.addListener(SWT.DefaultSelection) {
                log.info("Default selected")
            }

            listOf(
                PhoneService.NewPhoneRequestEvent::class,
                PhoneService.PhoneChangedEvent::class,
                TransferService.FileTransferBeginEvent::class,
                TransferService.FileTransferReceiveEvent::class,
                TransferService.FileTransferFinishEvent::class
            ).forEach {
                eventBus.subscribe(it) {
                    display.asyncExec { refreshTrayIcon() }
                }
            }

            eventBus.subscribe(PhoneService.PhoneChangedEvent::class) {
                display.asyncExec { refreshTrayIcon() }
            }

            eventBus.subscribe(TransferService.TransferCompleteEvent::class) { (completedTransfer) ->
                display.asyncExec { notifyTransferFinished(completedTransfer) }
            }

            return trayIcon
        }
        return null
    }

    private fun buildTrayMenu(trayItem: TrayItem): Menu {
        val menu = Menu(shell, SWT.POP_UP)

        val showItem = MenuItem(menu, SWT.PUSH)
            .apply { text = t("graphicalInterface.trayIcon.show") }
            .apply { menu.defaultItem = this }
        showItem.addListener(SWT.Selection) {
            log.info("TODO show main window")
        }

        val settingsItem = MenuItem(menu, SWT.PUSH)
            .apply { text = t("graphicalInterface.trayIcon.settings") }
        settingsItem.addListener(SWT.Selection) {
            log.info("TODO show settings")
        }

        MenuItem(menu, SWT.SEPARATOR)

        val exitItem = MenuItem(menu, SWT.PUSH)
            .apply { text = t("graphicalInterface.trayIcon.exit") }
        exitItem.addListener(SWT.Selection) {
            confirmExit()
        }

        return menu
    }

    private fun refreshTrayIcon() {
        val pendingPhone = phoneService.listPhones(false).find { it.status == TokenStatus.PENDING }
        val transferingFile = transferService.transferTimes.keys.let {
            if (it.isEmpty()) {
                null
            } else {
                val first = it.first()
                Pair(first, transferService.transferTimes[first]!!)
            }
        }
        if (pendingPhone != null) {
            trayIcon?.toolTipText = t("graphicalInterface.trayIcon.tooltip.pendingPhone", APP_NAME, pendingPhone.name!!)
        } else if (transferingFile != null && !transferingFile.second.isEmpty()) {
            val (transferFile, data) = transferingFile
            val percentage = (((data.last().second).toFloat() / transferFile.fileSize!!) * 100).roundToInt()
            val bytesPerSec = transferService.calculateTransferRate(data)
            trayIcon?.toolTipText = t("graphicalInterface.trayIcon.tooltip.downloadingFile",
                APP_NAME,
                "$percentage%",
                bytesToHuman(bytesPerSec))
        } else {
            trayIcon?.toolTipText = APP_NAME
        }
    }

    private fun bytesToHuman(bytes: Long): String {
        return when {
            bytes > (1024 * 1024) -> "${String.format(Locale.getDefault(), "%.1f", bytes.toDouble() / (1024 * 1024))} MB/s"
            bytes > 1024 -> "${bytes / 1024} kB/s"
            else -> "$bytes B/s"
        }
    }

    private fun notifyTransferFinished(completedTransfer: TransferService.CompletedTransfer) {
        val toolTip = ToolTip(shell, SWT.BALLOON or SWT.ICON_INFORMATION)
        if (completedTransfer.locations.size == 1) {
            toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title", completedTransfer.transfer.files[0].fileName
                ?: "")
            toolTip.message = t("graphicalInterface.trayIcon.balloon.fileDownloaded.message")
            toolTip.addListener(SWT.Selection) {
                log.info("TODO open file")
            }
        } else {
            toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title", completedTransfer.transfer.files[0].fileName
                ?: "")
            toolTip.message = t("graphicalInterface.trayIcon.balloon.fileDownloaded.message")
            toolTip.addListener(SWT.Selection) {
                log.info("TODO open folder")
            }
        }
        trayIcon?.toolTip = toolTip
        toolTip.visible = true
    }
}