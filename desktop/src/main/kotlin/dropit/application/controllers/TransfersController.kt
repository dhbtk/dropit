package dropit.application.controllers

import dropit.application.currentPhone
import dropit.application.dto.TransferRequest
import dropit.application.model.Transfers
import io.javalin.http.Context
import javax.inject.Inject

class TransfersController @Inject constructor() : ApplicationController() {
    fun create(context: Context) {
        Transfers.create(context.currentPhone()!!, context.bodyAsClass(TransferRequest::class.java)).let {
            context.json(it.id.toString())
        }
    }
}
