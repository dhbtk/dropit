package dropit.application.settings

import dropit.domain.entity.Settings
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.Settings.SETTINGS
import org.jooq.DSLContext
import java.nio.file.Files
import java.nio.file.Paths

class AppSettings(val jooq: DSLContext) {
    var firstStart: Boolean = false

    var settings: Settings = createDefaultSettings()
        set(value) {
            field = jooq.transactionResult { _ ->
                jooq.update(SETTINGS).set(jooq.newRecord(SETTINGS, value)).execute()
                jooq.selectFrom(SETTINGS).fetchOneInto(Settings::class.java)
            }
        }

    private fun createDefaultSettings(): Settings {
        if (jooq.fetchOne(SETTINGS) == null) {
            val settings = dropit.domain.entity.Settings(
                    computerName = t("appSettings.init.defaultComputerName", System.getProperty("user.name")),
                    rootTransferFolder = getDefaultTransferFolder(),
                transferFolderName = "{0,date,yyyy-MM-dd HH-mm} {1}",
                    serverPort = 58992
            )
            jooq.newRecord(SETTINGS, settings).insert()
            firstStart = true
        }
        return jooq.selectFrom(SETTINGS).fetchOneInto(Settings::class.java)
    }

    private fun getDefaultTransferFolder(): String {
        return if (System.getProperty("dropit.test") == "true") {
            val folder = Files.createTempDirectory("dropit")
            folder.toString()
        } else {
            val path = Paths.get(System.getProperty("user.home"), t("appSettings.init.defaultTransferFolder"))
            (path.toFile().exists() && path.toFile().isDirectory) || path.toFile().mkdirs() || throw RuntimeException("Could not create default transfer folder $path")
            return path.toString()
        }
    }
}