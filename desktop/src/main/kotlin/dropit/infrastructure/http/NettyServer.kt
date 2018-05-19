package dropit.infrastructure.http

import org.springframework.context.ApplicationContext
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.stereotype.Component
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.ipc.netty.NettyContext
import reactor.ipc.netty.http.server.HttpServer

@Component
class NettyServer(applicationContext: ApplicationContext) {
    final val nettyContext: NettyContext
    init {
        val httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build()
        nettyContext = HttpServer.create("0.0.0.0", 45443).newHandler(ReactorHttpHandlerAdapter(httpHandler))
                .block()!!
    }
}