package dropit.ui

import dropit.APP_NAME
import dropit.application.PhoneSessionManager
import dropit.application.dto.TokenStatus
import dropit.application.settings.AppSettings
import dropit.domain.entity.ShowFileAction
import dropit.domain.service.ClipboardService
import dropit.domain.service.PhoneService
import dropit.domain.service.TransferService
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.FileTransfer
import org.eclipse.swt.dnd.ImageTransfer
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class GraphicalInterface @Inject constructor(
    private val eventBus: EventBus,
    private val phoneService: PhoneService,
    private val transferService: TransferService,
    private val phoneSessionManager: PhoneSessionManager,
    private val executor: Executor,
    private val appSettings: AppSettings,
    private val desktopIntegrations: DesktopIntegrations,
    private val display: Display
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

        eventBus.subscribe(ClipboardService.ClipboardReceiveEvent::class) { (data) ->
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

            trayIcon.addListener(SWT.Selection) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                logger.debug("Heap stats: ${usedMemory / (1024 * 1024)} MB used, ${Runtime.getRuntime().totalMemory() / (1024 * 1024)} MB total")
            }

            trayIcon.addListener(SWT.DefaultSelection) {
                logger.info("Default selected")
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

        MenuItem(menu, SWT.PUSH)
            .apply { text = "Send clipboard contents" }
            .apply {
                addListener(SWT.Selection) {
                    sendClipboardToPhone()
                }
            }

        MenuItem(menu, SWT.PUSH)
            .apply { text = t("graphicalInterface.trayIcon.show") }
            .apply { menu.defaultItem = this }
            .apply {
                addListener(SWT.Selection) {
                    logger.info("TODO show main window")
                }
            }

        MenuItem(menu, SWT.PUSH)
            .apply { text = t("graphicalInterface.trayIcon.settings") }
            .apply {
                addListener(SWT.Selection) {
                    logger.info("TODO show settings")
                }
            }

        MenuItem(menu, SWT.SEPARATOR)

        MenuItem(menu, SWT.PUSH)
            .apply { text = t("graphicalInterface.trayIcon.exit") }
            .apply {
                addListener(SWT.Selection) {
                    confirmExit()
                }
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
        if (completedTransfer.transfer.sendToClipboard!! && completedTransfer.locations.size == 1) {
            val path = completedTransfer.locations.values.first().toString()
            val mimeType = completedTransfer.transfer.files.first().mimeType!!
            if (mimeType.startsWith("image/")) {
                val image = Image(display, path)
                Clipboard(display)
                    .apply { setContents(arrayOf(image.imageData), arrayOf(ImageTransfer.getInstance())) }
                    .dispose()
                image.dispose()
            } else {
                Clipboard(display)
                    .apply { setContents(arrayOf(path), arrayOf(FileTransfer.getInstance())) }
                    .dispose()
            }
            toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title", completedTransfer.transfer.files[0].fileName
                ?: "")
            toolTip.message = t("graphicalInterface.trayIcon.balloon.fileIntoClipboard.message")
        } else {
            val autoOpen = appSettings.settings.openTransferOnCompletion
            if (autoOpen) {
                if (completedTransfer.locations.size == 1) {
                    // TODO: do this by mime type?
                    when (appSettings.settings.showTransferAction) {
                        ShowFileAction.OPEN_FOLDER -> desktopIntegrations.openFolderSelectFile(completedTransfer.locations.values.first())
                        ShowFileAction.OPEN_FILE -> desktopIntegrations.openFile(completedTransfer.locations.values.first())
                    }
                } else {
                    desktopIntegrations.openFolder(completedTransfer.transferFolder)
                }
            }

            if (completedTransfer.locations.size == 1) {
                toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title", completedTransfer.transfer.files[0].fileName
                    ?: "")
                toolTip.message = t("graphicalInterface.trayIcon.balloon.fileDownloaded.message")
                if (!autoOpen) {
                    toolTip.addListener(SWT.Selection) {
                        when (appSettings.settings.showTransferAction) {
                            ShowFileAction.OPEN_FOLDER -> desktopIntegrations.openFolderSelectFile(completedTransfer.locations.values.first())
                            ShowFileAction.OPEN_FILE -> desktopIntegrations.openFile(completedTransfer.locations.values.first())
                        }
                    }
                }
            } else {
                toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title", completedTransfer.transfer.files[0].fileName
                    ?: "")
                toolTip.message = t("graphicalInterface.trayIcon.balloon.fileDownloaded.message")
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

    private fun sendClipboardToPhone() {
        val clipboard = Clipboard(display)
        val stringContents = clipboard.getContents(TextTransfer.getInstance()) as String?
        val fileContents = clipboard.getContents(FileTransfer.getInstance()) as Array<String>?
        logger.info("string contents: $stringContents")
        logger.info("file contents: $fileContents")

        if (fileContents != null) {
            MessageBox(shell, SWT.ICON_WARNING or SWT.OK)
                .apply { text = "File sending not implemented" }
                .apply { message = "File sending is not yet implemented." }
                .apply { open() }
            return
        }

        val defaultPhoneId = appSettings.settings.currentPhoneId

        if (defaultPhoneId == null) {
            MessageBox(shell, SWT.ICON_WARNING or SWT.OK)
                .apply { text = APP_NAME }
                .apply { message = t("graphicalInterface.trayIcon.sendClipboard.noPhoneConfigured") }
                .apply { open() }
            return
        }

        executor.execute {
            if (stringContents != null) {
                phoneSessionManager.sendClipboard(defaultPhoneId, stringContents)
            }
        }
    }
}