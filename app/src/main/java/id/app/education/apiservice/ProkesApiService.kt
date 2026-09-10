package id.app.education.apiservice

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface ProkesApiService {

    // --- Siswa ---
    @GET("mobile/app/learning/health-protocols/student-form-early-detection")
    suspend fun studentEarlyDetectionForm(): Map<String, Any>

    @POST("mobile/app/learning/health-protocols/save-report")
    suspend fun saveHealthReport(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/check-report")
    suspend fun checkHealthReport(): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/check-vaccinated")
    suspend fun checkVaccinated(): Map<String, Any>

    @POST("mobile/app/learning/health-protocols/save-vaccinated")
    suspend fun saveVaccinated(@Body request: Map<String, Any>): Map<String, Any>

    // --- Guru ---
    @GET("mobile/app/learning/health-protocols/check-vaccinated-teacher")
    suspend fun checkVaccinatedTeacher(): Map<String, Any>

    @POST("mobile/app/learning/health-protocols/save-vaccinated-teacher")
    suspend fun saveVaccinatedTeacher(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/list-school-class-teacher")
    suspend fun listSchoolClassForTeacher(): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/list-school-class-student/{idClass}")
    suspend fun listStudentsInClass(@Path("idClass") idClass: Int): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/teacher-form-early-detection")
    suspend fun teacherEarlyDetectionForm(): Map<String, Any>

    @POST("mobile/app/learning/health-protocols/save-history-report/{studentId}")
    suspend fun saveStudentScreening(
        @Path("studentId") studentId: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/check-screening-student")
    suspend fun checkStudentScreeningStatus(): Map<String, Any>

    @GET("mobile/app/learning/health-protocols/check-history-report/{studentId}")
    suspend fun studentScreeningHistory(@Path("studentId") studentId: Int): Map<String, Any>
}
