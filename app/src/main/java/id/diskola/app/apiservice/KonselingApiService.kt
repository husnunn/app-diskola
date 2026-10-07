package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.ListAchievementResponse
import id.diskola.app.dataclass.ResponData.ListHandlingResponse
import id.diskola.app.dataclass.ResponData.ListViolationResponse
import id.diskola.app.dataclass.ResponData.SearchPoinStudentResponse
import id.diskola.app.dataclass.ResponData.StudentPoinListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

/** Param names taken from the legacy `ApiService.kt:1518-1568`, not guessed (the earlier scaffold
 * sent `q` for search, which the backend never accepted). */
interface KonselingApiService {

    @GET("mobile/konseling/list-violation")
    suspend fun listViolations(
        @Query("take") take: Int,
        @Query("skip") skip: Int,
        @Query("name") name: String = "",
    ): ListViolationResponse

    @GET("mobile/konseling/list-achievement")
    suspend fun listAchievements(
        @Query("take") take: Int,
        @Query("skip") skip: Int,
        @Query("name") name: String = "",
    ): ListAchievementResponse

    @GET("mobile/konseling/list-handling")
    suspend fun listHandlings(
        @Query("take") take: Int,
        @Query("skip") skip: Int,
        @Query("name") name: String = "",
    ): ListHandlingResponse

    @GET("mobile/konseling/score-student")
    suspend fun myScore(): StudentPoinListResponse

    @GET("mobile/konseling/score")
    suspend fun searchStudentScore(
        @Query("nisn") nisn: String? = null,
        @Query("name") name: String? = null,
    ): SearchPoinStudentResponse

    @Multipart
    @POST("mobile/konseling/create/violation")
    suspend fun createViolation(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?,
    )

    @Multipart
    @POST("mobile/konseling/create/achievement")
    suspend fun createAchievement(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?,
    )

    /** Body keys: `score_student_id` (the id of the *search result*, not check-account's `data.id`),
     * `school_handling_id`, `calling_at` (`yyyy-MM-dd HH:mm:ss`), `calling_message`. */
    @POST("mobile/konseling/create/calling-student")
    suspend fun createCallingStudent(@Body request: Map<String, @JvmSuppressWildcards Any>)
}
