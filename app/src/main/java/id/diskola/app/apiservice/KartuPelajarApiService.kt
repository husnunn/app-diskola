package id.diskola.app.apiservice

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface KartuPelajarApiService {

    @GET("mobile/app/accounts/student-card-template")
    suspend fun studentCardTemplate(): Map<String, Any>

    @Multipart
    @POST("mobile/app/accounts/user/student-card")
    suspend fun updateStudentCard(@Part photo: MultipartBody.Part): Map<String, Any>

    @GET("mobile/app/accounts/parent/pair-lists")
    suspend fun parentPairLists(): Map<String, Any>

    @POST("mobile/app/accounts/parent/accept-pair/{id}")
    suspend fun acceptParentPairing(@Path("id") id: Int): Map<String, Any>
}
