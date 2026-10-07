package id.diskola.app.repository

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import id.diskola.app.apiservice.AgendaApiService
import id.diskola.app.apiservice.ApiException
import id.diskola.app.dataclass.ResponData.AgendaDay
import id.diskola.app.dataclass.ResponData.StaffAgendaCheckBody
import id.diskola.app.dataclass.ResponData.StaffAgendaDayTable
import id.diskola.app.dataclass.ResponData.StaffAgendaEvent
import id.diskola.app.dataclass.ResponData.StaffAgendaGate
import id.diskola.app.dataclass.ResponData.StaffAgendaItem
import id.diskola.app.dataclass.ResponData.StaffAgendaItemTable
import id.diskola.app.dataclass.ResponData.StaffAgendaPolicy
import id.diskola.app.dataclass.ResponData.StaffAgendaSummary
import id.diskola.app.dataclass.ResponData.StaffAgendaWindow
import id.diskola.app.dataclass.ResponData.StaffAgendasData
import id.diskola.app.dataclass.localDb.AgendaDao
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Agenda Mingguan (doc `05` §8). One Room day-row + session-rows per `yyyy-MM-dd`; the caller
 * shows [readLocal] first and decides whether to [fetchDay] (cache miss, pull-to-refresh, or
 * returning to today) — the repository never fetches behind the caller's back, which is what made
 * the legacy screen load twice on open.
 */
class AgendaRepository @Inject constructor(
    private val api: AgendaApiService,
    private val dao: AgendaDao,
    moshi: Moshi,
) {
    private val gateAdapter: JsonAdapter<StaffAgendaGate> = moshi.adapter(StaffAgendaGate::class.java)
    private val policyAdapter: JsonAdapter<StaffAgendaPolicy> = moshi.adapter(StaffAgendaPolicy::class.java)
    private val eventAdapter: JsonAdapter<StaffAgendaEvent> = moshi.adapter(StaffAgendaEvent::class.java)

    suspend fun readLocal(date: String): AgendaDay? {
        val day = dao.getDay(date) ?: return null
        val items = dao.getItems(date).map { row ->
            StaffAgendaItem(
                agenda_id = row.agenda_id,
                name = row.name,
                kind = row.kind,
                window = StaffAgendaWindow(row.start_at, row.end_at),
                require_check = row.require_check,
                sort_order = row.sort_order,
                note = row.note,
                policy = parse(row.policy_json, policyAdapter),
                event = parse(row.event_json, eventAdapter),
            )
        }
        return AgendaDay(
            date = day.date,
            agendaEnabled = day.agenda_enabled,
            gate = parse(day.gate_json, gateAdapter) ?: StaffAgendaGate(),
            agendas = items,
            summary = StaffAgendaSummary(day.required_sessions, day.checked, day.late, day.missing),
        )
    }

    /** Network → Room (atomic replace) → the rebuilt day. */
    suspend fun fetchDay(date: String): AgendaDay {
        val data = api.getAgendas(date).data ?: throw IllegalStateException("Gagal memuat agenda")
        save(date, data)
        return readLocal(date) ?: throw IllegalStateException("Gagal memuat agenda")
    }

    suspend fun getItem(date: String, agendaId: Int): Pair<AgendaDay, StaffAgendaItem>? {
        val day = readLocal(date) ?: return null
        val item = day.agendas.firstOrNull { it.agenda_id == agendaId } ?: return null
        return day to item
    }

    /** Hub badge: `summary.missing` when the workgroup has agendas enabled, else (or on any failure) 0. */
    suspend fun missingToday(): Int = try {
        val data = api.getAgendasToday().data
        if (data?.agenda_enabled == true) data.summary?.missing ?: 0 else 0
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        0
    }

    suspend fun checkIn(date: String, agendaId: Int, lat: Double, lng: Double) {
        submit(date, "Check-in gagal") { api.checkInAgenda(StaffAgendaCheckBody(agendaId, lat, lng)) }
    }

    suspend fun checkOut(date: String, agendaId: Int, lat: Double, lng: Double) {
        submit(date, "Check-out gagal") { api.checkOutAgenda(StaffAgendaCheckBody(agendaId, lat, lng)) }
    }

    private suspend fun submit(date: String, fallback: String, call: suspend () -> Unit) {
        try {
            call()
        } catch (e: ApiException) {
            // Validation texts (`errors`) win over the generic `message`, as in legacy `userFacingHttpError`.
            throw IllegalStateException(
                e.validationMessages.joinToString("\n").ifBlank { e.message?.takeIf { it.isNotBlank() } ?: fallback },
            )
        }
        // The server owns the resulting status; a failed refresh must not turn a successful report into an error.
        try {
            fetchDay(date)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // keep the stale cache; the screen refreshes on its next resume
        }
    }

    private suspend fun save(date: String, data: StaffAgendasData) {
        val summary = data.summary ?: StaffAgendaSummary()
        val day = StaffAgendaDayTable(
            date = date,
            agenda_enabled = data.agenda_enabled,
            workgroup_id = data.workgroup_id,
            day = data.day,
            agenda_source = data.agenda_source,
            gate_json = gateAdapter.toJson(data.gate ?: StaffAgendaGate()),
            required_sessions = summary.required_sessions,
            checked = summary.checked,
            late = summary.late,
            missing = summary.missing,
        )
        val items = data.agendas.orEmpty().map { item ->
            StaffAgendaItemTable(
                date = date,
                agenda_id = item.agenda_id,
                name = item.name,
                kind = item.kind,
                start_at = item.startAt,
                end_at = item.endAt,
                require_check = item.require_check,
                sort_order = item.sort_order,
                note = item.note,
                policy_json = item.policy?.let { policyAdapter.toJson(it) }.orEmpty(),
                event_json = item.event?.let { eventAdapter.toJson(it) }.orEmpty(),
            )
        }
        dao.replaceDay(day, items)
    }

    private fun <T> parse(json: String, adapter: JsonAdapter<T>): T? {
        if (json.isBlank()) return null
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
