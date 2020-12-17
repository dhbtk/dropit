package dropit.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Layout
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.spi.ContextAwareBase
import dropit.APP_NAME
import dropit.infrastructure.fs.ConfigFolderProvider
import java.io.FileOutputStream
import java.io.OutputStream

@Suppress("ComplexMethod")
class LogbackConfigurator : ContextAwareBase(), Configurator {
    private val consolePattern =
        "%d{yyyy-MM-dd HH:mm:ss} [%20.20thread] %highlight(%-5level) %cyan(%-30.30logger{29}) - %msg%n"
    private val useConsole = System.getProperty("dropit.debug") == "true"
    private val logLevels = mapOf(
        Logger.ROOT_LOGGER_NAME to Level.INFO,
        "org.jooq.Constants" to Level.WARN,
        "dropit" to Level.DEBUG
    )

    override fun configure(loggerContext: LoggerContext) {
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).apply {
            addAppender(loggerAppender(loggerContext, false))
            if (useConsole) addAppender(loggerAppender(loggerContext, true))
        }

        for ((name, level) in logLevels) {
            loggerContext.getLogger(name).level = level
        }
    }

    private fun loggerAppender(
        loggerContext: LoggerContext,
        console: Boolean
    ): OutputStreamAppender<ILoggingEvent> {
        return OutputStreamAppender<ILoggingEvent>()
            .apply {
                context = loggerContext
                name = "logfile"
                encoder = LayoutWrappingEncoder<ILoggingEvent>().apply {
                    context = loggerContext
                    layout = loggerLayout(loggerContext, console)
                    isImmediateFlush = true
                }
                outputStream = loggerOutputStream(console)
                start()
            }
    }

    private fun loggerOutputStream(console: Boolean): OutputStream {
        return if (console) {
            System.out
        } else {
            ConfigFolderProvider().configFolder
                .resolve("$APP_NAME.log").toFile().let { FileOutputStream(it, true) }
        }
    }

    private fun loggerLayout(
        loggerContext: LoggerContext,
        console: Boolean
    ): Layout<ILoggingEvent> {
        return if (console) {
            PatternLayout().apply {
                pattern = consolePattern
                context = loggerContext
                start()
            }
        } else {
            TTLLLayout().apply {
                context = loggerContext
                start()
            }
        }
    }
}


