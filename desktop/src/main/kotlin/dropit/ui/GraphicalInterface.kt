package dropit.ui

import dropit.APP_NAME
import dropit.application.OutgoingService
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.ShowFileAction
import dropit.domain.service.IncomingService
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import dropit.ui.main.MainWindowFactory
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.FileTransfer
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.ToolTip
import org.eclipse.swt.widgets.TrayItem
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class GraphicalInterface @Inject constructor(
    private val eventBus: EventBus,
    private val phoneService: PhoneService,
    private val incomingService: IncomingService,
    private val outgoingService: OutgoingService,
    private val executor: Executor,
    private val appSettings: AppSettings,
    private val desktopIntegrations: DesktopIntegrations,
    private val clipboardService: dropit.ui.service.ClipboardService,
    private val display: Display,
    private val mainWindowFactory: MainWindowFactory
) {
    val logger = LoggerFactory.getLogger(javaClass)
    private val shell = Shell(display)
    private val trayImage = Image(display, javaClass.getResourceAsStream("/ui/icon.png"))
    private val trayIcon = setupTrayIcon()

    init {
        eventBus.subscribe(PhoneService.NewPhoneRequestEvent::class) { (phone) ->
            logger.info("Auto approving phone $phone")
            phoneService.authorizePhone(phone.id!!)
        }

        eventBus.subscribe(IncomingService.ClipboardReceiveEvent::class) { (data) ->
            display.asyncExec {
                receiveClipboardText(data)
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

            @Suppress("MagicNumber")
            trayIcon.addListener(SWT.Selection) {
                val runtime = Runtime.getRuntime()
                System.gc()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024)
                logger.debug("Heap stats: $usedMemory MB used, $totalMemory MB total")
            }

            trayIcon.addListener(SWT.DefaultSelection) {
                logger.info("Default selected")
                mainWindowFactory.open()
            }

            listOf(
                PhoneService.NewPhoneRequestEvent::class,
                PhoneService.PhoneChangedEvent::class,
                IncomingService.FileTransferBeginEvent::class,
                IncomingService.FileTransferReceiveEvent::class,
                IncomingService.FileTransferFinishEvent::class
            ).forEach { event ->
                eventBus.subscribe(event) {
                    display.asyncExec { refreshTrayIcon() }
                }
            }

            eventBus.subscribe(PhoneService.PhoneChangedEvent::class) {
                display.asyncExec { refreshTrayIcon() }
            }

            eventBus.subscribe(IncomingService.TransferCompleteEvent::class) { (completedTransfer) ->
                display.asyncExec { notifyTransferFinished(completedTransfer) }
            }

            return trayIcon
        }
        return null
    }

    private fun buildTrayMenu(trayItem: TrayItem): Menu {
        val menu = Menu(shell, SWT.POP_UP)

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.sendClipboard")
                addListener(SWT.Selection) {
                    clipboardService.sendClipboardToPhone(shell)
                }
            }

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.show")
                menu.defaultItem = this
                addListener(SWT.Selection) {
                    mainWindowFactory.open()
                }
            }

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.settings")
                addListener(SWT.Selection) {
                    logger.info("TODO show settings")
                }
            }

        MenuItem(menu, SWT.SEPARATOR)

        MenuItem(menu, SWT.PUSH)
            .apply {
                text = t("graphicalInterface.trayIcon.exit")
                addListener(SWT.Selection) {
                    confirmExit()
                }
            }

        return menu
    }

    @Suppress("MagicNumber")
    private fun refreshTrayIcon() {
        val pendingPhone = phoneService.listPhones(false).find { it.status == TokenStatus.PENDING }
        val transferingFile = incomingService.transferTimes.keys.let { set ->
            if (set.isEmpty()) {
                null
            } else {
                val first = set.first()
                Pair(first, incomingService.transferTimes[first]!!)
            }
        }
        if (pendingPhone != null) {
            trayIcon?.toolTipText = t("graphicalInterface.trayIcon.tooltip.pendingPhone",
                APP_NAME, pendingPhone.name!!)
        } else if (transferingFile != null && !transferingFile.second.isEmpty()) {
            val (transferFile, data) = transferingFile
            val percentage = (((data.last().second).toFloat() / transferFile.fileSize!!) * 100).roundToInt()
            val bytesPerSec = incomingService.calculateTransferRate(data)
            trayIcon?.toolTipText = t("graphicalInterface.trayIcon.tooltip.downloadingFile",
                APP_NAME,
                "$percentage%",
                bytesToHuman(bytesPerSec))
        } else {
            trayIcon?.toolTipText = APP_NAME
        }
    }

    @Suppress("MagicNumber")
    private fun bytesToHuman(bytes: Long): String {
        return when {
            bytes > (1024 * 1024) -> bytes.let { it.toDouble() / (1024 * 1024) }.let {
                "${String.format(Locale.getDefault(), "%.1f", it)} MB/s"
            }
            bytes > 1024 -> "${bytes / 1024} kB/s"
            else -> "$bytes B/s"
        }
    }

    private fun notifyTransferFinished(completedTransfer: IncomingService.CompletedTransfer) {
        val toolTip = ToolTip(shell, SWT.BALLOON or SWT.ICON_INFORMATION)
        if (completedTransfer.transfer.sendToClipboard!! && completedTransfer.locations.size == 1) {
            notifyClipboardFile(completedTransfer, toolTip)
        } else {
            val autoOpen = appSettings.settings.openTransferOnCompletion
            if (autoOpen) {
                openTransfer(completedTransfer)
            }

            if (completedTransfer.locations.size == 1) {
                val transferFile = completedTransfer.transfer.files[0]
                toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title",
                    transferFile.fileName ?: "")
                toolTip.message = t("graphicalInterface.trayIcon.balloon.fileDownloaded.message")
                if (!autoOpen) {
                    toolTip.addListener(SWT.Selection) {
                        openLocation(completedTransfer.locations.values.first())
                    }
                }
            } else {
                toolTip.text = t("graphicalInterface.trayIcon.balloon.transferComplete.title")
                toolTip.message = t("graphicalInterface.trayIcon.balloon.transferComplete.message")
                if (!autoOpen) {
                    toolTip.addListener(SWT.Selection) {
                        desktopIntegrations.openFolder(completedTransfer.transferFolder)
                    }
                }
            }
        }
        trayIcon?.toolTip = toolTip
        toolTip.visible = true
    }

    @Suppress("ForbiddenComment")
    private fun openTransfer(completedTransfer: IncomingService.CompletedTransfer) {
        if (completedTransfer.locations.size == 1) {
            // TODO: do this by mime type?
            openLocation(completedTransfer.locations.values.first())
        } else {
            desktopIntegrations.openFolder(completedTransfer.transferFolder)
        }
    }

    private fun openLocation(location: File) {
        when (appSettings.settings.showTransferAction) {
            ShowFileAction.OPEN_FOLDER -> desktopIntegrations.openFolderSelectFile(location)
            ShowFileAction.OPEN_FILE -> desktopIntegrations.openFile(location)
        }
    }

    private fun notifyClipboardFile(completedTransfer: IncomingService.CompletedTransfer, toolTip: ToolTip) {
        val path = completedTransfer.locations.values.first().toString()
        val mimeType = completedTransfer.transfer.files.first().mimeType!!
        if (mimeType.startsWith("image/")) {
            val image = Image(display, path)
            Clipboard(display)
                .apply { setContents(arrayOf(image.imageData), arrayOf(desktopIntegrations.getImageTransfer())) }
                .dispose()
            image.dispose()
        } else {
            Clipboard(display)
                .apply { setContents(arrayOf(path), arrayOf(FileTransfer.getInstance())) }
                .dispose()
        }
        toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title",
            completedTransfer.transfer.files[0].fileName ?: "")
        toolTip.message = t("graphicalInterface.trayIcon.balloon.fileIntoClipboard.message")
    }

    private fun receiveClipboardText(data: String) {
        Clipboard(display)
            .apply { setContents(arrayOf(data), arrayOf(TextTransfer.getInstance())) }
            .dispose()
        if (trayIcon != null) {
            val toolTip = ToolTip(shell, SWT.BALLOON or SWT.ICON_INFORMATION)
            toolTip.text = t("graphicalInterface.trayIcon.balloon.clipboardReceived.title")
            trayIcon.toolTip = toolTip
            toolTip.visible = true
        }
    }
}