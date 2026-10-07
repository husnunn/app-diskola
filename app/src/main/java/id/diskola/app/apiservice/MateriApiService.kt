package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.ClassRoomResponse
import id.diskola.app.dataclass.ResponData.DetailMateriResponse
import id.diskola.app.dataclass.ResponData.GradeResponse
import id.diskola.app.dataclass.ResponData.MajorResponse
import id.diskola.app.dataclass.ResponData.MapelResponse
import id.diskola.app.dataclass.ResponData.MateriResponse
import id.diskola.app.dataclass.ResponData.UploadMateriResponse
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

    /** Doc `05-pembelajaran-materi-tugas.md` §3.4 last paragraph — feeds the "Ditampilkan ke →
     * Jenjang" dropdown in the upload form. Was `Map<String, Any>`/never called; the form used a
     * hardcoded `listOf("10","11","12")` instead. */
    @GET("mobile/teacher/school-grade")
    suspend fun teacherSchoolGrade(): GradeResponse

    /** Doc §3.4 last paragraph — feeds "Ditampilkan ke → Kelas". Was `Map<String, Any>`/never
     * called; the form reused the filter-list endpoint (`assignmentClass`) instead. */
    @GET("mobile/teacher/school-class-room")
    suspend fun teacherSchoolClassRoom(): ClassRoomResponse

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
    // Doc §2.5's confirmed-from-source contract (`mobile/app/learning/theories/students/...`) —
    // these used to sit unused as untyped placeholders while the screens called a different,
    // undocumented `mobile/student/school-subject...` family instead (fixed 30-09-2026, verify on
    // device that the backend actually serves this path before relying on it further).
    @GET("mobile/app/learning/theories/students/subjects")
    suspend fun studentSubjects(
        @Query("take") take: Int = 20,
        @Query("skip") skip: Int = 0
    ): MapelResponse

    @GET("mobile/app/learning/theories/students/subjects/{subjectId}/theories")
    suspend fun studentTheories(
        @Path("subjectId") subjectId: Int,
        @Query("take") take: Int = 20,
        @Query("skip") skip: Int = 0
    ): MateriResponse

    @GET("mobile/app/learning/theories/students/subjects/{subjectId}/theories/{theoryId}")
    suspend fun studentTheoryDetail(
        @Path("subjectId") subjectId: Int,
        @Path("theoryId") theoryId: Int
    ): DetailMateriResponse
}
