package dropit.application

import dropit.application.security.TokenService
import dropit.application.settings.AppSettings
import dropit.domain.service.PhoneService
import dropit.domain.service.TransferService
import dropit.jooq.tables.Phone
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
        val appSettings: AppSettings,
        val phoneService: PhoneService,
        val transferService: TransferService,
        val token: TokenService
) {
    val logger = LoggerFactory.getLogger(this::class.java)
    val javalin: Javalin
    init {
        javalin = Javalin.create()
                .requestLogger { ctx, ms ->
                    val phone = ctx.attribute<Phone>("phone")
                    logger.info("${ctx.method()} ${ctx.path()} took ${ms} ms")
                }
                .server {
                    val server = Server()
                    val connector = ServerConnector(server, getSslContextFactory())
                    connector.port = appSettings.settings.serverPort
                    server.connectors = arrayOf(connector)
                    server
                }
                .start(appSettings.settings.serverPort)
                .routes {
                    get("/") {
                        it.result("0.1")
                    }
                    path("token") {
                        post {

                        }

                        get {
                            it.attribute("phone", token.getPendingPhone(it))
                            val phone = token.getPendingPhone(it)
                            it.result(phone.status!!.name)
                        }
                    }
                }
    }

    private fun getSslContextFactory(): SslContextFactory {
        val sslContextFactory = SslContextFactory()
        sslContextFactory.keyStorePath = this::class.java.getResource("/ssl/dropit.jks").toExternalForm()
        sslContextFactory.setKeyStorePassword("""C<9/wg${"$"}uxV2nCBMT""")
        return sslContextFactory
    }
}