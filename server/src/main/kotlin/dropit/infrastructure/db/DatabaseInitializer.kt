package dropit.infrastructure.db

import com.zaxxer.hikari.HikariDataSource
import dropit.APP_NAME
import dropit.infrastructure.fs.ConfigFolderProvider
import org.flywaydb.core.Flyway
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import javax.sql.DataSource

class DatabaseInitializer @Inject constructor(configFolderProvider: ConfigFolderProvider) {
    val dataSource: DataSource = HikariDataSource()
            .apply {
                jdbcUrl = "jdbc:sqlite:$databasePath"
                migrate(this)
            }
    val databasePath: Path = if (System.getProperty("dropit.test") == "true") {
        File.createTempFile("dropit", ".db").apply { deleteOnExit() }.toPath()
    } else {
        configFolderProvider.configFolder.resolve("$APP_NAME.db")
    }

    private fun migrate(dataSource: DataSource) {
        Flyway.configure()
                .mixed(true)
                .dataSource(dataSource)
                .load()
                .apply { migrate() }
    }
}
