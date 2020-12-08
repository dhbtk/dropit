package dropit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

val rootLogger: Logger = LoggerFactory.getLogger("dropit")

val Any.logger: Logger by LoggerDelegate()

class LoggerDelegate {
    private lateinit var logger: Logger

    operator fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        return if (this::logger.isInitialized) {
            logger
        } else {
            logger = LoggerFactory.getLogger(thisRef.javaClass)
            logger
        }
    }
}
