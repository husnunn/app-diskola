package id.app.education.apiservice

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface IzinApiService {

    @GET("mobile/attendance/staff/leave-request/today")
    suspend fun staffLeaveRequestToday(): Map<String, Any>

    @GET("mobile/attendance/student/leave-request/today")
    suspend fun studentLeaveRequestToday(): Map<String, Any>

    @GET("mobile/attendance/staff/leave-request")
    suspend fun staffLeaveRequestList(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/attendance/student/leave-request")
    suspend fun studentLeaveRequestList(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @Multipart
    @POST("mobile/attendance/staff/leave-request")
    suspend fun submitStaffLeaveRequest(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/attendance/student/leave-request")
    suspend fun submitStudentLeaveRequest(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>
}
