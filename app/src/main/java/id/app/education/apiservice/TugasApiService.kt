package id.app.education.apiservice

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
 * Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet.
 * `mobile/app/learning/assignment/teachers/class` ("Daftar kelas") is intentionally not
 * duplicated here — it's already [MateriApiService.assignmentClass].
 */
interface TugasApiService {

    // --- Siswa ---
    @GET("mobile/app/learning/assignment/students/backlog")
    suspend fun studentAssignmentBacklog(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/learning/assignment/students/done")
    suspend fun studentAssignmentDone(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/learning/assignment/students/scored")
    suspend fun studentAssignmentScored(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @Multipart
    @POST("mobile/app/learning/assignment/students/{id}/collect")
    suspend fun collectAssignment(
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @GET("mobile/app/learning/assignment/students/{subjectAssignmentId}/collect/{studentAssignmentId}")
    suspend fun getAssignmentAnswerDetail(
        @Path("subjectAssignmentId") subjectAssignmentId: Int,
        @Path("studentAssignmentId") studentAssignmentId: Int
    ): Map<String, Any>

    // --- Guru ---
    @GET("mobile/app/learning/assignment/teachers/backlog")
    suspend fun teacherAssignmentBacklog(@QueryMap filter: Map<String, String> = emptyMap()): Map<String, Any>

    @GET("mobile/app/learning/assignment/teachers/schedule-day")
    suspend fun teacherAssignmentScheduleDay(): Map<String, Any>

    @GET("mobile/app/learning/assignment/teachers/schedule")
    suspend fun teacherAssignmentSchedule(
        @Query("class") classId: Int,
        @Query("day") day: String
    ): Map<String, Any>

    @Multipart
    @POST("mobile/app/learning/assignment/teachers/create")
    suspend fun createAssignment(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/app/learning/assignment/teachers/update/{id}")
    suspend fun updateAssignment(
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @DELETE("mobile/app/learning/assignment/teachers/delete/{id}")
    suspend fun deleteAssignment(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/assignment/teachers/scored")
    suspend fun teacherAssignmentScoredGroups(): Map<String, Any>

    @GET("mobile/app/learning/assignment/teachers/scored/{id}")
    suspend fun teacherAssignmentScoredDetail(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/app/learning/assignment/teachers/scored/{colledtedId}/student-assignment/{assignmentId}")
    suspend fun saveAssignmentScore(
        @Path("colledtedId") colledtedId: Int,
        @Path("assignmentId") assignmentId: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>
}
