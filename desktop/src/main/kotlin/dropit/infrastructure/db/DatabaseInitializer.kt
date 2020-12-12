package dropit.infrastructure.db

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Named
import javax.sql.DataSource

class DatabaseInitializer @Inject constructor(@Named("databasePath") databasePath: Path) {
//    private val databasePath: Path = if (System.getProperty("dropit.test") == "true") {
//        File.createTempFile("dropit", ".db").apply { deleteOnExit() }.toPath()
//    } else {
//        configFolderProvider.configFolder.resolve("$APP_NAME.db")
//    }
    val dataSource: DataSource = HikariDataSource()
            .apply {
                jdbcUrl = "jdbc:sqlite:$databasePath"
                migrate(this)
            }

    private fun migrate(dataSource: DataSource) {
        Flyway.configure()
                .mixed(true)
                .dataSource(dataSource)
                .load()
                .apply { migrate() }
    }
}
