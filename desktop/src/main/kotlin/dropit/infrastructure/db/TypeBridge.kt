@file:Suppress("unchecked_cast")

package dropit.infrastructure.db

import org.jooq.Field
import org.jooq.Record
import java.beans.PropertyDescriptor
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

/**
 * Class name convention: RecordToSourceTypeBridge
 */
interface TypeBridge {
    fun matches(sourcePropertyClass: Class<*>, recordFieldClass: Class<*>): Boolean

    fun convertToRecord(source: Any, sourceProperty: KProperty<*>, record: Record, recordField: Field<*>)

    fun convertToEntity(parameter: KParameter, property: PropertyDescriptor, value: Any?): Any?
}

val TYPE_BRIDGES = arrayOf(
    SameToSameTypeBridge(),
    NumberToBooleanTypeBridge(),
    StringToUUIDTypeBridge(),
    StringToEnumTypeBridge(),
    TimestampToLocalDateTimeTypeBridge()
)

class SameToSameTypeBridge : TypeBridge {
    override fun matches(sourcePropertyClass: Class<*>, recordFieldClass: Class<*>): Boolean {
        return recordFieldClass.isAssignableFrom(sourcePropertyClass)
    }

    override fun convertToRecord(source: Any, sourceProperty: KProperty<*>, record: Record, recordField: Field<*>) {
        record.set(recordField as Field<Any?>, sourceProperty.getter.call(source))
    }

    override fun convertToEntity(parameter: KParameter, property: PropertyDescriptor, value: Any?): Any? {
        return value
    }
}

class NumberToBooleanTypeBridge : TypeBridge {
    override fun matches(sourcePropertyClass: Class<*>, recordFieldClass: Class<*>): Boolean {
        return Number::class.java.isAssignableFrom(recordFieldClass) &&
            sourcePropertyClass == java.lang.Boolean::class.java
    }

    override fun convertToRecord(source: Any, sourceProperty: KProperty<*>, record: Record, recordField: Field<*>) {
        val number = if (sourceProperty.getter.call(source) as Boolean) 1 else 0
        record.set(recordField as Field<Number>, number)
    }

    override fun convertToEntity(parameter: KParameter, property: PropertyDescriptor, value: Any?): Any? {
        return value?.let { it as Int }?.let { it == 1 }
    }
}

class StringToUUIDTypeBridge : TypeBridge {
    override fun matches(sourcePropertyClass: Class<*>, recordFieldClass: Class<*>): Boolean {
        return recordFieldClass == String::class.java && sourcePropertyClass == UUID::class.java
    }

    override fun convertToRecord(source: Any, sourceProperty: KProperty<*>, record: Record, recordField: Field<*>) {
        record.set(recordField as Field<String?>, sourceProperty.getter.call(source)?.toString())
    }

    override fun convertToEntity(parameter: KParameter, property: PropertyDescriptor, value: Any?): Any? {
        return value?.let { UUID.fromString(it as String) }
    }
}

class StringToEnumTypeBridge : TypeBridge {
    override fun matches(sourcePropertyClass: Class<*>, recordFieldClass: Class<*>): Boolean {
        return recordFieldClass == String::class.java && Enum::class.java.isAssignableFrom(sourcePropertyClass)
    }

    override fun convertToRecord(source: Any, sourceProperty: KProperty<*>, record: Record, recordField: Field<*>) {
        record.set(recordField as Field<String?>, (sourceProperty.getter.call(source) as Enum<*>?)?.name)
    }

    override fun convertToEntity(parameter: KParameter, property: PropertyDescriptor, value: Any?): Any? {
        return value?.let {
            parameter.type.jvmErasure.java.getMethod("valueOf", String::class.java).invoke(null, it)
        }
    }
}

class TimestampToLocalDateTimeTypeBridge : TypeBridge {
    override fun matches(sourcePropertyClass: Class<*>, recordFieldClass: Class<*>): Boolean {
        return recordFieldClass == Timestamp::class.java && sourcePropertyClass == LocalDateTime::class.java
    }

    override fun convertToRecord(source: Any, sourceProperty: KProperty<*>, record: Record, recordField: Field<*>) {
        val value = sourceProperty.getter.call(source) as LocalDateTime?
        if (value != null) {
            record.set(recordField as Field<Timestamp?>, Timestamp.valueOf(value))
        }
    }

    override fun convertToEntity(parameter: KParameter, property: PropertyDescriptor, value: Any?): Any? {
        return value?.let { it as Timestamp }?.toLocalDateTime()
    }
}