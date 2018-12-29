package dropit.infrastructure.db

import org.apache.commons.lang3.ClassUtils.primitiveToWrapper
import org.jooq.Configuration
import org.jooq.Field
import org.jooq.Record
import org.jooq.RecordType
import org.jooq.impl.DSL
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

@Suppress("unchecked_cast")
class RecordUnmapperProvider(private val configuration: Configuration) : org.jooq.RecordUnmapperProvider {
    override fun <E : Any?, R : Record?> provide(type: Class<out E>?, recordType: RecordType<R>?): org.jooq.RecordUnmapper<E, R> {
        return RecordUnmapper(configuration, recordType as RecordType<*>) as org.jooq.RecordUnmapper<E, R>
    }

    class RecordUnmapper(val configuration: Configuration, val recordType: RecordType<*>) : org.jooq.RecordUnmapper<Any, Record> {
        override fun unmap(source: Any?): Record {
            val properties = source!!::class.members.filter { it is KProperty }.map{ it as KProperty }
            val record = DSL.using(configuration).newRecord(*recordType.fields())
            record.fields().forEach { field ->
                val property = properties.find { it.name == field.name.camelCase() }
                if(property != null) {
                    val fieldClass = field.type
                    val propClass = boxIfNeeded(property.returnType.jvmErasure.java)
                    if(fieldClass.isAssignableFrom(propClass)) {
                        record.set(field as Field<Any?>, property.getter.call(source))
                    } else if (Number::class.java.isAssignableFrom(fieldClass) && propClass == java.lang.Boolean::class.java) {
                        val number = if (property.getter.call(source) as Boolean) 1 else 0
                        record.set(field as Field<Number>, number)
                    } else if(fieldClass == String::class.java && propClass == UUID::class.java) {
                        record.set(field as Field<String?>, property.getter.call(source)?.toString())
                    } else if(fieldClass == String::class.java && Enum::class.java.isAssignableFrom(propClass)) {
                        record.set(field as Field<String?>, (property.getter.call(source) as Enum<*>?)?.name)
                    } else if(fieldClass == Timestamp::class.java && propClass == LocalDateTime::class.java) {
                        val value = property.getter.call(source) as LocalDateTime?
                        if(value != null) {
                            record.set(field as Field<Timestamp?>, Timestamp.valueOf(value))
                        }
                    }
                }
            }
            return record
        }

        private fun boxIfNeeded(sourceClass: Class<*>): Class<*> {
            return primitiveToWrapper(sourceClass)
        }

    }
}

private fun String.camelCase(): String {
    return this.split("_").mapIndexed { i, s -> if(i == 0) { s.toLowerCase() } else { s.toLowerCase().capitalize() } }.joinToString("")
}
