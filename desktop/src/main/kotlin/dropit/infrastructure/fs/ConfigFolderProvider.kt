package dropit.infrastructure.fs

import dropit.APP_NAME
import dropit.infrastructure.i18n.t
import java.nio.file.Path
import java.nio.file.Paths

class ConfigFolderProvider {
    val configFolder: Path = System.getProperty("os.name").toLowerCase()
        .let {
            when {
                it.contains("win") -> Paths.get(System.getenv("APPDATA"), APP_NAME)
                it.contains("mac") -> Paths.get(System.getProperty("user.home"), "Library", APP_NAME)
                else -> Paths.get(System.getProperty("user.home"), ".${APP_NAME.toLowerCase()}")
            }
        }.apply {
            val file = this.toFile()
            if (file.exists() && !file.isDirectory) {
                throw RuntimeException(t("configFolderProvider.init.configFolderObstructed", this.toString()))
            }
            if (!file.exists() && !file.mkdirs()) {
                throw RuntimeException(t("configFolderProvider.init.couldNotCreateFolder", this.toString()))
            }
        }
}