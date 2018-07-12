package dropit.application.configuration

import com.zaxxer.hikari.HikariDataSource
import dropit.APP_NAME
import dropit.infrastructure.db.RecordMapperProvider
import dropit.infrastructure.db.RecordUnmapperProvider
import dropit.infrastructure.fs.ConfigFolderProvider
import org.flywaydb.core.Flyway
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {

    @Bean
    fun dataSource(applicationContext: ApplicationContext, configFolderProvider: ConfigFolderProvider): DataSource {
        val dataSource = HikariDataSource()
        dataSource.jdbcUrl =  if(applicationContext.environment.acceptsProfiles("test")) {
            "jdbc:sqlite:${configFolderProvider.configFolder.resolve("$APP_NAME.test.db")}"
        } else {
            "jdbc:sqlite:${configFolderProvider.configFolder.resolve("$APP_NAME.db")}"
        }
        dataSource.maximumPoolSize = 20
        dataSource.poolName = "pool"
        return dataSource
    }


    @Bean
    fun flyway(dataSource: DataSource): Flyway {
        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()
        return flyway
    }

    @Bean
    fun jooqConfiguration(dataSource: DataSource, flyway: Flyway): org.jooq.Configuration {
        return DefaultConfiguration()
                .set(dataSource)
                .set(SQLDialect.SQLITE)
                .set(RecordMapperProvider())
                .set(RecordUnmapperProvider(DefaultConfiguration().set(SQLDialect.SQLITE)))
    }

    @Bean
    fun create(jooqConfiguration: org.jooq.Configuration) = jooqConfiguration.dsl()
}