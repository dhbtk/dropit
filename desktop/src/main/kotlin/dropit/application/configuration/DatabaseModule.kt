package dropit.application.configuration

import dagger.Module
import dagger.Provides
import dropit.APP_NAME
import dropit.infrastructure.db.CrudListener
import dropit.infrastructure.db.DatabaseInitializer
import dropit.infrastructure.db.SqlLogger
import dropit.infrastructure.fs.ConfigFolderProvider
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.jooq.impl.DefaultRecordListenerProvider
import java.nio.file.Path
import javax.inject.Named
import javax.inject.Singleton
import javax.sql.DataSource

@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun dataSource(databaseInitializer: DatabaseInitializer) = databaseInitializer.dataSource

    @Provides
    @Singleton
    fun jooqConfiguration(dataSource: DataSource): org.jooq.Configuration = DefaultConfiguration()
        .set(dataSource)
        .set(SQLDialect.SQLITE)
        .set(DefaultRecordListenerProvider(CrudListener()))
        .set(DefaultExecuteListenerProvider(SqlLogger()))

    @Provides
    @Named("databasePath")
    fun databasePath(configFolderProvider: ConfigFolderProvider): Path {
        return configFolderProvider.configFolder.resolve("$APP_NAME.db")
    }

    @Provides
    @Singleton
    fun create(jooqConfiguration: org.jooq.Configuration): org.jooq.DSLContext = jooqConfiguration.dsl()
}
