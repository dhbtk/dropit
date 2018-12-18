package dropit

import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.factories.TransferFactory
import org.junit.jupiter.api.Assertions.assertNotNull
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
    val client = ClientFactory(webServer.objectMapper).create("https://localhost:58992", phoneData, null)

    beforeEachTest {
        TestHelper.clearDatabase(component.jooq())
    }

    describe("requesting a token and uploading a file") {
        it("works as expected") {
            val token = client.requestToken().blockingFirst()
            assertNotNull(token)

            phoneService.authorizePhone(UUID.fromString(phoneData.id))

            val status = client.getTokenStatus().blockingFirst()

            assertEquals(TokenStatus.AUTHORIZED, status.status)

            val transferRequest = TransferFactory.transferRequestBinary()

            val transferId = client.createTransfer(transferRequest).blockingFirst()

            assertNotNull(transferId)

            client.uploadFile(
                transferRequest.files[0],
                javaClass.getResourceAsStream("/zeroes.bin")
            ).blockingFirst()
        }
    }

    afterGroup {
        webServer.javalin.stop()
    }
})