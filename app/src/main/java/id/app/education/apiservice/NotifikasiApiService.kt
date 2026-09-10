package id.app.education.apiservice

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface NotifikasiApiService {

    @GET("mobile/notification")
    suspend fun getNotifications(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/notification/{id}")
    suspend fun getNotificationDetail(@Path("id") id: Int): Map<String, Any>

    @POST("mobile/notification/{id}")
    suspend fun markNotificationRead(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/notification/summary")
    suspend fun getNotificationSummary(): Map<String, Any>
}
