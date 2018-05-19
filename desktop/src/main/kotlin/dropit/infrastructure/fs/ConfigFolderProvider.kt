package dropit.infrastructure.fs

import dropit.APP_NAME
import dropit.infrastructure.i18n.t
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths

@Component
class ConfigFolderProvider {
    final val configFolder: Path
    init {
        val os = System.getProperty("os.name").toLowerCase()
        configFolder = when {
            os.contains("win") -> Paths.get(System.getenv("APPDATA"), APP_NAME)
            os.contains("mac") -> Paths.get(System.getProperty("user.home"), "Library", APP_NAME)
            else -> Paths.get(System.getenv("HOME"), ".${APP_NAME.toLowerCase()}")
        }
        val file = configFolder.toFile()
        if(file.exists() && !file.isDirectory) {
            throw RuntimeException(t("configFolderProvider.init.configFolderObstructed", configFolder.toString()))
        }
        if(!file.exists() && !file.mkdirs()) {
            throw RuntimeException(t("configFolderProvider.init.couldNotCreateFolder", configFolder.toString()))
        }
    }
}