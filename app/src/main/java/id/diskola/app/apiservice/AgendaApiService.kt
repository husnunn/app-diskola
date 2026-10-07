package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.StaffAgendaCheckBody
import id.diskola.app.dataclass.ResponData.StaffAgendaCheckResponse
import id.diskola.app.dataclass.ResponData.StaffAgendasResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AgendaApiService {

    /** `date` = `yyyy-MM-dd`. */
    @GET("mobile/attendance/staff/agendas")
    suspend fun getAgendas(@Query("date") date: String? = null): StaffAgendasResponse

    /** Hub badge only (`summary.missing`). */
    @GET("mobile/attendance/staff/agendas/today")
    suspend fun getAgendasToday(): StaffAgendasResponse

    @POST("mobile/attendance/staff/agendas/check")
    suspend fun checkInAgenda(@Body body: StaffAgendaCheckBody): StaffAgendaCheckResponse

    @POST("mobile/attendance/staff/agendas/check-out")
    suspend fun checkOutAgenda(@Body body: StaffAgendaCheckBody): StaffAgendaCheckResponse
}
