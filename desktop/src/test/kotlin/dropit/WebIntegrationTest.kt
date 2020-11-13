package dropit

import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.domain.service.IncomingService
import dropit.factories.TransferFactory
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*
import kotlin.test.assertEquals

object WebIntegrationTest : Spek({
    val component = TestHelper.createComponent()
    val webServer = component.webServer()

    val phoneService by memoized { component.phoneService() }
    val phoneData = TokenRequest(
        UUID.randomUUID().toString(),
        "Phone"
    )
    val dropItClient = ClientFactory(webServer.objectMapper).create("https://localhost:58992", phoneData, null)

    beforeEachTest {
        TestHelper.clearDatabase(component.jooq(), component.appSettings())
    }

    describe("sending a file and sending clipboard text") {
        it("works as expected") {
//            webServer.javalin.start()
            val token = dropItClient.requestToken().blockingFirst()
            assertNotNull(token)

            phoneService.authorizePhone(UUID.fromString(phoneData.id))

            val status = dropItClient.getTokenStatus().blockingFirst()

            assertEquals(TokenStatus.AUTHORIZED, status.status)

            val transferRequest = TransferFactory.transferRequestBinary()

            val transferId = dropItClient.createTransfer(transferRequest).blockingFirst()

            assertNotNull(transferId)

            dropItClient.uploadFile(
                transferRequest.files[0],
                javaClass.getResourceAsStream("/zeroes.bin")
            ) {}.blockingFirst()

            val textToSend = "abcde\nfghijk"
            var callbackCalled = false
            component.eventBus().subscribe(IncomingService.ClipboardReceiveEvent::class) { (data) ->
                callbackCalled = true
                assertEquals(textToSend, data)
            }
            dropItClient.sendToClipboard(textToSend).blockingFirst()

            assertTrue(callbackCalled)
        }
    }

    afterGroup {
        webServer.javalin.stop()
    }
})
