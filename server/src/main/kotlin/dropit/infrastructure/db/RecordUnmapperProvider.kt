package dropit.infrastructure.db

import org.jooq.Configuration
import org.jooq.Record
import org.jooq.RecordType
import org.jooq.impl.DSL

@Suppress("unchecked_cast", "MaxLineLength", "SpreadOperator")
class RecordUnmapperProvider(private val configuration: Configuration) : org.jooq.RecordUnmapperProvider {
    override fun <E : Any?, R : Record?> provide(type: Class<out E>?, recordType: RecordType<R>?): org.jooq.RecordUnmapper<E, R> {
        return RecordUnmapper(configuration, recordType as RecordType<*>) as org.jooq.RecordUnmapper<E, R>
    }

    class RecordUnmapper(val configuration: Configuration, val recordType: RecordType<*>) : org.jooq.RecordUnmapper<Any, Record> {
        override fun unmap(source: Any?): Record {
            val record = DSL.using(configuration).newRecord(*recordType.fields())
            EntityBridge.copyToRecord(source!!, record)
            return record
        }
    }
}
