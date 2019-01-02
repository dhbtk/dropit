package dropit.application.client

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenResponse
import dropit.application.dto.TransferRequest
import io.reactivex.Observable
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.InputStream
import java.util.*

class Client(
    val okHttpClient: OkHttpClient,
    objectMapper: ObjectMapper,
    val host: String,
    val phoneData: TokenRequest,
    var token: String?) {
    private var dropItServer = Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .baseUrl(host)
        .client(okHttpClient)
        .build().create(DropItServer::class.java)

    fun requestToken(): Observable<String> {
        return Observable.fromCallable {
            dropItServer.requestToken(phoneData)
                .execute().body()!!
        }.doOnNext { token = it }
    }

    fun getTokenStatus(): Observable<TokenResponse> {
        return headerObservable()
            .map {
                dropItServer.getTokenStatus(it)
                    .execute().body()
            }
    }

    fun createTransfer(request: TransferRequest): Observable<String> {
        return headerObservable()
            .map {
                dropItServer.createTransfer(it, request)
                    .execute().body()
            }
    }

    fun uploadFile(fileRequest: FileRequest, inputStream: InputStream, callback: (Long) -> Unit): Observable<Unit> {
        return headerObservable()
            .map {
                val body = InputStreamBody(inputStream, fileRequest.fileSize!!, callback)
                dropItServer.uploadFile(
                    it,
                    fileRequest.id!!,
                    MultipartBody.Part.create(
                        Headers.Builder().addUnsafeNonAscii(
                            "Content-Disposition",
                            "form-data; name=\"file\"; filename=\"${fileRequest.fileName!!.replace("\"", "%22")}\"").build(),
                        body
                    )
                ).execute().body()
            }
    }

    fun sendToClipboard(data: String): Observable<Unit> {
        return headerObservable()
            .map { dropItServer.sendToClipboard(it, data).execute().body() }
    }

    fun connectWebSocket(listener: WebSocketListener): WebSocket {
        return okHttpClient.newBuilder().build().newWebSocket(
            Request.Builder()
                .url(host.replaceFirst("https://", "wss://") + "/ws")
                .header("Authorization", tokenHeader())
                .build(),
            listener
        )
    }

    fun downloadFile(fileId: UUID, listener: ProgressListener): Observable<Response> {
        return Observable.fromCallable {
            val tempClient = okHttpClient.newBuilder()
                .addNetworkInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder().body(
                        ProgressResponseBody(originalResponse.body()!!, listener)
                    ).build()
                }.build()
            val request = Request.Builder()
                .url("$host/downloads/$fileId")
                .header("Authorization", tokenHeader())
                .build()
            tempClient.newCall(request).execute()
        }
    }

    private fun tokenHeader() = "Bearer $token"

    private fun headerObservable(): Observable<String> {
        val obs = if (token != null) {
            Observable.just(token)
        } else {
            requestToken()
        }
        return obs.map { tokenHeader() }
    }

    abstract class DropitClientException : RuntimeException()
    class UnauthorizedException : DropitClientException()
    class ForbiddenException : DropitClientException()
    class ServerErrorException : DropitClientException()

    class ErrorHandlingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())

            val exception = when (response.code()) {
                401 -> UnauthorizedException()
                403 -> ForbiddenException()
                500 -> ServerErrorException()
                else -> null
            }

            exception != null && throw exception

            return response
        }

    }
}