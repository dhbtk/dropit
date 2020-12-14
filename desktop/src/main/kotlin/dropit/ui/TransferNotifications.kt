package dropit.ui

import dagger.Lazy
import dropit.application.model.FileTransfers
import dropit.application.model.ShowFileAction
import dropit.application.model.files
import dropit.application.model.folder
import dropit.application.settings.AppSettings
import dropit.infrastructure.NeedsStart
import dropit.infrastructure.event.EventBus
import dropit.infrastructure.i18n.t
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.FileTransfer
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.ToolTip
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferNotifications @Inject constructor(
    private val eventBus: EventBus,
    private val display: Display,
    private val trayIcon: Lazy<TrayIcon>,
    private val appSettings: AppSettings,
    private val desktopIntegrations: DesktopIntegrations
) : NeedsStart {
    private val shell = Shell(display)

    private fun notifyTransferFinished(completedTransfer: FileTransfers.CompletedTransfer) {
        val toolTip = ToolTip(shell, SWT.BALLOON or SWT.ICON_INFORMATION)
        if (completedTransfer.transfer.sendToClipboard!! && completedTransfer.locations.size == 1) {
            notifyClipboardFile(completedTransfer, toolTip)
        } else {
            notifyFile(completedTransfer, toolTip)
        }
        trayIcon.get().toolTip = toolTip
        toolTip.visible = true
    }

    private fun notifyFile(completedTransfer: FileTransfers.CompletedTransfer, toolTip: ToolTip) {
        val autoOpen = appSettings.openTransferOnCompletion
        if (autoOpen) {
            openTransfer(completedTransfer)
        }

        if (completedTransfer.locations.size == 1) {
            val transferFile = completedTransfer.transfer.files[0]
            toolTip.text = t("graphicalInterface.trayIcon.balloon.fileDownloaded.title", transferFile.fileName ?: "")
            toolTip.message = t("graphicalInterface.trayIcon.balloon.fileDownloaded.message")
            if (!autoOpen) {
                toolTip.addListener(SWT.Selection) { openLocation(completedTransfer.locations.values.first()) }
            }
        } else {
            toolTip.text = t("graphicalInterface.trayIcon.balloon.transferComplete.title")
            toolTip.message = t("graphicalInterface.trayIcon.balloon.transferComplete.message")
            if (!autoOpen) {
                toolTip.addListener(SWT.Selection) { desktopIntegrations.openFolder(completedTransfer.transfer.folder.toFile()) }
            }
        }
    }

    @Suppress("ForbiddenComment")
    private fun openTransfer(completedTransfer: FileTransfers.CompletedTransfer) {
        if (completedTransfer.locations.size == 1) {
            // TODO: do this by mime type?
            openLocation(completedTransfer.locations.values.first())
        } else {
            desktopIntegrations.openFolder(completedTransfer.transfer.folder.toFile())
        }
    }

    private fun openLocation(location: File) {
        when (appSettings.showTransferAction) {
            ShowFileAction.OPEN_FOLDER -> desktopIntegrations.openFolderSelectFile(location)
            ShowFileAction.OPEN_FILE -> desktopIntegrations.openFile(location)
        }
    }

    private fun notifyClipboardFile(completedTransfer: FileTransfers.CompletedTransfer, toolTip: ToolTip) {
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
        toolTip.text = t(
            "graphicalInterface.trayIcon.balloon.fileDownloaded.title",
            completedTransfer.transfer.files[0].fileName ?: ""
        )
        toolTip.message = t("graphicalInterface.trayIcon.balloon.fileIntoClipboard.message")
    }

    override fun start() {
        eventBus.subscribe(FileTransfers.TransferCompleteEvent::class) { completedTransfer ->
            display.asyncExec { notifyTransferFinished(completedTransfer) }
        }
    }
}
