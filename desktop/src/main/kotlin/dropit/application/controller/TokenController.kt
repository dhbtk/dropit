package dropit.application.controller

import dropit.application.dto.TokenRequest
import dropit.domain.service.PhoneService

//@RestController("/token")
class TokenController(private val phoneService: PhoneService) {
//    @PostMapping
    fun createToken(request: TokenRequest): String = phoneService.requestToken(request)

//    @GetMapping
//    fun getToken(webExchange: ServerWebExchange): TokenStatus = phoneService.getTokenStatus(webExchange.token()!!)
}