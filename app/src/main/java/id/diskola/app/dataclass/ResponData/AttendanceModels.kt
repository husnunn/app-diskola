package id.diskola.app.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class AttendanceScheduleResponse(
    val data: List<AttendanceScheduleItem> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
data class AttendanceScheduleItem(
    val date: String? = "",
    @Json(name = "subject_schedule_id") val subjectScheduleId: Int? = 0,
    @Json(name = "name_of_day") val nameOfDay: String? = "",
    @Json(name = "school_name") val schoolName: String? = "",
    @Json(name = "class_name") val className: String? = "",
    @Json(name = "teacher_id") val teacherId: Int? = null,
    @Json(name = "teacher_name") val teacherName: String? = "",
    @Json(name = "teacher_nip") val teacherNip: String? = "",
    @Json(name = "subject_id") val subjectId: Int? = 0,
    @Json(name = "subject_name") val subjectName: String? = "",
    @Json(name = "subject_icon_image") val subjectIconImage: String? = "",
    @Json(name = "attend_at") val attendAt: String? = "",
    @Json(name = "leave_at") val leaveAt: String? = "",
    @Json(name = "time_plot_start_at") val timePlotStartAt: String? = "",
    @Json(name = "time_plot_end_at") val timePlotEndAt: String? = "",
    val status: String? = "",
    @Json(name = "late_at") val lateAt: String? = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class AttendanceDetailResponse(
    val data: AttendanceDetailData
)

@Keep
@JsonClass(generateAdapter = true)
data class AttendanceDetailData(
    @Json(name = "attendance_id") val attendanceId: Int = 0,
    @Json(name = "subject_schedule_id") val subjectScheduleId: Int = 0,
    @Json(name = "class_password") val classPassword: String = "",
    @Json(name = "late_limit") val lateLimit: String? = "",
    @Json(name = "teacher_name") val teacherName: String = "",
    @Json(name = "school_name") val schoolName: String = "",
    @Json(name = "school_major_name") val schoolMajorName: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    @Json(name = "subject_icon_image") val subjectIconImage: String = "",
    @Json(name = "class_name") val className: String = "",
    val grade: Int = 0,
    @Json(name = "name_of_day") val nameOfDay: String = "",
    @Json(name = "attend_at") val attendAt: String = "",
    @Json(name = "leave_at") val leaveAt: String = "",
    @Json(name = "plot_start_at") val plotStartAt: String = "",
    @Json(name = "plot_end_at") val plotEndAt: String = "",
    @Json(name = "total_attended_student") val totalAttendedStudent: String? = "",
    @Json(name = "total_student") val totalStudent: String? = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class AttendRequest(
    @Json(name = "school_subject_schedule_id") val scheduleId: Int,
    val password: String
)

@Keep
@JsonClass(generateAdapter = true)
data class LeaveRequest(
    @Json(name = "school_subject_schedule_id") val scheduleId: Int
)

@Keep
@JsonClass(generateAdapter = true)
data class StartAttendanceRequest(
    @Json(name = "school_subject_schedule_id") val scheduleId: Int,
    val password: String,
    @Json(name = "late_limit") val lateLimit: Int
)

@Keep
@JsonClass(generateAdapter = true)
data class EndAttendanceRequest(
    @Json(name = "school_subject_schedule_id") val scheduleId: Int
)

@Keep
@JsonClass(generateAdapter = true)
data class AttendanceActionResponse(
    val data: AttendanceActionData
)

@Keep
@JsonClass(generateAdapter = true)
data class AttendanceActionData(
    @Json(name = "school_subject_schedule_id") val scheduleId: Int = 0,
    @Json(name = "student_id") val studentId: Int = 0,
    @Json(name = "attend_at") val attendAt: String? = null,
    @Json(name = "leave_at") val leaveAt: String? = null
)
