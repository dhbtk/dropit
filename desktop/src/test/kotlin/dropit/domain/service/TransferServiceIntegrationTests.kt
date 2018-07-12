package dropit.domain.service

import dropit.AbstractIntegrationTest
import dropit.application.dto.FileRequest
import dropit.application.dto.TransferRequest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.reactive.function.BodyInserters.fromMultipartData
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.toMono

class TransferServiceIntegrationTests : AbstractIntegrationTest() {
    @Autowired
    lateinit var transferService: TransferService

    @Test
    @Sql("/dataset/clear.sql", "/dataset/phone.sql")
    fun `it should upload a test file and mark the transfer as complete`() {
        val phoneToken = "b145285e-7ac5-4553-a49d-8940c12ea47d"
        val fileId = "d3c55137-46ed-4bd9-8637-f205a38cab96"

        val request = TransferRequest(
                "Test transfer",
                files = listOf(FileRequest(
                        id = fileId,
                        fileName = "zeroes.bin",
                        mimeType = "application/octet-stream",
                        fileSize = 33554432
                ))
        )
        val client = WebClient.create("http://localhost:45443")

        val transferId = client.post()
                .uri("/transfers")
                .header("Authorization", "Bearer $phoneToken")
                .body(request.toMono(), TransferRequest::class.java)
                .retrieve()
                .bodyToMono<String>().block()

        client.post()
                .uri("/files/{id}", fileId)
                .header("Authorization", "Bearer $phoneToken")
                .body(fromMultipartData("file",  ClassPathResource("zeroes.bin")))
                .retrieve().bodyToMono(Void::class.java).block()
    }
}