package dropit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

val rootLogger: Logger = LoggerFactory.getLogger("dropit")

val Any.logger: Logger by LoggerDelegate()

class LoggerDelegate {
    private val loggers = HashMap<KClass<*>, Logger>()

    operator fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        return loggers.computeIfAbsent(thisRef::class) { LoggerFactory.getLogger(it.java) }
    }
}
