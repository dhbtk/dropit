package dropit.infrastructure.fs

import dropit.APP_NAME
import dropit.infrastructure.i18n.t
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigFolderProvider @Inject constructor() {
    val configFolder: Path = System.getProperty("os.name").toLowerCase()
        .let { osName ->
            when {
                osName.contains("win") -> Paths.get(System.getenv("APPDATA"), APP_NAME)
                osName.contains("mac") -> Paths.get(System.getProperty("user.home"), "Library", APP_NAME)
                else -> Paths.get(System.getProperty("user.home"), ".${APP_NAME.toLowerCase()}")
            }
        }.apply {
            val file = this.toFile()
            if (file.exists() && !file.isDirectory) {
                throw IllegalStateException(t("configFolderProvider.init.configFolderObstructed", this.toString()))
            }
            if (!file.exists() && !file.mkdirs()) {
                throw IllegalStateException(t("configFolderProvider.init.couldNotCreateFolder", this.toString()))
            }
        }
}
