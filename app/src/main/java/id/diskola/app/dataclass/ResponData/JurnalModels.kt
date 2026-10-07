package id.diskola.app.dataclass.ResponData

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString

// Jurnal KBM (doc `07` §7–9, §14.2). Shapes were re-read from the legacy `PresensiModel.kt`; every field
// is lenient because the legacy models were non-null almost everywhere and one JSON `null` failed the
// whole response.

// ---- `mobile/attendance/schedule?date=` — one entry per time plot, with that plot's class sessions ----

@JsonClass(generateAdapter = true)
data class JurnalScheduleResponse(val data: List<JurnalPlot>? = null)

@JsonClass(generateAdapter = true)
data class JurnalPlot(
    val id: Int = 0,
    val school_id: Int? = null,
    @NullToEmptyString val name_of_day: String = "",
    @NullToEmptyString val start_at: String = "",
    @NullToEmptyString val end_at: String = "",
    val journals: Int? = null,
    val school_subject_schedule_id: Int? = null,
    val school_attendances: List<JurnalAttendance>? = null,
)

@JsonClass(generateAdapter = true)
data class JurnalAttendance(
    @Json(name = "attendance_id") val attendanceId: Int = 0,
    @NullToEmptyString val plot_start_at: String = "",
    @NullToEmptyString val plot_end_at: String = "",
    @NullToEmptyString val teacher_name: String = "",
    @NullToEmptyString val teacher_nip: String = "",
    @NullToEmptyString val school_name: String = "",
    @NullToEmptyString val learning_objective: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val school_major_name: String = "",
    @NullToEmptyString val subject_name: String = "",
    @NullToEmptyString val subject_icon_image: String = "",
    @NullToEmptyString val class_name: String = "",
    @NullToEmptyString val created_at: String = "",
    /** null = the teacher has not started the class; "" = started with no late mark. */
    val late_at: String? = null,
    val is_present: Boolean? = null,
)

// ---- Scopes + pickers ----

@JsonClass(generateAdapter = true)
data class TeacherDataScopeResponse(val data: TeacherDataScope? = null)

