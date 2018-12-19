package dropit

import dropit.application.client.ClientFactory
import dropit.application.client.DropItServer
import dropit.application.client.InputStreamBody
import dropit.application.client.TrustSelfSignedTrustManager
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.factories.TransferFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
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
        TestHelper.clearDatabase(component.jooq())
    }

    describe("using the Client class") {
        it("works as expected") {
            webServer.javalin.start()
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
            ).blockingFirst()
        }
    }

    describe("using the DropItServer class") {
        it("works as expected") {
            val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                .apply { init(null as KeyStore?) }.trustManagers
                .find { it is X509TrustManager } as X509TrustManager
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(TrustSelfSignedTrustManager(trustManager)), SecureRandom())
            }
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.HEADERS
            val okHttpClient = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(logger)
                .build()
            val client = Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create(webServer.objectMapper))
                .baseUrl("https://localhost:58992")
                .client(okHttpClient)
                .build().create(DropItServer::class.java)

            val phoneId = UUID.randomUUID()
            val tokenRequest = TokenRequest(phoneId.toString(), "Phone")

            val token = client.requestToken(tokenRequest).execute().body()!!
            val header = "Bearer $token"

            phoneService.authorizePhone(phoneId)

            val status = client.getTokenStatus(header).execute().body()!!

            assertEquals(TokenStatus.AUTHORIZED, status.status)

            val transferRequest = TransferFactory.transferRequestBinary()

            val transferId = client.createTransfer(header, transferRequest).execute().body()

            Assertions.assertNotNull(transferId)

            val response = client.uploadFile(
                header,
                transferRequest.files[0].id!!,
                MultipartBody.Part.createFormData(
                    "file",
                    "zeroes.bin",
                    InputStreamBody(javaClass.getResourceAsStream("/zeroes.bin"), transferRequest.files[0].fileSize!!)
                ))
                .execute()

            Assertions.assertTrue(response.isSuccessful)
        }
    }

    afterGroup {
        webServer.javalin.stop()
    }
})