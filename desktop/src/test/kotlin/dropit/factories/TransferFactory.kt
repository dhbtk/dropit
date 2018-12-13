package dropit.factories

import dropit.application.dto.FileRequest
import dropit.application.dto.TransferRequest
import java.util.*

object TransferFactory {
    fun transferRequestBinary() = TransferRequest(
        "Test transfer",
        listOf(FileRequest(
            id = UUID.randomUUID().toString(),
            fileName = "zeroes.bin",
            mimeType = "application/octet-stream",
            fileSize = 33554432
        ))
    )
}