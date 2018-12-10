package dropit.infrastructure.db

import org.apache.commons.lang3.ClassUtils
import org.jooq.*
import org.jooq.RecordMapperProvider
import java.beans.Introspector
import java.lang.reflect.Type
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

@Suppress("unchecked_cast")
class RecordMapperProvider : RecordMapperProvider {
    override fun <R : Record?, E : Any?> provide(recordType: RecordType<R>?, type: Class<out E>?): org.jooq.RecordMapper<R, E> {
        return RecordMapper((type as Class<Any>).kotlin as KClass<*>) as org.jooq.RecordMapper<R, E>
    }

    class RecordMapper(val type: KClass<*>) : org.jooq.RecordMapper<Record, Any> {
        override fun map(record: Record?): Any = recordToData(record!!, type)

        private fun <T : Any> recordToData(source: Record, dest: KClass<T>): T {
            val parameters = dest.primaryConstructor!!.parameters
            val parametersMap = HashMap<KParameter, Any>()
            val sourceProperties = Introspector.getBeanInfo(source.javaClass).propertyDescriptors
            // same type properties
            sourceProperties.forEach { property ->
                val matchingParameter = parameters.find { parameter -> isEquivalentType(parameter.type.javaType, property.propertyType) && parameter.name == property.name }
                if (matchingParameter != null) {
                    val value = property.readMethod.invoke(source)
                    if (value != null) {
                        parametersMap[matchingParameter] = value
                    }
                }
            }

            sourceProperties.filter { it.propertyType == String::class.java }
                    .forEach { property ->
                        // string to enums
                        val enumParameter = parameters.find { it.name == property.name && Enum::class.java.isAssignableFrom(it.type.jvmErasure.java) }
                        if (enumParameter != null) {
                            val sourceValue = property.readMethod.invoke(source)
                            if (sourceValue != null) {
                                parametersMap[enumParameter] = enumParameter.type.jvmErasure.java
                                        .getMethod("valueOf", String::class.java)
                                        .invoke(null, sourceValue)
                            }
                        } else {
                            val uuidParameter = parameters.find { it.name == property.name && UUID::class.java.isAssignableFrom(it.type.jvmErasure.java) }
                            if (uuidParameter != null) {
                                val sourceValue = property.readMethod.invoke(source)
                                if (sourceValue != null && sourceValue is String) {
                                    parametersMap[uuidParameter] = UUID.fromString(sourceValue)
                                }
                            }
                        }
                    }
            sourceProperties.filter { it.propertyType == Timestamp::class.java }
                    .forEach { property ->
                        val dateTimeParameter = parameters.find { it.name == property.name && it.type.jvmErasure.java == LocalDateTime::class.java }
                        if (dateTimeParameter != null) {
                            val sourceValue = property.readMethod.invoke(source) as Timestamp?
                            if (sourceValue != null) {
                                val localDateTime = sourceValue.toLocalDateTime()
                                parametersMap[dateTimeParameter] = localDateTime
                            }
                        }
                    }
            return dest.primaryConstructor!!.callBy(parametersMap)
        }

        private fun isEquivalentType(jvmType: Type, propType: Class<*>?): Boolean {
            if(propType == null) {
                return false
            }

            if(jvmType == propType) {
                return true
            }

            if(jvmType is Class<*> && ClassUtils.primitiveToWrapper(jvmType) == propType) {
                return true
            }

            return false
        }
    }
}