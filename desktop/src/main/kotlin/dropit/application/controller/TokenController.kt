package dropit.application.controller

import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.domain.service.PhoneService
import dropit.infrastructure.request.token
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController("/token")
class TokenController(private val phoneService: PhoneService) {
    @PostMapping
    fun createToken(@RequestBody request: TokenRequest): String = phoneService.requestToken(request)

    @GetMapping
    fun getToken(webExchange: ServerWebExchange): TokenStatus = phoneService.getTokenStatus(webExchange.token()!!)
}