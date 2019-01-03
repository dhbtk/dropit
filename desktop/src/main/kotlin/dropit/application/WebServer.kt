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
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.json.JavalinJackson
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.*
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
                get("/") {
                    it.result("0.1")
                }
                path("token") {
                    post {
                        it.json(phoneService.requestToken(it.bodyAsClass(TokenRequest::class.java)))
                    }

                    get {
                        it.attribute("phone", token.getPendingPhone(it))
                        val phone = token.getPendingPhone(it)
                        it.json(phoneService.getTokenStatus(phone.token.toString()))
                    }
                }
                post("transfers") {
                    it.attribute("phone", token.getApprovedPhone(it))
                    it.json(incomingService.createTransfer(
                        it.attribute<Phone>("phone")!!,
                        it.bodyAsClass(TransferRequest::class.java)))
                }
                post("files/:id") {
                    it.attribute("phone", token.getApprovedPhone(it))
                    incomingService.receiveFile(
                        it.attribute<Phone>("phone")!!,
                        it.pathParam("id"),
                        it.req
                    )
                    it.status(201)
                }
                post("clipboard") {
                    it.attribute("phone", token.getApprovedPhone(it))
                    incomingService.receiveClipboard(it.bodyAsClass(String::class.java))
                    it.status(201)
                }
                get("downloads/:id") {
                    it.attribute("phone", token.getApprovedPhone(it))
                    val file = outgoingService.getFileDownload(
                        it.attribute<Phone>("phone")!!,
                        UUID.fromString(it.pathParam("id")))
                    it.header("Content-Type", Files.probeContentType(file.toPath()))
                    it.header("X-File-Name", file.name)
                    it.result(file.inputStream())
                }
            }
            .ws("ws") {
                it.onConnect(outgoingService::openSession)
                it.onMessage(outgoingService::receiveDownloadStatus)
                it.onError { session, throwable ->
                    logger.warn("Error on phone session", throwable)
                    outgoingService.closeSession(session)
                }
                it.onClose { session, statusCode, reason ->
                    logger.info("Closing session: statusCode = $statusCode, reason: $reason")
                    outgoingService.closeSession(session)
                }
            }
            .start(appSettings.settings.serverPort)
    }

    private fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory()
        sslContextFactory.keyStorePath = this::class.java.getResource("/ssl/dropit.jks").toExternalForm()
        sslContextFactory.setKeyStorePassword("""C<9/wg${"$"}uxV2nCBMT""")
        return sslContextFactory
    }
}