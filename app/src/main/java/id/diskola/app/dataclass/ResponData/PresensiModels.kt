package id.diskola.app.dataclass.ResponData

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString

// Daily presensi (doc `07` §4–6, §10, §14.1). Every field is lenient: legacy models made most of
// them non-null, so one JSON `null` failed the whole response.

// ---- Data Absensi: `mobile/attendance/{student|staff}/by-month` ----

@JsonClass(generateAdapter = true)
data class PresensiMonthResponse(val data: List<PresensiDayItem> = emptyList())

@JsonClass(generateAdapter = true)
data class PresensiDayItem(
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val attend_at: String = "",
    @NullToEmptyString val leave_at: String = "",
    val is_holiday: Boolean? = null,
    val attend_is_late: Boolean? = null,
    val leave_is_early: Boolean? = null,
    val attend_is_offsite: Boolean? = null,
    val leave_is_offsite: Boolean? = null,
    @NullToEmptyString val attend_status: String = "",
    @NullToEmptyString val leave_status: String = "",
    @NullToEmptyString val attend_note: String = "",
    @NullToEmptyString val leave_note: String = "",
    @NullToEmptyString val attend_address: String = "",
    @NullToEmptyString val leave_address: String = "",
    @NullToEmptyString val attend_photo_url: String = "",
    @NullToEmptyString val leave_photo_url: String = "",
    val leave_request_status: String? = null,
    val leave_request_type: String? = null,
)

/** One day row. [attendAt]/[leaveAt] are already `HH:mm` (or blank). */
@Entity(tableName = "presensi_day", indices = [Index("date", unique = true)])
data class PresensiDayTable(
    @PrimaryKey val date: String = "",
    val attendAt: String = "",
    val leaveAt: String = "",
    val isHoliday: Boolean = false,
    val attendIsLate: Boolean = false,
    val leaveIsEarly: Boolean = false,
    val attendIsOffsite: Boolean = false,
    val leaveIsOffsite: Boolean = false,
    val attendStatus: String = "",
    val leaveStatus: String = "",
    val attendNote: String = "",
    val leaveNote: String = "",
    val attendAddress: String = "",
    val leaveAddress: String = "",
    val attendPhotoUrl: String = "",
    val leavePhotoUrl: String = "",
    val leaveRequestStatus: String = "",
    val leaveRequestType: String = "",
)

// ---- Rekap: `mobile/attendance/{student|staff}/by-year` ----

@JsonClass(generateAdapter = true)
data class PresensiYearResponse(val data: List<PresensiRekapItem> = emptyList())

/** `month` is shown verbatim (its format is unverified); a bare number is accepted too. */
@JsonClass(generateAdapter = true)
data class PresensiRekapItem(
    @NullToEmptyString val month: String = "",
    val ontime: Int? = null,
    val late: Int? = null,
    val izin: Int? = null,
    val sakit: Int? = null,
    val alpha: Int? = null,
)

@Entity(tableName = "presensi_rekap", primaryKeys = ["year", "orderIndex"])
data class PresensiRekapTable(
    val year: Int = 0,
    val orderIndex: Int = 0,
    val month: String = "",
    val ontime: Int = 0,
    val late: Int = 0,
    val izin: Int = 0,
    val sakit: Int = 0,
    val alpha: Int = 0,
)

// ---- Masuk/Pulang sekolah: `…/{student|staff}/check` + `check-in|check-out` ----

@JsonClass(generateAdapter = true)
data class PresensiCheckResponse(
    @NullToEmptyString val message: String = "",
    val data: PresensiCheckData? = null,
)

@JsonClass(generateAdapter = true)
data class PresensiCheckData(
    val allow_attendance: Boolean? = null,
    @NullToEmptyString val type_attendance: String = "",
    @NullToEmptyString val attendance_response_text: String = "",
    @NullToEmptyString val attendance_response_text_button: String = "",
    val schedule: PresensiSchedule? = null,
) {
    /** `type_attendance` "checkin" (any case) is Masuk, everything else Pulang (legacy rule). */
    val isCheckIn: Boolean get() = type_attendance.equals("checkin", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class PresensiSchedule(
    @NullToEmptyString val start_at: String = "",
    @NullToEmptyString val end_at: String = "",
    @NullToEmptyString val source: String = "",
    @NullToEmptyString val name: String = "",
)

// ---- Dinas luar (guru): `mobile/attendance/staff/offsite/*` ----

@JsonClass(generateAdapter = true)
data class OffsiteCheckResponse(
    val code: Int? = null,
    @NullToEmptyString val message: String = "",
    val data: OffsiteCheckData? = null,
)

@JsonClass(generateAdapter = true)
data class OffsiteCheckData(
    val allow_attendance: Boolean? = null,
    @NullToEmptyString val type_attendance: String = "",
    @NullToEmptyString val attendance_response_text_button: String = "",
    @NullToEmptyString val attendance_response_text: String = "",
    val require_photo: Boolean? = null,
    val offsite_enabled: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class OffsiteSubmitResponse(
    val code: Int? = null,
    @NullToEmptyString val message: String = "",
    val data: OffsiteSubmitData? = null,
)

@JsonClass(generateAdapter = true)
data class OffsiteSubmitData(
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val time: String = "",
    @NullToEmptyString val type_attendance: String = "",
    @NullToEmptyString val attendance_response_text_button: String = "",
    @NullToEmptyString val attendance_response_text: String = "",
    val allow_attendance: Boolean? = null,
)

// ---- Izin/sakit: `…/{student|staff}/leave-request*` ----

@JsonClass(generateAdapter = true)
data class LeaveRequestTodayResponse(val data: LeaveRequestItem? = null)

@JsonClass(generateAdapter = true)
data class LeaveRequestListResponse(
    val data: List<LeaveRequestItem> = emptyList(),
    val meta: LeaveRequestMeta? = null,
)

@JsonClass(generateAdapter = true)
data class LeaveRequestMeta(
    val total: Int? = null,
    val current_page: Int? = null,
    val last_page: Int? = null,
)

@JsonClass(generateAdapter = true)
data class LeaveRequestSubmitResponse(
    @NullToEmptyString val message: String = "",
    val data: LeaveRequestItem? = null,
)

@JsonClass(generateAdapter = true)
data class LeaveRequestItem(
    @NullToEmptyString val uuid: String = "",
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val note: String = "",
    @NullToEmptyString val file_url: String = "",
    @NullToEmptyString val approval_status: String = "",
    val rejection_note: String? = null,
    val reviewed_at: String? = null,
)

@Entity(tableName = "leave_request")
data class LeaveRequestTable(
    @PrimaryKey val uuid: String = "",
    val date: String = "",
    val status: String = "",
    val note: String = "",
    val fileUrl: String = "",
    val approvalStatus: String = "",
    val rejectionNote: String = "",
    val reviewedAt: String = "",
)

fun LeaveRequestItem.toTable() = LeaveRequestTable(
    uuid = uuid,
    date = date,
    status = status,
    note = note,
    fileUrl = file_url,
    approvalStatus = approval_status,
    rejectionNote = rejection_note.orEmpty(),
    reviewedAt = reviewed_at.orEmpty(),
)

// ---- Feature gate: `mobile/app/check-feature-availability` ----

@JsonClass(generateAdapter = true)
data class FeatureAvailabilityResponse(val data: FeatureAvailabilityData? = null)

@JsonClass(generateAdapter = true)
data class FeatureAvailabilityData(
    @NullToEmptyString val name: String = "",
    val available: Boolean? = null,
    val message: String? = null,
)
