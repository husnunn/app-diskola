package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.AssignmentAnswerDetailResponse
import id.diskola.app.dataclass.ResponData.AssignmentDayResp
import id.diskola.app.dataclass.ResponData.AssignmentResponse
import id.diskola.app.dataclass.ResponData.AssignmentScheduleResp
import id.diskola.app.dataclass.ResponData.CollectHomeworkResponse
import id.diskola.app.dataclass.ResponData.HomeworkCollectedResponse
import id.diskola.app.dataclass.ResponData.HomeworkCreateResponse
import id.diskola.app.dataclass.ResponData.HomeworkResponse
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
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * `mobile/app/learning/assignment/teachers/class` ("Daftar kelas") is intentionally not duplicated
 * here — it's already [MateriApiService.assignmentClass].
 */
interface TugasApiService {

    // --- Siswa ---
    @GET("mobile/app/learning/assignment/students/backlog")
    suspend fun studentAssignmentBacklog(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): HomeworkResponse

    @GET("mobile/app/learning/assignment/students/done")
    suspend fun studentAssignmentDone(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): HomeworkResponse

    @GET("mobile/app/learning/assignment/students/scored")
    suspend fun studentAssignmentScored(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): HomeworkResponse

    /** Doc §5.5/§6.4: jawaban boleh lebih dari satu file (`file[]`) — bukan satu file seperti
     * placeholder sebelumnya. */
    @Multipart
    @POST("mobile/app/learning/assignment/students/{id}/collect")
    suspend fun collectAssignment(
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): CollectHomeworkResponse

    @GET("mobile/app/learning/assignment/students/{subjectAssignmentId}/collect/{studentAssignmentId}")
    suspend fun getAssignmentAnswerDetail(
        @Path("subjectAssignmentId") subjectAssignmentId: Int,
        @Path("studentAssignmentId") studentAssignmentId: Int
    ): AssignmentAnswerDetailResponse

    // --- Guru ---
    @GET("mobile/app/learning/assignment/teachers/backlog")
    suspend fun teacherAssignmentBacklog(@QueryMap filter: Map<String, String> = emptyMap()): HomeworkResponse

    @GET("mobile/app/learning/assignment/teachers/schedule-day")
    suspend fun teacherAssignmentScheduleDay(): AssignmentDayResp

    @GET("mobile/app/learning/assignment/teachers/schedule")
    suspend fun teacherAssignmentSchedule(
        @Query("school_class_id") classId: Int,
        @Query("day") day: String
    ): AssignmentScheduleResp

    @Multipart
    @POST("mobile/app/learning/assignment/teachers/create")
    suspend fun createAssignment(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): HomeworkCreateResponse

    @Multipart
    @POST("mobile/app/learning/assignment/teachers/update/{id}")
    suspend fun updateAssignment(
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): HomeworkCreateResponse

    @DELETE("mobile/app/learning/assignment/teachers/delete/{id}")
    suspend fun deleteAssignment(@Path("id") id: Int): Any

    @GET("mobile/app/learning/assignment/teachers/scored")
    suspend fun teacherAssignmentScoredGroups(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): HomeworkCollectedResponse

    @GET("mobile/app/learning/assignment/teachers/scored/{id}")
    suspend fun teacherAssignmentScoredDetail(@Path("id") id: Int): AssignmentResponse

    @POST("mobile/app/learning/assignment/teachers/scored/{colledtedId}/student-assignment/{assignmentId}")
    suspend fun saveAssignmentScore(
        @Path("colledtedId") colledtedId: Int,
        @Path("assignmentId") assignmentId: Int,
        @Body request: Map<String, Any>
    ): Any
}
