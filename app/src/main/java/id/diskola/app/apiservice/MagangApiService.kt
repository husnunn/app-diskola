package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.MagangAttendRequest
import id.diskola.app.dataclass.ResponData.MagangAttendanceResponse
import id.diskola.app.dataclass.ResponData.MagangScheduleResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Magang endpoints (doc `07` §14.4), signatures from the legacy `ApiService.kt:1486-1514`. The write calls return nothing the app reads
 * (legacy re-fetched the schedule and the reports afterwards), so they are `Unit` and an empty or unexpected success body cannot turn a sent
 * request into a reported failure.
 */
interface MagangApiService {

    @GET("mobile/internship/schedule")
    suspend fun schedule(): MagangScheduleResponse

    @POST("mobile/internship/attend")
    suspend fun attend(@Body body: MagangAttendRequest)

    @GET("mobile/internship/attendance")
    suspend fun attendance(): MagangAttendanceResponse

    /** Daily report plus check-out: `id` is the schedule id, `daily_report` the notes, `file` the photo. */
    @Multipart
    @POST("mobile/internship/leave")
    suspend fun dailyReport(
        @Part("id") id: RequestBody,
        @Part("daily_report") dailyReport: RequestBody,
        @Part file: MultipartBody.Part?,
    )

    /** Izin: `status` is `sakit` or `izin`. */
    @Multipart
    @POST("mobile/internship/leave-request")
    suspend fun leaveRequest(
        @Part("id") id: RequestBody,
        @Part("status") status: RequestBody,
        @Part("note") note: RequestBody,
        @Part file: MultipartBody.Part?,
    )

    /** Exists on the server but legacy hid its only button; nothing calls this. */
    @DELETE("mobile/internship/leave-request/{id}")
    suspend fun deleteLeaveRequest(@Path("id") id: Long)
}
