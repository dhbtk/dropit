package dropit.application.controller

import dropit.application.dto.TransferRequest
import dropit.domain.service.TransferService
import dropit.infrastructure.request.token
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class TransferController(val transferService: TransferService) {
    @PostMapping("/transfers")
    fun createTransfer(webExchange: ServerWebExchange, @RequestBody transferRequest: TransferRequest): String {
        return transferService.createTransfer(webExchange.token()!!, transferRequest)
    }

    @PostMapping("/files/{fileId}")
    fun uploadFile(@PathVariable fileId: String, @RequestPart("file") body: Mono<FilePart>, webExchange: ServerWebExchange): Mono<Void> {
        return transferService.uploadFile(webExchange.token()!!, fileId, body)
    }
}