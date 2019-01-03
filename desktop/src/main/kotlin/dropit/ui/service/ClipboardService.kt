package dropit.ui.service

import dropit.APP_NAME
import dropit.application.OutgoingService
import dropit.application.settings.AppSettings
import dropit.infrastructure.i18n.t
import dropit.ui.DesktopIntegrations
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.FileTransfer
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.ImageLoader
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(
    private val display: Display,
    private val desktopIntegrations: DesktopIntegrations,
    private val appSettings: AppSettings,
    private val outgoingService: OutgoingService,
    private val executor: Executor
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendClipboardToPhone(shell: Shell) {
        val clipboard = Clipboard(display)
        val stringContents = clipboard.getContents(TextTransfer.getInstance()) as String?
        val fileContents = clipboard.getContents(FileTransfer.getInstance()) as Array<String>?
        val imageContents = clipboard.getContents(desktopIntegrations.getImageTransfer()) as ImageData?
        val defaultPhoneId = appSettings.settings.currentPhoneId
        logger.info("string contents: $stringContents")
        logger.info("file contents: $fileContents")
        logger.info("image contents: $imageContents")

        if (defaultPhoneId == null) {
            MessageBox(shell, SWT.ICON_WARNING or SWT.OK)
                .apply { text = APP_NAME }
                .apply { message = t("graphicalInterface.trayIcon.sendClipboard.noPhoneConfigured") }
                .apply { open() }
            return
        }

        executor.execute {
            if (imageContents != null) {
                outgoingService.sendFile(defaultPhoneId, imageClipboardToFile(imageContents))
            } else if (fileContents != null) {
                val files = fileContents.map(::File)
                files.forEach {
                    outgoingService.sendFile(defaultPhoneId, it)
                }
            } else if (stringContents != null) {
                outgoingService.sendClipboard(defaultPhoneId, stringContents)
            }
        }
    }

    fun sendFilesToPhone(shell: Shell, files: Array<String>) {
        val defaultPhoneId = appSettings.settings.currentPhoneId
        if (defaultPhoneId == null) {
            MessageBox(shell, SWT.ICON_WARNING or SWT.OK)
                .apply { text = APP_NAME }
                .apply { message = t("graphicalInterface.trayIcon.sendClipboard.noPhoneConfigured") }
                .apply { open() }
            return
        }

        executor.execute {
            files.forEach {
                outgoingService.sendFile(defaultPhoneId, File(it))
            }
        }
    }

    private fun imageClipboardToFile(imageData: ImageData): File {
        val tempDir = Files.createTempDirectory("dropitClipboard")
        val fileName = "clipboard_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd_kk-mm"))}.png"
        val file = Paths.get(tempDir.toString(), fileName).toFile()
        ImageLoader()
            .apply { data = arrayOf(imageData) }
            .apply { save(file.toString(), SWT.IMAGE_PNG) }
        return file
    }
}