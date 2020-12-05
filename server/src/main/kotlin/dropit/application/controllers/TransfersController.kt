package dropit.application.controllers

import dropit.application.currentPhone
import dropit.application.dto.TransferRequest
import dropit.domain.service.IncomingService
import io.javalin.http.Context
import javax.inject.Inject

class TransfersController @Inject constructor(val incomingService: IncomingService) : ApplicationController() {
    fun create(context: Context) {
        incomingService.createTransfer(context.currentPhone()!!, context.bodyAsClass(TransferRequest::class.java)).let {
            context.json(it)
        }
    }
}
