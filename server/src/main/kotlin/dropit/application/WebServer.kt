package dropit.application

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.TokenRequest
import dropit.application.dto.TransferRequest
import dropit.application.security.TokenService
import dropit.application.settings.AppSettings
import dropit.domain.entity.Phone
import dropit.domain.service.IncomingService
import dropit.domain.service.PhoneService
import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import dropit.logger
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context
import io.javalin.plugin.json.JavalinJackson
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
        val appSettings: AppSettings,
        val phoneService: PhoneService,
        val incomingService: IncomingService,
        val phoneSessionService: PhoneSessionService,
        val token: TokenService,
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
                val phone = ctx.attribute<Phone>("phone")
                logger.info("[${phone?.name}] ${ctx.method()} ${ctx.path()} took $ms ms")
            }
            config.server {
                val server = Server()
                val connector = ServerConnector(server, getSslContextFactory())
                connector.port = appSettings.settings.serverPort
                connector.idleTimeout = Long.MAX_VALUE
                server.connectors = arrayOf(connector)
                server
            }
        }
            .routes(routes::configure)
            .routes {
                post("transfers") { context ->
                    context.attribute("phone", token.getApprovedPhone(context))
                    context.json(
                        incomingService.createTransfer(
                            context.attribute<Phone>("phone")!!,
                            context.bodyAsClass(TransferRequest::class.java)
                        )
                    )
                }
                post("files/:id") { context ->
                    context.attribute("phone", token.getApprovedPhone(context))
                    incomingService.receiveFile(
                        UUID.fromString(context.pathParam("id")),
                        context.req
                    )
                    context.status(HttpStatus.CREATED_201)
                }
                post("clipboard") { context ->
                    context.attribute("phone", token.getApprovedPhone(context))
                    incomingService.receiveClipboard(context.bodyAsClass(String::class.java))
                    context.status(HttpStatus.CREATED_201)
                }
                get("downloads/:id") { context ->
                    context.attribute("phone", token.getApprovedPhone(context))
                    val file = phoneSessionService.getFileDownload(
                        context.attribute<Phone>("phone")!!,
                        UUID.fromString(context.pathParam("id"))
                    )
                    context.header("Content-Type", Files.probeContentType(file.toPath()))
                    context.header("X-File-Name", file.name)
                    context.result(file.inputStream())
                }
            }
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
            .start(appSettings.settings.serverPort)
    }

    private fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory.Server()
        sslContextFactory.keyStorePath = javaClass.getResource("/ssl/dropit.jks").toExternalForm()
        sslContextFactory.setKeyStorePassword("C<9/wg${"$"}uxV2nCBMT")
        return sslContextFactory
    }
}
