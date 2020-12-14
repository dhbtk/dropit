package dropit.application

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.model.role
import dropit.application.settings.AppSettings
import dropit.infrastructure.NeedsStart
import dropit.infrastructure.NeedsStop
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.PHONE
import dropit.logger
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.security.Role
import io.javalin.core.validation.JavalinValidation
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.json.JavalinJackson
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
    private val appSettings: AppSettings,
    private val phoneSessions: PhoneSessions,
    objectMapper: ObjectMapper,
    private val routes: Routes,
    private val jooq: DSLContext
) : NeedsStart, NeedsStop {
    private val server: Javalin

    init {
        JavalinValidation.register(UUID::class.java) { UUID.fromString(it) }
        JavalinJackson.configure(objectMapper)
        server = Javalin
            .create(::configure)
            .routes(routes::configure)
            .ws("ws", phoneSessions::configureEndpoint)
    }

    override fun start() {
        server.events { event ->
            event.serverStartFailed {
                throw IllegalStateException("Failed to start the server component. Is the application running already?")
            }
        }
        server.start(appSettings.serverPort)
    }

    override fun stop() {
        server.stop()
    }

    private fun configure(config: JavalinConfig) {
        config.showJavalinBanner = false
        config.requestLogger(::requestLogger)
        config.server(::server)
        config.accessManager(::accessManager)
    }

    private fun accessManager(handler: Handler, context: Context, permittedRoles: Set<Role>) {
        val currentPhone: PhoneRecord? = context.currentPhoneUuid().let { id ->
            jooq.selectFrom(PHONE).where(PHONE.TOKEN.eq(id)).fetchOptional().orElse(null)
        }
        val role = currentPhone?.role()
        if (permittedRoles.isEmpty() || (role != null && role in permittedRoles)) {
            handler.handle(context)
        } else {
            context.status(401).result("Unauthorized")
        }
    }

    private fun requestLogger(ctx: Context, ms: Float) {
        logger.info("[${ctx.currentPhone()?.name}] ${ctx.method()} ${ctx.path()} took $ms ms")
    }

    private fun server() = Server().apply {
        connectors = arrayOf(serverConnector(this))
    }

    private fun serverConnector(server: Server) = ServerConnector(server, sslContextFactory()).apply {
        port = appSettings.serverPort
        idleTimeout = Long.MAX_VALUE
    }

    private fun sslContextFactory(): SslContextFactory {
        return SslContextFactory.Server().apply {
            keyStorePath = javaClass.getResource("/ssl/dropit.jks").toExternalForm()
            setKeyStorePassword("C<9/wg${"$"}uxV2nCBMT")
        }
    }
}
