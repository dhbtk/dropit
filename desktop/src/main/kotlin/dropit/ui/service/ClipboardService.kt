package dropit.ui.service

import dropit.APP_NAME
import dropit.application.PhoneSessions
import dropit.application.settings.AppSettings
import dropit.infrastructure.i18n.t
import dropit.logger
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
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(
    private val display: Display,
    private val desktopIntegrations: DesktopIntegrations,
    private val appSettings: AppSettings,
    private val phoneSessions: PhoneSessions,
    private val executor: Executor
) {
    fun sendClipboardToPhone(shell: Shell) {
        val clipboard = Clipboard(display)
        val stringContents = clipboard.getContents(TextTransfer.getInstance()) as String?
        val fileContents = clipboard.getContents(FileTransfer.getInstance()) as Array<*>?
        val imageContents = clipboard.getContents(desktopIntegrations.getImageTransfer()) as ImageData?
        val defaultPhoneId = appSettings.currentPhoneId
        logger.info("string contents: $stringContents")
        logger.info("file contents: $fileContents")
        logger.info("image contents: $imageContents")

        if (defaultPhoneId == null) {
            MessageBox(shell, SWT.ICON_WARNING or SWT.OK).apply {
                text = APP_NAME
                message = t("graphicalInterface.trayIcon.sendClipboard.noPhoneConfigured")
                open()
            }
            return
        }

        executor.execute {
            when {
                imageContents != null -> phoneSessions.sendFile(defaultPhoneId, imageClipboardToFile(imageContents))
                fileContents != null -> sendClipboardFiles(fileContents, defaultPhoneId)
                stringContents != null -> phoneSessions.sendClipboard(defaultPhoneId, stringContents)
            }
        }
    }

    private fun sendClipboardFiles(fileContents: Array<*>, defaultPhoneId: UUID) {
        val files = fileContents.filterIsInstance<String>().map(::File)
        files.forEach {
            phoneSessions.sendFile(defaultPhoneId, it)
        }
    }

    fun sendFilesToPhone(shell: Shell, files: Array<String>) {
        val defaultPhoneId = appSettings.currentPhoneId
        if (defaultPhoneId == null) {
            MessageBox(shell, SWT.ICON_WARNING or SWT.OK)
                .apply {
                    text = APP_NAME
                    message = t("graphicalInterface.trayIcon.sendClipboard.noPhoneConfigured")
                    open()
                }
            return
        }

        executor.execute {
            files.forEach {
                phoneSessions.sendFile(defaultPhoneId, File(it))
            }
        }
    }

    private fun imageClipboardToFile(imageData: ImageData): File {
        val tempDir = Files.createTempDirectory("dropitClipboard")
        val fileName = "clipboard_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd_kk-mm"))}.png"
        val file = Paths.get(tempDir.toString(), fileName).toFile()
        ImageLoader().apply {
            data = arrayOf(imageData)
            save(file.toString(), SWT.IMAGE_PNG)
        }
        return file
    }
}
