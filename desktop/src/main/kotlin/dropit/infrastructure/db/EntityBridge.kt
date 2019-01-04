package dropit.infrastructure.db

import org.apache.commons.lang3.ClassUtils
import org.jooq.Record
import java.beans.Introspector
import java.util.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

object EntityBridge {
    fun copyToRecord(source: Any, record: Record) {
        val properties = source::class.members.filter { it is KProperty }.map { it as KProperty }
        record.fields().forEach { field ->
            val property = properties.find { it.name == field.name.camelCase() }
            if (property != null) {
                val fieldClass = field.type
                val propClass = boxIfNeeded(property.returnType.jvmErasure.java)
                TYPE_BRIDGES.find { it.matches(propClass, fieldClass) }
                    ?.convertToRecord(source, property, record, field)
            }
        }
    }

    fun recordToEntity(record: Record, entityClass: KClass<*>): Any {
        val parameters = entityClass.primaryConstructor!!.parameters
        val parametersMap = HashMap<KParameter, Any>()
        val sourceProperties = Introspector.getBeanInfo(record.javaClass).propertyDescriptors
        sourceProperties.forEach { property ->
            val parameter = parameters.find { it.name == property.name }
            if (parameter != null) {
                TYPE_BRIDGES.find { it.matches(boxIfNeeded(parameter.type.jvmErasure.java), property.propertyType) }
                    ?.convertToEntity(parameter, property, property.readMethod.invoke(record))
                    ?.let { parametersMap[parameter] = it }
            }
        }
        return entityClass.primaryConstructor!!.callBy(parametersMap)
    }

    private fun boxIfNeeded(sourceClass: Class<*>): Class<*> {
        return ClassUtils.primitiveToWrapper(sourceClass)
    }
}

private fun String.camelCase(): String {
    return this.split("_").mapIndexed { i, s ->
        if (i == 0) {
            s.toLowerCase()
        } else {
            s.toLowerCase().capitalize()
        }
    }.joinToString("")
}