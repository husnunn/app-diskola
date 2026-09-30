package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.AkmExplanationResponse
import id.diskola.app.dataclass.ResponData.AkmScheduleDetailResponse
import id.diskola.app.dataclass.ResponData.AkmScheduleResponse
import id.diskola.app.dataclass.ResponData.AkmSettingResponse
import id.diskola.app.dataclass.ResponData.ExamPasswordCheckResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoint paths + query params are now confirmed against the old app's real `ApiService.kt`
 * contract, documented in `docs/repo lama/api/04-pembayaran-klaspay-ppob-akm.md:556-987` and the
 * mechanism docs under `docs/repo lama/asesmen/asesmen-teknis-*.md` — not guessed. Only the exact
 * request/response *shapes* for this project's own backend remain to be verified live; the paths
 * themselves mirror a confirmed real contract.
 *
 * Scope is school-authored assessments only (`is_school_scope == true`, hence `gov_schedule` is
 * always sent as `0`). The old contract documents a parallel national-AKM endpoint group
 * (`akm/schedules`, `akm/scored`, etc.) but that track was dropped from this app per explicit user
 * decision — see `docs/FLOW_QUESTIONS.md`.
 */
interface AsesmenApiService {

    @GET("mobile/app/learning/akm/exam-schedules")
    suspend fun examSchedules(): AkmScheduleResponse

    @GET("mobile/app/learning/akm/exam-schedules-scored")
    suspend fun examSchedulesScored(): AkmScheduleResponse

    @GET("mobile/app/learning/akm/exam-schedules/{id}")
    suspend fun examScheduleDetail(@Path("id") id: Int): AkmScheduleDetailResponse

    @GET("mobile/app/learning/akm/student/exam-school/{id}/download")
    suspend fun downloadExamSchool(
        @Path("id") id: Int,
        @Query("gov_schedule") govSchedule: Int = 0,
    ): AkmScheduleDetailResponse

    // Device-bound: the checked password is only valid from whichever device sent this header
    // (`docs/repo lama/dokumentasi-asesmen.md:202-205` — "perangkat lain" is a real server error).
    @POST("mobile/app/learning/akm/student/exam-school/{id}/check-password")
    suspend fun checkExamSchoolPassword(
        @Path("id") id: Int,
        @Header("X-Device-Fingerprint") deviceFingerprint: String,
        @Body request: Map<String, String>
    ): ExamPasswordCheckResponse

    @POST("mobile/app/learning/akm/student/exam-school/{akm_id}/answer/{student_id}")
    suspend fun submitExamSchoolAnswer(
        @Path("akm_id") akmId: Int,
        @Path("student_id") studentId: Int,
        @Header("X-Device-Fingerprint") deviceFingerprint: String,
        @Query("gov_schedule") govSchedule: Int = 0,
        @Body request: List<Map<String, Any?>>
    ): Map<String, Any>

    @GET("mobile/app/learning/akm/exam-schedules-scored/{id}/explains")
    suspend fun examExplanations(
        @Path("id") id: Int,
        @Query("gov_schedule") govSchedule: Int = 0,
    ): AkmExplanationResponse

    // --- Setelan — confirmed real endpoint ---
    @GET("mobile/setting-akm")
    suspend fun getAkmSettings(): AkmSettingResponse
}
