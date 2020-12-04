package dropit.ui

import dropit.infrastructure.ui.CocoaIntegration
import dropit.infrastructure.ui.GuiIntegrations
import dropit.ui.DesktopIntegrations.OperatingSystem.*
import dropit.ui.image.LinuxPngTransfer
import org.eclipse.swt.dnd.ByteArrayTransfer
import org.eclipse.swt.dnd.ImageTransfer
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesktopIntegrations @Inject constructor() {
    enum class OperatingSystem {
        WINDOWS, LINUX, MACOSX
    }

    val currentOS = System.getProperty("os.name").toLowerCase().let { os ->
        when {
            os.contains("win") -> WINDOWS
            os.contains("mac os") -> MACOSX
            else -> LINUX
        }
    }

    fun buildGuiIntegrations(): GuiIntegrations =
        when (currentOS) {
            MACOSX -> CocoaIntegration()
            else -> GuiIntegrations.Default()
        }

    fun openFolderSelectFile(file: File) {
        when (currentOS) {
            WINDOWS -> ProcessBuilder("explorer.exe", "/select,$file").start()
            MACOSX -> ProcessBuilder("open", "-R", file.toString()).start()
            LINUX -> ProcessBuilder("xdg-open", file.parent).start()
        }
    }

    fun openFolder(folder: File) {
        when (currentOS) {
            WINDOWS -> ProcessBuilder("explorer.exe", folder.toString()).start()
            MACOSX -> ProcessBuilder("open", folder.toString()).start()
            LINUX -> ProcessBuilder("xdg-open", folder.toString()).start()
        }
    }

    fun openFile(file: File) {
        when (currentOS) {
            WINDOWS -> ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", file.toString()).start()
            MACOSX -> ProcessBuilder("open", file.toString()).start()
            LINUX -> ProcessBuilder("xdg-open", file.toString()).start()
        }
    }

    fun openUrl(url: String) {
        Desktop.getDesktop().browse(URI(url))
    }

    /**
     * Workaround for ImageTransfers not working properly on Linux
     */
    fun getImageTransfer(): ByteArrayTransfer =
        when (currentOS) {
            LINUX -> LinuxPngTransfer.instance
            else -> ImageTransfer.getInstance()
        }
}
