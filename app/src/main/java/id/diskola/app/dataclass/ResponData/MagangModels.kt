package id.diskola.app.dataclass.ResponData

import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString

// Magang / internship (doc `07` §11, §14.4). Shapes come from reading the legacy `MagangModel.kt` and from what the legacy screens show for a
// dev account (`check_in_at` as `yyyy-MM-dd HH:mm:ss`, schedule times as `HH:mm:ss`), not from a published contract, so every field is lenient:
// one JSON `null` (or a number where a string was expected) must not fail the list. Company coordinates and radius arrive as strings (legacy
// parsed them with `toDoubleOrNull`); Moshi also reads a number as a string.

// ---- `mobile/internship/schedule` — one entry per day the student is expected at a company ----

@JsonClass(generateAdapter = true)
data class MagangScheduleResponse(val data: List<MagangScheduleItem>? = null)

@JsonClass(generateAdapter = true)
data class MagangScheduleItem(
    /** The schedule id: what `attend`, `leave` and `leave-request` send back as `id`. */
    val id: Long = 0,
    val company: MagangCompany? = null,
    val schedule: MagangScheduleTime? = null,
    val attendance: MagangAttendanceState? = null,
)

@JsonClass(generateAdapter = true)
data class MagangCompany(
    /** Dev rows carry ids past `Int.MAX_VALUE` (2149267629): an `Int` here failed the whole schedule list. */
    val id: Long? = null,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val address: String = "",
    @NullToEmptyString val latitude: String = "",
    @NullToEmptyString val longitude: String = "",
    @NullToEmptyString val radius: String = "",
)

@JsonClass(generateAdapter = true)
data class MagangScheduleTime(
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val start_time: String = "",
    @NullToEmptyString val end_time: String = "",
)

/** What the student may do with this day: the buttons on the card come straight from these flags. */
@JsonClass(generateAdapter = true)
data class MagangAttendanceState(
    @NullToEmptyString val status: String = "",
    val can_attend: Boolean? = null,
    val can_leave: Boolean? = null,
    val can_submit_leave_request: Boolean? = null,
    val leave_request: MagangLeaveRef? = null,
    /** Legacy defaulted to true when the server said nothing. */
    val radius_enabled: Boolean? = null,
)

/** The izin already filed for this schedule day, as the schedule list reports it. */
@JsonClass(generateAdapter = true)
data class MagangLeaveRef(
    val id: Long? = null,
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val note: String = "",
    @NullToEmptyString val file: String = "",
    val can_delete: Boolean? = null,
)

// ---- `mobile/internship/attendance` — reports (check-in/out) and filed izin, told apart by `record_type` ----

@JsonClass(generateAdapter = true)
data class MagangAttendanceResponse(val data: List<MagangAttendanceItem>? = null)

@JsonClass(generateAdapter = true)
data class MagangAttendanceItem(
    /** `leave_request` for an izin; anything else (or missing) is an attendance report. */
    val record_type: String? = null,
    val id: Long = 0,
    @NullToEmptyString val date: String = "",
    /** For an izin this holds the type (`sakit` / `izin`), not an approval state. */
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val note: String = "",
    val can_delete: Boolean? = null,
    @NullToEmptyString val check_in_at: String = "",
    @NullToEmptyString val check_in_status: String = "",
    @NullToEmptyString val check_out_at: String = "",
    @NullToEmptyString val check_out_status: String = "",
    @NullToEmptyString val file: String = "",
    @NullToEmptyString val daily_report: String = "",
    val location: MagangLocation? = null,
    val company: MagangCompany? = null,
)

/** The server spells the latitude `latitide` in some rows and `latitude` in others. */
@JsonClass(generateAdapter = true)
data class MagangLocation(
    @NullToEmptyString val latitide: String = "",
    @NullToEmptyString val latitude: String = "",
    @NullToEmptyString val longitude: String = "",
)

// ---- Requests ----

/** `lat`/`lng` are doubles here (Presensi sends them as strings). */
@JsonClass(generateAdapter = true)
data class MagangAttendRequest(val id: Long, val lat: Double, val lng: Double)
