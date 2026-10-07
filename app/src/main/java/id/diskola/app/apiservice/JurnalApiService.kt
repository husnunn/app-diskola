package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.CaptureScopeResponse
import id.diskola.app.dataclass.ResponData.JournalCreateResponse
import id.diskola.app.dataclass.ResponData.JournalPreviewResponse
import id.diskola.app.dataclass.ResponData.JournalSaveRequest
import id.diskola.app.dataclass.ResponData.JournalUpdateRequest
import id.diskola.app.dataclass.ResponData.JurnalScheduleResponse
import id.diskola.app.dataclass.ResponData.LearningQrRequest
import id.diskola.app.dataclass.ResponData.ListClassResponse
import id.diskola.app.dataclass.ResponData.ListPlotResponse
import id.diskola.app.dataclass.ResponData.ListStudentResponse
import id.diskola.app.dataclass.ResponData.ListSubjectResponse
import id.diskola.app.dataclass.ResponData.ListTeacherResponse
import id.diskola.app.dataclass.ResponData.ScheduleDetailResponse
import id.diskola.app.dataclass.ResponData.TeacherCheckInRequest
import id.diskola.app.dataclass.ResponData.TeacherDataScopeResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Jurnal KBM endpoints (doc `07` §14.2), signatures from the legacy `ApiService.kt:491-1640`. */
interface JurnalApiService {

    @GET("mobile/attendance/schedule")
    suspend fun schedule(@Query("date") date: String): JurnalScheduleResponse

    @GET("mobile/attendance/teacher-data-scope")
    suspend fun teacherDataScope(): TeacherDataScopeResponse

    @GET("mobile/attendance/journal-capture-scope")
    suspend fun captureScope(): CaptureScopeResponse

    @GET("mobile/attendance/list-plot")
    suspend fun listPlot(): ListPlotResponse

    @GET("mobile/attendance/list-class")
    suspend fun listClass(@Query("take") take: Int, @Query("skip") skip: Int, @Query("name") name: String = ""): ListClassResponse

    @GET("mobile/attendance/list-subject")
    suspend fun listSubject(@Query("take") take: Int, @Query("skip") skip: Int, @Query("name") name: String = ""): ListSubjectResponse

    @GET("mobile/attendance/list-teacher")
    suspend fun listTeacher(@Query("take") take: Int, @Query("skip") skip: Int, @Query("name") name: String = ""): ListTeacherResponse

    /** One `school_time_plot_id[]` part per selected hour. */
    @Multipart
    @POST("mobile/attendance/journal")
    suspend fun createJournal(
        @Part("school_time_plot_id[]") plotIds: List<@JvmSuppressWildcards RequestBody>,
        @Part("school_subject_id") subjectId: RequestBody,
        @Part("school_class_id") classId: RequestBody,
        @Part("learning_objective") objective: RequestBody,
        @Part capturePhoto: MultipartBody.Part?,
    ): JournalCreateResponse

    /** [teacherId] is the teacher's user UUID; [lat]/[lng] are best effort. */
    @Multipart
    @POST("mobile/attendance/student/journal-student")
    suspend fun createStudentJournal(
        @Part("school_time_plot_id[]") plotIds: List<@JvmSuppressWildcards RequestBody>,
        @Part("school_subject_id") subjectId: RequestBody,
        @Part("teacher_id") teacherId: RequestBody,
        @Part("school_class_id") classId: RequestBody,
        @Part("status") status: RequestBody,
        @Part("lat") lat: RequestBody?,
        @Part("lng") lng: RequestBody?,
        @Part capturePhoto: MultipartBody.Part?,
    ): JournalCreateResponse

    @POST("mobile/attendance/check-in")
    suspend fun teacherCheckIn(@Body body: TeacherCheckInRequest)

    @PUT("mobile/attendance/student/journal-update")
    suspend fun studentJournalUpdate(@Body body: JournalUpdateRequest)

    @POST("mobile/attendance/student/learning/qr")
    suspend fun learningQr(@Body body: LearningQrRequest)

    @GET("mobile/attendance/schedule/{id}")
    suspend fun scheduleDetail(@Path("id") attendanceId: Int): ScheduleDetailResponse

    @GET("mobile/attendance/schedule/{id}/list-student")
    suspend fun listStudent(@Path("id") attendanceId: Int): ListStudentResponse

    /** [scheduleId] is `school_subject_schedule_id`, not the attendance id. */
    @GET("mobile/attendance/journal/{scheduleId}")
    suspend fun journalPreview(@Path("scheduleId") scheduleId: Int, @Query("date") date: String? = null): JournalPreviewResponse

    @POST("mobile/attendance/journal/save")
    suspend fun journalSave(@Body body: JournalSaveRequest)
}
