package id.diskola.app.apiservice

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface AgendaApiService {

    @GET("mobile/attendance/staff/agendas")
    suspend fun getAgendas(): Map<String, Any>

    @GET("mobile/attendance/staff/agendas/today")
    suspend fun getAgendasToday(): Map<String, Any>

    @POST("mobile/attendance/staff/agendas/check")
    suspend fun checkInAgenda(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/attendance/staff/agendas/check-out")
    suspend fun checkOutAgenda(@Body request: Map<String, Any>): Map<String, Any>
}
