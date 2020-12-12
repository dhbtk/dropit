package dropit.application.model

import dropit.infrastructure.i18n.t
import dropit.jooq.tables.pojos.Settings
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object DefaultSettings {
    val defaultSettings = createInstance()

    private fun createInstance() = Settings(
        computerName = computerName(),
        rootTransferFolder = transferFolder(),
        transferFolderName = "{0,date,yyyy-MM-dd HH-mm} {1}",
        computerId = UUID.randomUUID(),
        computerSecret = UUID.randomUUID().toString(),
        serverPort = 58992,
        separateTransferFolders = true,
        openTransferOnCompletion = true,
        showTransferAction = ShowFileAction.OPEN_FILE,
        logClipboardTransfers = true,
        keepWindowOnTop = false
    )

    private fun computerName(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: UnknownHostException) {
            t("appSettings.init.defaultComputerName", System.getProperty("user.name"))
        }
    }

    private fun transferFolder(): String {
        return if (System.getProperty("dropit.test") == "true") {
            val folder = Files.createTempDirectory("dropit")
            folder.toString()
        } else {
            val path = Paths.get(System.getProperty("user.home"), t("appSettings.init.defaultTransferFolder"))
            if (!(path.toFile().exists() && path.toFile().isDirectory)) {
                if (!path.toFile().mkdirs()) {
                    throw IllegalStateException("Could not create default transfer folder $path")
                }
            }
            return path.toString()
        }
    }
}
