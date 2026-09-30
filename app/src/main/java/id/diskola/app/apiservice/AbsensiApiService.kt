package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.AbsensiResponse
import id.diskola.app.dataclass.ResponData.AttendRequest
import id.diskola.app.dataclass.ResponData.AttendanceActionResponse
import id.diskola.app.dataclass.ResponData.AttendanceDetailResponse
import id.diskola.app.dataclass.ResponData.AttendanceScheduleResponse
import id.diskola.app.dataclass.ResponData.EndAttendanceRequest
import id.diskola.app.dataclass.ResponData.LeaveRequest
import id.diskola.app.dataclass.ResponData.RekapAbsensiResponse
import id.diskola.app.dataclass.ResponData.StartAttendanceRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface AbsensiApiService {

    @GET("mobile/attendance/schedule")
    suspend fun getAttendanceSchedule(
        @Query("date") date: String? = null
    ): AttendanceScheduleResponse

    @GET("mobile/attendance/schedule/{scheduleId}")
    suspend fun getAttendanceDetail(
        @Path("scheduleId") scheduleId: Int,
        @Query("date") date: String? = null
    ): AttendanceDetailResponse

    @POST("mobile/attendance/start")
    suspend fun startAttendance(@Body request: StartAttendanceRequest): AttendanceActionResponse

    @POST("mobile/attendance/end")
    suspend fun endAttendance(@Body request: EndAttendanceRequest): AttendanceActionResponse

    @POST("mobile/attendance/attend")
    suspend fun attendClass(@Body request: AttendRequest): AttendanceActionResponse

    @POST("mobile/attendance/leave")
    suspend fun leaveClass(@Body request: LeaveRequest): AttendanceActionResponse

    @GET("mobile/attendance/data/{month}/{year}")
    suspend fun getMonthlyAttendance(
        @Path("month") month: Int,
        @Path("year") year: Int
    ): AbsensiResponse

    @GET("mobile/attendance/summary/{year}")
    suspend fun getAnnualAttendanceSummary(
        @Path("year") year: Int
    ): RekapAbsensiResponse

    // --- Placeholder scaffolding below: request/response shapes not yet defined, no UI consumer yet. ---

    // Jadwal & absensi kelas
    @GET("mobile/attendance/schedule/{id}/list-student")
    suspend fun getScheduleStudentList(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/attendance/check-in/student")
    suspend fun teacherCheckInStudent(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/attendance/student/learning/qr")
    suspend fun studentAttendQr(@Body request: Map<String, Any>): Map<String, Any>

    // Presensi siswa
    @GET("mobile/attendance/student/by-month")
    suspend fun studentAttendanceByMonth(
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Map<String, Any>

    @GET("mobile/attendance/student/by-year")
    suspend fun studentAttendanceByYear(@Query("year") year: Int): Map<String, Any>

    @GET("mobile/attendance/student/check")
    suspend fun studentAttendanceCheckToday(): Map<String, Any>

    @POST("mobile/attendance/student/check-in")
    suspend fun studentCheckIn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/attendance/student/check-out")
    suspend fun studentCheckOut(@Body request: Map<String, Any>): Map<String, Any>

    @PUT("mobile/attendance/student/journal-update")
    suspend fun updateStudentJournal(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/attendance/student/journal-student")
    suspend fun postStudentJournal(@Body request: Map<String, Any>): Map<String, Any>

    // Presensi staff/guru
    @GET("mobile/attendance/staff/by-month")
    suspend fun staffAttendanceByMonth(
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Map<String, Any>

    @GET("mobile/attendance/staff/by-year")
    suspend fun staffAttendanceByYear(@Query("year") year: Int): Map<String, Any>

    @GET("mobile/attendance/staff/check")
    suspend fun staffAttendanceCheck(): Map<String, Any>

    @GET("mobile/attendance/setting/me/today")
    suspend fun attendanceSettingToday(): Map<String, Any>

    @POST("mobile/attendance/check-in")
    suspend fun genericCheckIn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/attendance/staff/check-in")
    suspend fun staffCheckIn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/attendance/staff/check-out")
    suspend fun staffCheckOut(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/attendance/staff/offsite/check")
    suspend fun staffOffsiteCheck(): Map<String, Any>

    @Multipart
    @POST("mobile/attendance/staff/offsite/check-in")
    suspend fun staffOffsiteCheckIn(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/attendance/staff/offsite/check-out")
    suspend fun staffOffsiteCheckOut(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    // Jurnal pembelajaran
    @GET("mobile/attendance/list-class")
    suspend fun journalListClass(): Map<String, Any>

    @GET("mobile/attendance/list-subject")
    suspend fun journalListSubject(): Map<String, Any>

    @GET("mobile/attendance/list-teacher")
    suspend fun journalListTeacher(): Map<String, Any>

    @GET("mobile/attendance/list-plot")
    suspend fun journalListPlot(): Map<String, Any>

    @POST("mobile/attendance/journal")
    suspend fun postJournal(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/attendance/journal/{scheduleId}")
    suspend fun previewJournal(@Path("scheduleId") scheduleId: Int): Map<String, Any>

    @POST("mobile/attendance/journal/save")
    suspend fun saveJournal(@Body request: Map<String, Any>): Map<String, Any>
}
