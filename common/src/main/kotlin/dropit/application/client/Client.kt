package dropit.application.client

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.FileRequest
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenResponse
import dropit.application.dto.TransferRequest
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.InputStream
import java.util.*

class Client(
    val okHttpClient: OkHttpClient,
    objectMapper: ObjectMapper,
    val host: String,
    val phoneData: TokenRequest,
    var token: String?
) {
    private var dropItServer = Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .baseUrl(host)
        .client(okHttpClient)
        .build().create(DropItServer::class.java)

    fun requestToken(): Single<String> {
        return Single.fromCallable {
            dropItServer.requestToken(phoneData)
                .execute().body()!!
        }.doAfterSuccess { token = it }
    }

    fun version(): Single<String> {
        return Single.create { emitter ->
            dropItServer.version().enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
                    emitter.onSuccess(response.body()!!)
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    emitter.tryOnError(t)
                }
            })
        }
    }

    fun getTokenStatus(): Single<TokenResponse> {
        return headerObservable()
            .map { header ->
                dropItServer.getTokenStatus(header)
                    .execute().body()
            }
    }

    fun createTransfer(request: TransferRequest): Observable<String> {
        return headerObservable().toObservable()
            .map { header ->
                dropItServer.createTransfer(header, request)
                    .execute().body()
            }
    }

    fun uploadFile(
        fileRequest: FileRequest,
        inputStream: InputStream,
        callback: (Long) -> Unit
    ): Completable {
        return headerObservable().map { header ->
            val body = InputStreamBody(inputStream, fileRequest.fileSize!!, callback)
            val sanitizedName = fileRequest.fileName!!.replace("\"", "%22")
            dropItServer.uploadFile(
                header,
                fileRequest.id!!,
                MultipartBody.Part.create(
                    Headers.Builder().addUnsafeNonAscii(
                        "Content-Disposition",
                        "form-data; name=\"file\"; filename=\"$sanitizedName\""
                    ).build(),
                    body
                )
            ).execute()
        }.ignoreElement()
    }

    fun sendToClipboard(data: String): Completable {
        return headerObservable()
            .map {
                dropItServer.sendToClipboard(it, data).execute()
            }.ignoreElement()
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
                        ProgressResponseBody(originalResponse.body!!, listener)
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

    private fun headerObservable(): Single<String> {
        val obs = if (token != null) {
            Single.just(token)
        } else {
            requestToken()
        }
        return obs.map { tokenHeader() }
    }

    abstract class DropitClientException : RuntimeException()
    class UnauthorizedException : DropitClientException()
    class ForbiddenException : DropitClientException()
    class ServerErrorException : DropitClientException()

    @Suppress("MagicNumber")
    class ErrorHandlingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())

            val exception = when (response.code) {
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
