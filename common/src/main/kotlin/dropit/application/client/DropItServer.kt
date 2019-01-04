package dropit.application.client

import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenResponse
import dropit.application.dto.TransferRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface DropItServer {
    @POST("/token")
    fun requestToken(@Body request: TokenRequest): Call<String>

    @GET("/token")
    fun getTokenStatus(@Header("Authorization") token: String): Call<TokenResponse>

    @POST("/transfers")
    fun createTransfer(@Header("Authorization") token: String, @Body request: TransferRequest): Call<String>

    @POST("/files/{id}")
    @Multipart
    fun uploadFile(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): Call<Unit>

    @POST("/clipboard")
    fun sendToClipboard(@Header("Authorization") token: String, @Body data: String): Call<Unit>
}