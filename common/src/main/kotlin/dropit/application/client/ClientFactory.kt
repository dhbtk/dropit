package dropit.application.client

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.TokenRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class ClientFactory @Inject constructor(private val objectMapper: ObjectMapper) {
    private val trustManager = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }.trustManagers
        .find { it is X509TrustManager } as X509TrustManager
    private val sslSocketFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(TrustSelfSignedTrustManager(trustManager)), SecureRandom())
    }.socketFactory
    private val okHttpLogger = HttpLoggingInterceptor()
        .apply { level = HEADERS }
    @Suppress("MagicNumber")
    private val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslSocketFactory, trustManager)
            .apply {
                try {
                    Class.forName("android.os.Build")
                    addInterceptor(okHttpLogger)
                } catch (e: ClassNotFoundException) {

                }
            }
        .addInterceptor(Client.ErrorHandlingInterceptor())
        .connectTimeout(5, TimeUnit.SECONDS)
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .build()

    fun create(host: String, phoneData: TokenRequest, token: String?): Client = Client(
        okHttpClient,
        objectMapper,
        host,
        phoneData,
        token
    )
}
