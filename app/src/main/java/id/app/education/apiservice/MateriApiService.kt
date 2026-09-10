package id.app.education.apiservice

import id.app.education.dataclass.ResponData.ClassRoomResponse
import id.app.education.dataclass.ResponData.DetailMateriResponse
import id.app.education.dataclass.ResponData.MajorResponse
import id.app.education.dataclass.ResponData.MapelResponse
import id.app.education.dataclass.ResponData.MateriResponse
import id.app.education.dataclass.ResponData.UploadMateriResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface MateriApiService {

    // --- TEACHER ---
    @GET("mobile/app/learning/assignment/teachers/class")
    suspend fun assignmentClass(): ClassRoomResponse

    @GET("mobile/app/learning/theories/teachers/subjects")
    suspend fun teacherSubject(
        @Query("take") take: Int,
        @Query("skip") skip: Int
    ): MapelResponse

    @GET("mobile/app/learning/theories/teachers/school-majors")
    suspend fun teacherMajor(@Query("take") take: Int = 1000): MajorResponse

    @GET("mobile/app/learning/theories/teachers/theories")
    suspend fun teacherTheory(
        @Query("take") take: Int,
        @Query("skip") skip: Int,
        @Query("school_subject") school_subject: Int? = null,
        @Query("school_class") school_class: Int? = null
    ): MateriResponse

    @GET("mobile/app/learning/theories/teachers/subjects/{subjectId}/theories")
    suspend fun teacherTheoryBySubject(
        @Path("subjectId") subjectId: Int,
        @Query("take") take: Int,
        @Query("skip") skip: Int
    ): MateriResponse

    @GET("mobile/app/learning/theories/teachers/subjects/{subjectId}/theories/{theoryId}")
    suspend fun teacherTheoryDetail(
        @Path("subjectId") subjectId: Int,
        @Path("theoryId") theoryId: Int
    ): DetailMateriResponse

    @Multipart
    @POST("mobile/app/learning/theories/teachers/theories")
    suspend fun createTheory(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): UploadMateriResponse

    @Multipart
    @POST("mobile/app/learning/theories/teachers/theories/update/{id}")
    suspend fun updateTheory(
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): UploadMateriResponse

    @DELETE("mobile/app/learning/theories/teachers/theories/{id}")
    suspend fun deleteTheory(@Path("id") id: Long): Any

    // --- STUDENT ---
    @GET("mobile/student/school-subject")
    suspend fun studentSubjects(
        @Query("take") take: Int = 1000,
        @Query("skip") skip: Int = 0
    ): MapelResponse

    @GET("mobile/student/school-subject/{subjectId}/theory")
    suspend fun studentTheories(
        @Path("subjectId") subjectId: Int,
        @Query("take") take: Int = 1000,
        @Query("skip") skip: Int = 0
    ): MateriResponse

    @GET("mobile/student/school-subject/{subjectId}/theory/{theoryId}")
    suspend fun studentTheoryDetail(
        @Path("subjectId") subjectId: Int,
        @Path("theoryId") theoryId: Int
    ): DetailMateriResponse

    // --- Placeholder scaffolding below: request/response shapes not yet defined, no UI consumer yet. ---

    @GET("mobile/teacher/school-subject")
    suspend fun teacherSubjectAlt(
        @Query("take") take: Int = 1000,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/teacher/school-grade")
    suspend fun teacherSchoolGrade(): Map<String, Any>

    @GET("mobile/teacher/school-class-room")
    suspend fun teacherSchoolClassRoom(): Map<String, Any>

    @GET("mobile/app/learning/theories/students/subjects")
    suspend fun learningStudentSubjects(
        @Query("take") take: Int = 1000,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/learning/theories/students/subjects/{subjectId}/theories")
    suspend fun learningStudentTheories(
        @Path("subjectId") subjectId: Int,
        @Query("take") take: Int = 1000,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/learning/theories/students/subjects/{subjectId}/theories/{theoryId}")
    suspend fun learningStudentTheoryDetail(
        @Path("subjectId") subjectId: Int,
        @Path("theoryId") theoryId: Int
    ): Map<String, Any>
}
