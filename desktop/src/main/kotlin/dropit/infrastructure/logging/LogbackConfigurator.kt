package dropit.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.spi.ContextAwareBase
import dropit.APP_NAME
import dropit.infrastructure.fs.ConfigFolderProvider

class LogbackConfigurator : ContextAwareBase(), Configurator {
    override fun configure(loggerContext: LoggerContext) {
        val useConsole = System.getProperty("dropit.debug") == "true"
        val appender = if (useConsole) {
            setupConsoleLogging(loggerContext)
        } else {
            setupFileLogging(loggerContext)
        }

        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            .apply {
                addAppender(appender)
                level = Level.INFO
            }

        loggerContext.getLogger("org.jooq.Constants")
            .apply { level = Level.WARN }

        loggerContext.getLogger("dropit")
            .apply { level = Level.DEBUG }
    }

    private fun setupFileLogging(context: LoggerContext): OutputStreamAppender<ILoggingEvent> {
        return OutputStreamAppender<ILoggingEvent>()
            .apply { this.context = context }
            .apply { this.name = "logfile" }
            .apply {
                this.encoder = LayoutWrappingEncoder<ILoggingEvent>()
                    .apply { this.context = context }
                    .apply {
                        this.layout = TTLLLayout()
                            .apply { this.context = context }
                            .apply { start() }
                    }
            }
            .apply { this.outputStream = ConfigFolderProvider().configFolder.resolve("$APP_NAME.log").toFile().outputStream() }
            .apply { start() }
    }

    private fun setupConsoleLogging(context: LoggerContext): OutputStreamAppender<ILoggingEvent> {
        return ConsoleAppender<ILoggingEvent>()
            .apply { this.context = context }
            .apply { this.name = "console" }
            .apply {
                this.encoder = LayoutWrappingEncoder<ILoggingEvent>()
                    .apply { this.context = context }
                    .apply {
                        this.layout = PatternLayout()
                            .apply { this.pattern = "%d{yyyy-MM-dd HH:mm:ss} [%20.20thread] %highlight(%-5level) %cyan(%-30.30logger{29}) - %msg%n" }
                            .apply { this.context = context }
                            .apply { start() }
                    }
            }.apply { start() }
    }
}