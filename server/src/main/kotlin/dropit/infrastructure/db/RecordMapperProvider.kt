package dropit.infrastructure.db

import org.jooq.Record
import org.jooq.RecordMapperProvider
import org.jooq.RecordType
import kotlin.reflect.KClass

@Suppress("unchecked_cast")
class RecordMapperProvider : RecordMapperProvider {
    override fun <R : Record?, E : Any?> provide(recordType: RecordType<R>?, type: Class<out E>?):
        org.jooq.RecordMapper<R, E> {
        return RecordMapper((type as Class<Any>).kotlin as KClass<*>) as org.jooq.RecordMapper<R, E>
    }

    class RecordMapper(val type: KClass<*>) : org.jooq.RecordMapper<Record, Any> {
        override fun map(record: Record?): Any = recordToData(record!!, type)

        private fun <T : Any> recordToData(source: Record, dest: KClass<T>): T {
            return EntityBridge.recordToEntity(source, dest) as T
        }
    }
}