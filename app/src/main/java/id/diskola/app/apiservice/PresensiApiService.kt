package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.FeatureAvailabilityResponse
import id.diskola.app.dataclass.ResponData.LeaveRequestListResponse
import id.diskola.app.dataclass.ResponData.LeaveRequestSubmitResponse
import id.diskola.app.dataclass.ResponData.LeaveRequestTodayResponse
import id.diskola.app.dataclass.ResponData.OffsiteCheckResponse
import id.diskola.app.dataclass.ResponData.OffsiteSubmitResponse
import id.diskola.app.dataclass.ResponData.PresensiCheckResponse
import id.diskola.app.dataclass.ResponData.PresensiMonthResponse
import id.diskola.app.dataclass.ResponData.PresensiYearResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Daily presensi, Dinas Luar and Izin (doc `07` §14.1). Signatures come from the legacy
 * `ApiService.kt:535-653`. `role` is `"student"` or `"staff"` — the endpoints differ only by that
 * path segment, so one method serves both. Dinas Luar exists for staff only.
 */
interface PresensiApiService {

    @GET("mobile/attendance/{role}/by-month")
    suspend fun byMonth(
        @Path("role") role: String,
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): PresensiMonthResponse

    @GET("mobile/attendance/{role}/by-year")
    suspend fun byYear(@Path("role") role: String, @Query("year") year: Int): PresensiYearResponse

    @GET("mobile/attendance/{role}/check")
    suspend fun check(@Path("role") role: String): PresensiCheckResponse

    /** Body is `{lat, lng}` as **Strings** (legacy `PresensiViewModel.kt:1182`). */
    @POST("mobile/attendance/{role}/check-in")
    suspend fun checkIn(@Path("role") role: String, @Body body: Map<String, String>)

    @POST("mobile/attendance/{role}/check-out")
    suspend fun checkOut(@Path("role") role: String, @Body body: Map<String, String>)

    // ---- Dinas luar (staff only) ----

    @GET("mobile/attendance/staff/offsite/check")
    suspend fun offsiteCheck(): OffsiteCheckResponse

    @Multipart
    @POST("mobile/attendance/staff/offsite/check-in")
    suspend fun offsiteCheckIn(
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("address") address: RequestBody,
        @Part("note") note: RequestBody,
        @Part photo: MultipartBody.Part?,
    ): OffsiteSubmitResponse

    @Multipart
    @POST("mobile/attendance/staff/offsite/check-out")
    suspend fun offsiteCheckOut(
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("address") address: RequestBody,
        @Part("note") note: RequestBody,
        @Part photo: MultipartBody.Part?,
    ): OffsiteSubmitResponse

    // ---- Izin / sakit ----

    @GET("mobile/attendance/{role}/leave-request/today")
    suspend fun leaveToday(@Path("role") role: String): LeaveRequestTodayResponse

    @GET("mobile/attendance/{role}/leave-request")
    suspend fun leaveList(
        @Path("role") role: String,
        @Query("page") page: Int = 1,
    ): LeaveRequestListResponse

    @Multipart
    @POST("mobile/attendance/{role}/leave-request")
    suspend fun submitLeave(
        @Path("role") role: String,
        @Part("status") status: RequestBody,
        @Part("note") note: RequestBody,
        @Part file: MultipartBody.Part,
    ): LeaveRequestSubmitResponse

    // ---- Feature gate ----

    @GET("mobile/app/check-feature-availability")
    suspend fun featureAvailability(@Query("name") name: String): FeatureAvailabilityResponse
}
