package dropit.application

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.model.role
import dropit.application.settings.AppSettings
import dropit.domain.service.IncomingService
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.PHONE
import dropit.logger
import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJackson
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.jooq.DSLContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
    val appSettings: AppSettings,
    val phoneService: PhoneService,
    val incomingService: IncomingService,
    val phoneSessionService: PhoneSessionService,
    val objectMapper: ObjectMapper,
    val bus: EventBus,
    val routes: Routes,
    val jooq: DSLContext
) {
    data class ServerStartFailedEvent(override val payload: Unit) : AppEvent<Unit>

    val javalin: Javalin

    init {
        JavalinJackson.configure(objectMapper)
        javalin = Javalin.create { config ->
            config.requestLogger { ctx, ms ->
                val phone = ctx.attribute<PhoneRecord>("currentPhone")
                logger.info("[${phone?.name}] ${ctx.method()} ${ctx.path()} took $ms ms")
            }
            config.server {
                val server = Server()
                val connector = ServerConnector(server, getSslContextFactory())
                connector.port = appSettings.serverPort
                connector.idleTimeout = Long.MAX_VALUE
                server.connectors = arrayOf(connector)
                server
            }
            config.accessManager { handler, ctx, permittedRoles ->
                val role = ctx.currentPhone()?.role()
                if (role != null && role in permittedRoles) {
                    handler.handle(ctx)
                } else {
                    ctx.status(401).result("Unauthorized");
                }
            }
        }
            .before { context ->
                val currentPhone: PhoneRecord? = context.currentPhoneUuid().let { id ->
                    jooq.selectFrom(PHONE).where(PHONE.TOKEN.eq(id)).fetchOptional().orElse(null)
                }
                context.attribute("currentPhone", currentPhone)
            }
            .routes(routes::configure)
            .ws("ws") { wsHandler ->
                wsHandler.onConnect { phoneSessionService.openSession(it) }
                wsHandler.onMessage { phoneSessionService.receiveDownloadStatus(it) }
                wsHandler.onError { session ->
                    logger.warn("Error on phone session with ID ${session.sessionId}", session.error())
                    phoneSessionService.closeSession(session)
                }
                wsHandler.onClose { session ->
                    logger.info("Closing session: id = ${session.sessionId} statusCode = ${session.status()}, reason: ${session.reason()}")
                    phoneSessionService.closeSession(session)
                }
            }
            .events { event ->
                event.serverStartFailed { bus.broadcast(ServerStartFailedEvent(Unit)) }
            }
            .start(appSettings.serverPort)
    }

    private fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory.Server()
        sslContextFactory.keyStorePath = javaClass.getResource("/ssl/dropit.jks").toExternalForm()
        sslContextFactory.setKeyStorePassword("C<9/wg${"$"}uxV2nCBMT")
        return sslContextFactory
    }
}
