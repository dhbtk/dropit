package dropit.application.settings

import dropit.domain.entity.Settings
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.Settings.SETTINGS
import org.jooq.DSLContext
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

@Component
class AppSettings(val create: DSLContext, val applicationContext: ApplicationContext) {
    var firstStart: Boolean = false

    var settings: Settings = createDefaultSettings()
        set(value) {
            field = create.transactionResult { _ ->
                create.update(SETTINGS).set(create.newRecord(SETTINGS, value)).execute()
                create.selectFrom(SETTINGS).fetchOneInto(Settings::class.java)
            }
        }

    private fun createDefaultSettings(): Settings {
        if (create.fetchOne(SETTINGS) == null) {
            val settings = dropit.domain.entity.Settings(
                    computerName = t("appSettings.init.defaultComputerName", System.getProperty("user.name")),
                    rootTransferFolder = getDefaultTransferFolder(),
                    transferFolderName = "yyyy-MM-dd HH-mm %transfer%"
            )
            create.newRecord(SETTINGS, settings).insert()
            firstStart = true
        }
        return create.selectFrom(SETTINGS).fetchOneInto(Settings::class.java)
    }

    private fun getDefaultTransferFolder(): String {
        return if (applicationContext.environment.acceptsProfiles("test")) {
            val folder = Files.createTempDirectory("dropit")
            folder.toString()
        } else {
            val path = Paths.get(System.getProperty("user.home"), t("appSettings.init.defaultTransferFolder"))
            (path.toFile().exists() && path.toFile().isDirectory) || path.toFile().mkdirs() || throw RuntimeException("Could not create default transfer folder $path")
            path.toString()
        }
    }
}