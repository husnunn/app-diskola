package id.diskola.app.repository

import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.apiservice.PresensiApiService
import id.diskola.app.dataclass.ResponData.OffsiteCheckData
import id.diskola.app.dataclass.ResponData.OffsiteSubmitResponse
import id.diskola.app.dataclass.ResponData.PresensiCheckData
import id.diskola.app.dataclass.ResponData.PresensiDayItem
import id.diskola.app.dataclass.ResponData.PresensiDayTable
import id.diskola.app.dataclass.ResponData.PresensiRekapTable
import id.diskola.app.dataclass.localDb.PresensiDao
import id.diskola.app.utils.PreferenceClass
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.session.SessionStore
import java.io.File
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** What `staff/offsite/check` said — [enabled] false covers both "not enabled" and any failure (403 included). */
data class OffsiteAvailability(
    val enabled: Boolean,
    val allowAttendance: Boolean,
    val typeAttendance: String,
    val buttonLabel: String,
    val responseText: String,
    val requirePhoto: Boolean,
    val message: String,
) {
    val isCheckIn: Boolean get() = typeAttendance.equals("checkin", ignoreCase = true)
    val isCheckOut: Boolean get() = typeAttendance.equals("checkout", ignoreCase = true)

    companion object {
        val Disabled = OffsiteAvailability(false, false, "", "", "", true, "")
    }
}

/**
 * Daily presensi (doc `07` §4–6): Data Absensi (Room-first, per month), Rekap (cache-first, per
 * year), Masuk/Pulang sekolah and Dinas Luar. Never fetches behind the caller's back — the ViewModel
 * decides when a refresh is due.
 */
