package id.diskola.app.apiservice

import retrofit2.http.GET
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface PengumumanApiService {

    @GET("mobile/app/learning/announcements")
    suspend fun getAnnouncements(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>
}
