package id.app.education.apiservice

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet.
 * No teacher-side create/score endpoints exist for this feature in the API reference.
 */
interface AsesmenApiService {

    // --- Ujian sekolah ---
    @GET("mobile/app/learning/akm/exam-schedules")
    suspend fun examSchedules(): Map<String, Any>

    @GET("mobile/app/learning/akm/exam-schedules-scored")
    suspend fun examSchedulesScored(): Map<String, Any>

    @GET("mobile/app/learning/akm/exam-schedules/{id}")
    suspend fun examScheduleDetail(
        @Path("id") id: Int,
        @Query("gov_schedule") govSchedule: Boolean? = null
    ): Map<String, Any>

    @GET("mobile/app/learning/akm/student/exam-school/{id}/download")
    suspend fun downloadExamSchool(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/app/learning/akm/student/exam-school/{id}/check-password")
    suspend fun checkExamPassword(
        @Path("id") id: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @POST("mobile/app/learning/akm/student/exam-school/{akm_id}/answer/{student_id}")
    suspend fun submitExamSchoolAnswer(
        @Path("akm_id") akmId: Int,
        @Path("student_id") studentId: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @GET("mobile/app/learning/akm/exam-schedules-scored/{id}/explains")
    suspend fun examExplanations(@Path("id") id: Int): Map<String, Any>

    // --- AKM pemerintah ---
    @GET("mobile/app/learning/akm/schedules")
    suspend fun akmSchedules(): Map<String, Any>

    @GET("mobile/app/learning/akm/scored")
    suspend fun akmScored(): Map<String, Any>

    @GET("mobile/app/learning/akm/schedules/{id}")
    suspend fun akmScheduleDetail(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/akm/student/exam/{id}/download")
    suspend fun downloadAkmExam(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/app/learning/akm/student/exam/{akm_id}/answer/{student_id}")
    suspend fun submitAkmAnswer(
        @Path("akm_id") akmId: Int,
        @Path("student_id") studentId: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @GET("mobile/app/learning/akm/student/exam/{id}/review")
    suspend fun reviewAkmExam(@Path("id") id: Int): Map<String, Any>

    // --- Setelan ---
    @GET("mobile/setting-akm")
    suspend fun getAkmSettings(): Map<String, Any>
}
