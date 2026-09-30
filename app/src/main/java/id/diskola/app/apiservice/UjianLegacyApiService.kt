package id.diskola.app.apiservice

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. Legacy exam module, separate from Asesmen/AKM. */
interface UjianLegacyApiService {

    @GET("mobile/app/learning/examinations/students/exams-list")
    suspend fun examList(): Map<String, Any>

    @GET("mobile/app/learning/examinations/students/exams-scored")
    suspend fun examScoredList(): Map<String, Any>

    @PUT("mobile/app/learning/examinations/students/exams/{id}/download")
    suspend fun downloadExam(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/examinations/students/exams/{id}/detail")
    suspend fun examDetail(@Path("id") id: Int): Map<String, Any>

    @PUT("mobile/app/learning/examinations/students/exams/{id}/start")
    suspend fun startExam(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/app/learning/examinations/students/exams/{id}/answer")
    suspend fun submitExamAnswer(
        @Path("id") id: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @PUT("mobile/app/learning/examinations/students/exams/{id}/stop")
    suspend fun stopExam(@Path("id") id: Int): Map<String, Any>
}
