package dropit.application.model

import dropit.application.dto.TransferRequest
import dropit.application.dto.TransferStatus
import dropit.jooq.tables.pojos.Transfer
import dropit.jooq.tables.records.PhoneRecord
import java.util.*

fun TransferRequest.toTransfer(phone: PhoneRecord) = Transfer(
    id = UUID.randomUUID(),
    name = name,
    sendToClipboard = sendToClipboard,
    phoneId = phone.id,
    status = TransferStatus.PENDING
)
