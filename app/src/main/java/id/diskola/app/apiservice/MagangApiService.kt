package id.diskola.app.apiservice

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface MagangApiService {

    @GET("mobile/internship/schedule")
    suspend fun internshipSchedule(): Map<String, Any>

    @POST("mobile/internship/attend")
    suspend fun internshipAttend(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/internship/attendance")
    suspend fun internshipAttendanceReport(): Map<String, Any>

    @Multipart
    @POST("mobile/internship/leave")
    suspend fun internshipDailyReport(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/internship/leave-request")
    suspend fun internshipLeaveRequest(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @DELETE("mobile/internship/leave-request/{id}")
    suspend fun deleteInternshipLeaveRequest(@Path("id") id: Int): Map<String, Any>
}
