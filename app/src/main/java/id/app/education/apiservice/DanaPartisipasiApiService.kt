package id.app.education.apiservice

import retrofit2.http.GET
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface DanaPartisipasiApiService {

    @GET("payment/bill/list")
    suspend fun billList(): Map<String, Any>

    @GET("payment/bill/history/id/{id}")
    suspend fun billHistoryDetail(@Path("id") id: String): Map<String, Any>

    @GET("dana-partisipasi/school/{schoolId}/student")
    suspend fun participationStudents(@Path("schoolId") schoolId: String): Map<String, Any>
}
