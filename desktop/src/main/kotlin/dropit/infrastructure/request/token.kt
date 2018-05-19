package dropit.infrastructure.request

import org.springframework.web.server.ServerWebExchange

fun ServerWebExchange.token() = request.headers["Authorization"]?.first()?.split(" ")?.get(1)