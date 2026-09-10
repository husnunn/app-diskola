package id.app.education.apiservice

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface KonselingApiService {

    @GET("mobile/konseling/list-violation")
    suspend fun listViolations(): Map<String, Any>

    @GET("mobile/konseling/list-achievement")
    suspend fun listAchievements(): Map<String, Any>

    @GET("mobile/konseling/list-handling")
    suspend fun listHandlings(): Map<String, Any>

    @GET("mobile/konseling/score-student")
    suspend fun myScore(): Map<String, Any>

    @GET("mobile/konseling/score")
    suspend fun searchStudentScore(@Query("q") q: String): Map<String, Any>

    @Multipart
    @POST("mobile/konseling/create/violation")
    suspend fun createViolation(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/konseling/create/achievement")
    suspend fun createAchievement(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @POST("mobile/konseling/create/calling-student")
    suspend fun createCallingStudent(@Body request: Map<String, Any>): Map<String, Any>
}
