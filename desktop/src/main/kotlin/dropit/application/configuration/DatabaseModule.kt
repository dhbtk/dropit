package dropit.application.configuration

import dagger.Module
import dagger.Provides
import dropit.infrastructure.db.CrudListener
import dropit.infrastructure.db.DatabaseInitializer
import dropit.infrastructure.db.RecordMapperProvider
import dropit.infrastructure.db.RecordUnmapperProvider
import org.jooq.SQLDialect
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultRecordListenerProvider
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
        .set(RecordMapperProvider())
        .set(RecordUnmapperProvider(DefaultConfiguration().set(SQLDialect.SQLITE)))
        .set(DefaultRecordListenerProvider(CrudListener()))

    @Provides
    @Singleton
    fun create(jooqConfiguration: org.jooq.Configuration): org.jooq.DSLContext = jooqConfiguration.dsl()
}
