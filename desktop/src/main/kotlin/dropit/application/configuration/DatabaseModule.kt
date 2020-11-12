package dropit.application.configuration

import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import dropit.APP_NAME
import dropit.infrastructure.db.CrudListener
import dropit.infrastructure.db.RecordMapperProvider
import dropit.infrastructure.db.RecordUnmapperProvider
import dropit.infrastructure.fs.ConfigFolderProvider
import org.flywaydb.core.Flyway
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultRecordListenerProvider
import java.io.File
import javax.inject.Singleton
import javax.sql.DataSource

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun dataSource(configFolderProvider: ConfigFolderProvider): DataSource = HikariDataSource()
        .apply {
            jdbcUrl = if (System.getProperty("dropit.test") == "true") {
                val file = File.createTempFile("dropit", ".db")
                file.deleteOnExit()
                "jdbc:sqlite:$file"
            } else {
                "jdbc:sqlite:${configFolderProvider.configFolder.resolve("$APP_NAME.db")}"
            }
        }


    @Provides
    @Singleton
    fun flyway(dataSource: DataSource) = Flyway.configure()
            .mixed(true)
        .dataSource(dataSource)
        .load()
        .apply { migrate() }

    @Provides
    @Singleton
    fun jooqConfiguration(dataSource: DataSource, flyway: Flyway): org.jooq.Configuration = DefaultConfiguration()
        .set(dataSource)
        .set(SQLDialect.SQLITE)
        .set(RecordMapperProvider())
        .set(RecordUnmapperProvider(DefaultConfiguration().set(SQLDialect.SQLITE)))
        .set(DefaultRecordListenerProvider(CrudListener()))

    @Provides
    @Singleton
    fun create(jooqConfiguration: org.jooq.Configuration): org.jooq.DSLContext = jooqConfiguration.dsl()
}
