package dropit.application

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.TokenRequest
import dropit.application.dto.TransferRequest
import dropit.application.security.TokenService
import dropit.application.settings.AppSettings
import dropit.domain.entity.Phone
import dropit.domain.service.IncomingService
import dropit.domain.service.PhoneService
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.json.JavalinJackson
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
    val appSettings: AppSettings,
    val phoneService: PhoneService,
    val incomingService: IncomingService,
    val outgoingService: OutgoingService,
    val token: TokenService,
    val objectMapper: ObjectMapper
) {
    val logger = LoggerFactory.getLogger(this::class.java)
    val javalin: Javalin

    init {
        JavalinJackson.configure(objectMapper)
        javalin = Javalin.create()
            .disableStartupBanner()
            .requestLogger { ctx, ms ->
                val phone = ctx.attribute<Phone>("phone")
                logger.info("[${phone?.name}] ${ctx.method()} ${ctx.path()} took $ms ms")
            }
            .server {
                val server = Server()
                val connector = ServerConnector(server, getSslContextFactory())
                connector.port = appSettings.settings.serverPort
                connector.idleTimeout = Long.MAX_VALUE
                server.connectors = arrayOf(connector)
                server
            }
            .routes {
                get("/") { context ->
                    context.result("0.1")
                }
                path("token") {
                    post { context ->
                        context.json(phoneService.requestToken(context.bodyAsClass(TokenRequest::class.java)))
                    }

                    get { context ->
                        context.attribute("phone", token.getPendingPhone(context))
                        val phone = token.getPendingPhone(context)
                        context.json(phoneService.getTokenStatus(phone.token.toString()))
                    }
                }
                post("transfers") { context ->
                    context.attribute("phone", token.getApprovedPhone(context))
                    context.json(incomingService.createTransfer(
                        context.attribute<Phone>("phone")!!,
                        context.bodyAsClass(TransferRequest::class.java)))
                }
                post("files/:id") { context ->
                    context.attribute("phone", token.getApprovedPhone(context))
                    incomingService.receiveFile(
                        context.pathParam("id"),
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
                    val file = outgoingService.getFileDownload(
                        context.attribute<Phone>("phone")!!,
                        UUID.fromString(context.pathParam("id")))
                    context.header("Content-Type", Files.probeContentType(file.toPath()))
                    context.header("X-File-Name", file.name)
                    context.result(file.inputStream())
                }
            }
            .ws("ws") { wsHandler ->
                wsHandler.onConnect(outgoingService::openSession)
                wsHandler.onMessage(outgoingService::receiveDownloadStatus)
                wsHandler.onError { session, throwable ->
                    logger.warn("Error on phone session", throwable)
                    outgoingService.closeSession(session)
                }
                wsHandler.onClose { session, statusCode, reason ->
                    logger.info("Closing session: statusCode = $statusCode, reason: $reason")
                    outgoingService.closeSession(session)
                }
            }
            .start(appSettings.settings.serverPort)
    }

    private fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory()
        sslContextFactory.keyStorePath = javaClass.getResource("/ssl/dropit.jks").toExternalForm()
        sslContextFactory.setKeyStorePassword("C<9/wg${"$"}uxV2nCBMT")
        return sslContextFactory
    }
}