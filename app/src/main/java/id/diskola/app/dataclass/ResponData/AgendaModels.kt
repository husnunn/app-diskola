package id.diskola.app.dataclass.ResponData

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import id.diskola.app.di.module.NullToEmptyString

// ---- `mobile/attendance/staff/agendas` — shapes from legacy `AgendaMingguanModel.kt:11-187`.
// Every object/list is nullable-with-default (legacy crashed the whole load on `"gate": null`). ----

@JsonClass(generateAdapter = true)
data class StaffAgendasResponse(
    @NullToEmptyString val message: String = "",
    val data: StaffAgendasData? = null,
)

@JsonClass(generateAdapter = true)
data class StaffAgendasData(
    val agenda_enabled: Boolean = false,
    val workgroup_id: Int? = null,
    @NullToEmptyString val date: String = "",
    val day: Int = 0,
    @NullToEmptyString val agenda_source: String = "",
    val gate: StaffAgendaGate? = null,
    val agendas: List<StaffAgendaItem>? = null,
    val summary: StaffAgendaSummary? = null,
)

/** The staff's school-gate presensi for the day (`in`/`out`) — comes in the same day response. */
@JsonClass(generateAdapter = true)
data class StaffAgendaGate(
    @Json(name = "in") val gateIn: StaffAgendaGateEvent? = null,
    val out: StaffAgendaGateEvent? = null,
)

@JsonClass(generateAdapter = true)
data class StaffAgendaGateEvent(
    @NullToEmptyString val time: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val via: String = "",
)

@JsonClass(generateAdapter = true)
data class StaffAgendaItem(
    val agenda_id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val kind: String = "",
    val window: StaffAgendaWindow? = null,
    val require_check: Boolean = false,
    val sort_order: Int = 0,
    val note: String? = null,
    val policy: StaffAgendaPolicy? = null,
    val event: StaffAgendaEvent? = null,
) {
    val startAt: String get() = window?.start_at.orEmpty()
    val endAt: String get() = window?.end_at.orEmpty()

    /** "start – end", `-` for a blank side — the legacy `windowLabel()`. */
    val windowLabel: String get() = "${startAt.ifBlank { "-" }} – ${endAt.ifBlank { "-" }}"

    val needsCheckout: Boolean
        get() = event != null && policy?.checkout_required == true && event.checkout_time.isNullOrBlank()

    val uiStatus: AgendaUiStatus
        get() = when {
            event == null -> AgendaUiStatus.PENDING
            needsCheckout -> AgendaUiStatus.NEED_CHECKOUT
            event.status.equals("Terlambat", ignoreCase = true) -> AgendaUiStatus.LATE
            else -> AgendaUiStatus.HADIR
        }
}

enum class AgendaUiStatus(val label: String) {
    PENDING("Belum"),
    NEED_CHECKOUT("Belum pulang"),
    LATE("Terlambat"),
    HADIR("Hadir"),
}

@JsonClass(generateAdapter = true)
data class StaffAgendaWindow(
    @NullToEmptyString val start_at: String = "",
    @NullToEmptyString val end_at: String = "",
)

@JsonClass(generateAdapter = true)
data class StaffAgendaPolicy(
    val checkout_required: Boolean? = null,
    val early_leave_enabled: Boolean? = null,
    val late_enabled: Boolean? = null,
) {
    val checkoutRequired: Boolean get() = checkout_required == true
    val earlyLeaveEnabled: Boolean get() = early_leave_enabled == true

    /** Legacy default is `true` when the server omits it. */
    val lateEnabled: Boolean get() = late_enabled != false
}

@JsonClass(generateAdapter = true)
data class StaffAgendaEvent(
    val id: Int = 0,
    @NullToEmptyString val time: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val via: String = "",
    val checkout_time: String? = null,
    val checkout_status: String? = null,
)

@JsonClass(generateAdapter = true)
data class StaffAgendaSummary(
    val required_sessions: Int = 0,
    val checked: Int = 0,
    val late: Int = 0,
    val missing: Int = 0,
)

/** Same body for `check` and `check-out`. */
@JsonClass(generateAdapter = true)
data class StaffAgendaCheckBody(
    val agenda_id: Int,
    val lat: Double,
    val lng: Double,
)

@JsonClass(generateAdapter = true)
data class StaffAgendaCheckResponse(
    @NullToEmptyString val message: String = "",
)

// ---- Room cache, one row per date (day) + one per session (item) ----

@Entity(tableName = "staff_agenda_day")
data class StaffAgendaDayTable(
    @PrimaryKey val date: String = "",
    val agenda_enabled: Boolean = false,
    val workgroup_id: Int? = null,
    val day: Int = 0,
    val agenda_source: String = "",
    val gate_json: String = "",
    val required_sessions: Int = 0,
    val checked: Int = 0,
    val late: Int = 0,
    val missing: Int = 0,
)

@Entity(
    tableName = "staff_agenda_item",
    primaryKeys = ["date", "agenda_id"],
    indices = [Index("date")],
)
data class StaffAgendaItemTable(
    val date: String = "",
    val agenda_id: Int = 0,
    val name: String = "",
    val kind: String = "",
    val start_at: String = "",
    val end_at: String = "",
    val require_check: Boolean = false,
    val sort_order: Int = 0,
    val note: String? = null,
    val policy_json: String = "",
    val event_json: String = "",
)

/** One day as the UI consumes it, rebuilt from [StaffAgendaDayTable]/[StaffAgendaItemTable]. */
data class AgendaDay(
    val date: String,
    val agendaEnabled: Boolean,
    val gate: StaffAgendaGate,
    val agendas: List<StaffAgendaItem>,
    val summary: StaffAgendaSummary,
)

/** Check page mode — route argument. */
@Serializable
enum class AgendaCheckMode { CHECK_IN, CHECK_OUT }
