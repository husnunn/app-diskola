package id.diskola.app.apiservice

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface TryOutApiService {

    @GET("mobile/app/learning/try-out/schedules")
    suspend fun tryOutSchedules(): Map<String, Any>

    @GET("mobile/app/learning/try-out/schedules/{id}")
    suspend fun tryOutScheduleDetail(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/try-out/scored")
    suspend fun tryOutScored(): Map<String, Any>

    @GET("mobile/app/learning/try-out/scored/{id}")
    suspend fun tryOutScoreReview(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/try-out/student/training/{id}/download")
    suspend fun downloadTryOut(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/app/learning/try-out/student/training/{akm_id}/answer/{student_id}")
    suspend fun submitTryOutAnswer(
        @Path("akm_id") akmId: Int,
        @Path("student_id") studentId: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @GET("mobile/app/learning/try-out/scored/{id}/explanation")
    suspend fun tryOutExplanation(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/try-out/passing-grade")
    suspend fun passingGradeList(): Map<String, Any>
}
