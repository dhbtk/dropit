package dropit.application.controller

import dropit.domain.service.TransferService

//@RestController
class TransferController(val transferService: TransferService) {
//    @PostMapping("/transfers")
//    fun createTransfer(webExchange: ServerWebExchange, @RequestBody transferRequest: TransferRequest): String {
//        return transferService.createTransfer(webExchange.token()!!, transferRequest)
//    }
//
//    @PostMapping("/files/{fileId}")
//    fun uploadFile(@PathVariable fileId: String, @RequestPart("file") body: Mono<FilePart>, webExchange: ServerWebExchange): Mono<Void> {
//        return transferService.uploadFile(webExchange.token()!!, fileId, body)
//    }
}