@JsonClass(generateAdapter = true)
data class TeacherDataScope(
    val teacher_master_data_scope: String? = null,
    val scope: String? = null,
) {
    /** Only `assigned` is special; anything else (or blank) means every class/subject/plot is offered. */
    val isAssigned: Boolean
        get() = (teacher_master_data_scope?.takeIf { it.isNotBlank() } ?: scope).orEmpty().trim().equals("assigned", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class CaptureScopeResponse(val data: CaptureScope? = null)

@JsonClass(generateAdapter = true)
data class CaptureScope(
    val required: Boolean? = null,
    @NullToEmptyString val label: String = "",
)

@JsonClass(generateAdapter = true)
data class ListPlotResponse(val data: List<PlotItem>? = null)

@JsonClass(generateAdapter = true)
data class PlotItem(
    val time_plot_id: Int = 0,
    @NullToEmptyString val time_plot_day: String = "",
    @NullToEmptyString val time_plot_start_at: String = "",
    @NullToEmptyString val time_plot_end_at: String = "",
) {
    val label: String get() = "${time_plot_start_at.take(5)} - ${time_plot_end_at.take(5)}"
}

@JsonClass(generateAdapter = true)
data class ListClassResponse(val data: List<JurnalClassItem>? = null)

@JsonClass(generateAdapter = true)
data class JurnalClassItem(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    val grade: Int? = null,
) {
    /** `"<grade> - <name>"` when grade > 0, the bare name otherwise (legacy picker rule). */
    val label: String get() = if ((grade ?: 0) > 0) "$grade - $name" else name
}

@JsonClass(generateAdapter = true)
data class ListSubjectResponse(val data: List<JurnalSubjectItem>? = null)

@JsonClass(generateAdapter = true)
data class JurnalSubjectItem(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
)

@JsonClass(generateAdapter = true)
data class ListTeacherResponse(val data: List<JurnalTeacherItem>? = null)

/** [user]`.id` is the teacher's **UUID** — the `teacher_id` the student journal form sends. */
@JsonClass(generateAdapter = true)
data class JurnalTeacherItem(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val nip: String = "",
    val user: JurnalTeacherUser? = null,
)

@JsonClass(generateAdapter = true)
data class JurnalTeacherUser(@NullToEmptyString val id: String = "")

// ---- Create / start / verify ----

@JsonClass(generateAdapter = true)
data class JournalCreateResponse(val data: List<JournalCreatedAttendance>? = null)

@JsonClass(generateAdapter = true)
data class JournalCreatedAttendance(
    @Json(name = "attendance_id") val attendanceId: Int = 0,
    @NullToEmptyString val plot_start_at: String = "",
    @NullToEmptyString val plot_end_at: String = "",
    @NullToEmptyString val learning_objective: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val subject_name: String = "",
    @NullToEmptyString val class_name: String = "",
    @NullToEmptyString val capture_photo_url: String = "",
)

/** Teacher "Mulai kelas". Null coordinates are omitted from the body. */
@JsonClass(generateAdapter = true)
data class TeacherCheckInRequest(
    val school_attendance_id: Int,
    val lat: Double? = null,
    val lng: Double? = null,
)

/** Student "Verifikasi Jurnal": [status] is Terlaksana / Penugasan / Tidak Terlaksana. */
@JsonClass(generateAdapter = true)
data class JournalUpdateRequest(
    val school_attendance_id: Int,
    val status: String,
)

@JsonClass(generateAdapter = true)
data class LearningQrRequest(
    val time_plot_id: Int,
    val subject_id: Int,
    val class_id: Int,
    val teacher_id: Int,
    val schedule_id: Int,
)

// ---- Detail: `schedule/{id}`, `schedule/{id}/list-student`, `journal/{scheduleId}`, `journal/save` ----

@JsonClass(generateAdapter = true)
data class ScheduleDetailResponse(val data: ScheduleDetailData? = null)

@JsonClass(generateAdapter = true)
data class ScheduleDetailData(
    @Json(name = "attendance_id") val attendanceId: Int = 0,
    val school_subject_schedule_id: Int? = null,
    @NullToEmptyString val start_at: String = "",
    @NullToEmptyString val end_at: String = "",
    @NullToEmptyString val teacher_name: String = "",
    @NullToEmptyString val teacher_nip: String = "",
    @NullToEmptyString val school_major_name: String = "",
    @NullToEmptyString val subject_name: String = "",
    @NullToEmptyString val class_name: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val learning_objective: String = "",
    @NullToEmptyString val plot_start_at: String = "",
    @NullToEmptyString val plot_end_at: String = "",
    @NullToEmptyString val capture_photo_url: String = "",
    val total_attended_student: Int? = null,
    val status_breakdown: StatusBreakdown? = null,
    val student_attendances: List<AttendanceStudentRow>? = null,
)

@JsonClass(generateAdapter = true)
data class StatusBreakdown(
    val hadir: Int? = null,
    val izin: Int? = null,
    val sakit: Int? = null,
    val alpha: Int? = null,
)

@JsonClass(generateAdapter = true)
data class AttendanceStudentRow(
    val student: AttendanceStudent? = null,
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val attend_at: String = "",
    @NullToEmptyString val leave_at: String = "",
    @NullToEmptyString val status: String = "",
    val status_source: String? = null,
    val is_late: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class AttendanceStudent(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val nis: String = "",
)

@JsonClass(generateAdapter = true)
data class ListStudentResponse(val data: List<ListStudentRow>? = null)

@JsonClass(generateAdapter = true)
data class ListStudentRow(
    val school_attendance_id: Int? = null,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val attend_at: String = "",
)

@JsonClass(generateAdapter = true)
data class JournalPreviewResponse(val data: JournalPreviewData? = null)

@JsonClass(generateAdapter = true)
data class JournalPreviewData(
    val attendance_id: Int? = null,
    val school_subject_schedule_id: Int? = null,
    @NullToEmptyString val date: String = "",
    val subject_name: String? = null,
    val class_name: String? = null,
    val learning_objective: String? = null,
    val session_status: String? = null,
    val is_started: Boolean? = null,
    val students: List<JournalStudent>? = null,
)

@JsonClass(generateAdapter = true)
data class JournalStudent(
    val student_id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val nisn: String = "",
    val status: String? = null,
    val status_source: String? = null,
    val attend_at: String? = null,
    val is_late: Boolean? = null,
    val is_overridable: Boolean? = null,
)

/** A blank [learning_objective] is sent as an absent field (Moshi omits nulls). */
@JsonClass(generateAdapter = true)
data class JournalSaveRequest(
    val school_subject_schedule_id: Int,
    val learning_objective: String? = null,
    val session_status: String,
    val students: List<JournalSaveStudent>,
)

@JsonClass(generateAdapter = true)
data class JournalSaveStudent(val student_id: Int, val status: String)