class PresensiRepository @Inject constructor(
    private val api: PresensiApiService,
    private val dao: PresensiDao,
    private val sessionStore: SessionStore,
    private val preference: PreferenceClass,
    private val asesmenApi: AsesmenApiService,
) {
    val isStudent: Boolean get() = sessionStore.isStudent
    val school get() = sessionStore.school
    val user get() = sessionStore.user
    private val role: String get() = if (sessionStore.isStudent) "student" else "staff"

    // ---- Data Absensi ----

    suspend fun readMonth(month: YearMonth): List<PresensiDayTable> = dao.getMonth(month.toString())

    suspend fun fetchMonth(month: YearMonth): List<PresensiDayTable> {
        val rows = api.byMonth(role, month.year, month.monthValue).data.map { it.toTable() }
        dao.replaceMonth(month.toString(), rows)
        return dao.getMonth(month.toString())
    }

    suspend fun day(date: String): PresensiDayTable? = dao.getDay(date)

    /** Newest cached day that has a clock time, for "Presensi terakhir …" (current month, then the previous one). */
    suspend fun lastRecord(): PresensiDayTable? {
        val now = YearMonth.now()
        return listOf(now, now.minusMonths(1)).firstNotNullOfOrNull { month ->
            dao.getMonth(month.toString()).lastOrNull { it.attendAt.isNotBlank() || it.leaveAt.isNotBlank() }
        }
    }

    // ---- Rekap ----

    suspend fun readRekap(year: Int): List<PresensiRekapTable> = dao.getRekap(year)

    suspend fun fetchRekap(year: Int): List<PresensiRekapTable> {
        val rows = api.byYear(role, year).data.mapIndexed { index, item ->
            PresensiRekapTable(
                year = year,
                orderIndex = index,
                month = item.month,
                ontime = item.ontime ?: 0,
                late = item.late ?: 0,
                izin = item.izin ?: 0,
                sakit = item.sakit ?: 0,
                alpha = item.alpha ?: 0,
            )
        }
        dao.replaceRekap(year, rows)
        return rows
    }

    // ---- Masuk / Pulang sekolah ----

    suspend fun check(): PresensiCheckData? = api.check(role).data

    suspend fun checkIn(lat: Double, lng: Double) = api.checkIn(role, coords(lat, lng))

    suspend fun checkOut(lat: Double, lng: Double) = api.checkOut(role, coords(lat, lng))

    /** `absence_setting` (radius enforced). 12 h cache; a missing key, no answer, or no cache → restricted. */
    suspend fun isRadiusRestricted(): Boolean {
        val now = System.currentTimeMillis()
        val cachedAt = preference.getLong(KEY_ABSENCE_AT)
        if (cachedAt > 0 && now - cachedAt < SETTING_TTL_MS && preference.contains(KEY_ABSENCE)) {
            return preference.getBoolean(KEY_ABSENCE, true)
        }
        return try {
            val value = asesmenApi.getAkmSettings().data?.absence_setting ?: true
            preference.putBoolean(KEY_ABSENCE, value)
            preference.putLong(KEY_ABSENCE_AT, now)
            value
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (preference.contains(KEY_ABSENCE)) preference.getBoolean(KEY_ABSENCE, true) else true
        }
    }

    // ---- Dinas Luar (staff only) ----

    suspend fun offsiteAvailability(): OffsiteAvailability {
        if (sessionStore.isStudent) return OffsiteAvailability.Disabled
        return try {
            val response = api.offsiteCheck()
            val data: OffsiteCheckData? = response.data
            OffsiteAvailability(
                enabled = data?.offsite_enabled == true,
                allowAttendance = data?.allow_attendance == true,
                typeAttendance = data?.type_attendance.orEmpty(),
                buttonLabel = data?.attendance_response_text_button.orEmpty(),
                responseText = data?.attendance_response_text.orEmpty(),
                requirePhoto = data?.require_photo ?: true,
                message = response.message,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OffsiteAvailability.Disabled
        }
    }

    /** [checkIn] picks the endpoint; the server decides whether the tap is an arrival or a departure. */
    suspend fun offsiteSubmit(
        checkIn: Boolean,
        lat: Double,
        lng: Double,
        address: String,
        note: String,
        photo: File?,
    ): OffsiteSubmitResponse {
        val lat0 = text(lat.toString())
        val lng0 = text(lng.toString())
        val addr = text(address.trim().take(PresensiRules.NOTE_MAX))
        val noteBody = text(note.trim())
        val part = photo?.let { MultipartBody.Part.createFormData("photo", it.name, it.asRequestBody("image/jpeg".toMediaTypeOrNull())) }
        return if (checkIn) api.offsiteCheckIn(lat0, lng0, addr, noteBody, part)
        else api.offsiteCheckOut(lat0, lng0, addr, noteBody, part)
    }

    private fun coords(lat: Double, lng: Double) = mapOf("lat" to lat.toString(), "lng" to lng.toString())

    private fun text(value: String): RequestBody = value.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun PresensiDayItem.toTable() = PresensiDayTable(
        date = date,
        attendAt = if (attend_at.isBlank()) "" else PresensiRules.toClock(attend_at),
        leaveAt = if (leave_at.isBlank()) "" else PresensiRules.toClock(leave_at),
        isHoliday = is_holiday == true,
        attendIsLate = attend_is_late == true,
        leaveIsEarly = leave_is_early == true,
        attendIsOffsite = attend_is_offsite == true,
        leaveIsOffsite = leave_is_offsite == true,
        attendStatus = attend_status,
        leaveStatus = leave_status,
        attendNote = attend_note,
        leaveNote = leave_note,
        attendAddress = attend_address,
        leaveAddress = leave_address,
        attendPhotoUrl = attend_photo_url,
        leavePhotoUrl = leave_photo_url,
        leaveRequestStatus = leave_request_status.orEmpty(),
        leaveRequestType = leave_request_type.orEmpty(),
    )

    private companion object {
        const val KEY_ABSENCE = "absence_setting"
        const val KEY_ABSENCE_AT = "absence_setting_at"
        const val SETTING_TTL_MS = 12L * 60 * 60 * 1000
    }
}
