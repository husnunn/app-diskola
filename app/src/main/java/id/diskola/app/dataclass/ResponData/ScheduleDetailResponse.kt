package id.diskola.app.dataclass.ResponData

import id.diskola.app.di.module.NullToEmptyString
import androidx.annotation.Keep
import androidx.room.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object ItemType {
    const val CLASS = 1
    const val SUBJECT = 2
    const val TEACHER = 3
}

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "schedule",
    indices = [Index(name = "schedules_idx", unique = true, value = ["id"])],
    primaryKeys = ["plot_start_at"]
)
data class ScheduleTable(
//    @PrimaryKey @Json(name = "subject_schedule_id") val id: Int = 0,
//    val date: String = "",
//    val name_of_day: String = "",
//    val school_name: String = "",
//    val class_name: String = "",
//    val teacher_name: String = "",
//    val teacher_nip: String = "",
//    val subject_name: String = "",
//    @Json(name = "subject_icon_image") val subject_image: String = "",
//    val time_plot_start_at: String = "",
//    val time_plot_end_at: String = "",
//    var status: String = "",
//    @NullToEmptyString val attend_at: String = "",
//    @NullToEmptyString val leave_at: String = "",
//    @NullToEmptyString var late_at: String = "",

    @Json(name = "attendance_id") val id : Int = 0,
//    val user_id : String = "",
    @NullToEmptyString var plot_start_at : String = "",
    @NullToEmptyString var plot_end_at : String = "",
    var late_at : String?,
    @NullToEmptyString val end_at : String = "",
    @NullToEmptyString val teacher_name : String = "",
    @NullToEmptyString val teacher_nip : String = "",
    @NullToEmptyString val school_name : String = "",
    @NullToEmptyString val learning_objective : String = "",
    @NullToEmptyString var status : String = "",
    var is_present: Boolean = false,
    @NullToEmptyString val school_major_name : String = "",
    @NullToEmptyString val subject_name : String = "",
    @NullToEmptyString @Json(name = "subject_icon_image") val subject_image: String = "",
    @NullToEmptyString val class_name : String = "",
//    val grade : Int = 0,
    @NullToEmptyString val created_at : String = "",
//    val laravel_through_key : Int = 0,
//    val semester : String = "",
    val present : Int = 0,
    val absent : Int = 0,
    @NullToEmptyString var timeLeft: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class PresensiListResponse(val data: List<PresensiItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class PresensiItem(
    var id : Int = 0,
    var school_id : Int = 0,
    var name_of_day : String = "",
    var start_at : String = "",
    var end_at : String = "",
    var journals : Int = 0,
    var school_attendances : List<ScheduleTable> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
data class PresensiClassRekapResponse(val data: PresensiClassRekapData = PresensiClassRekapData())

@Keep
@JsonClass(generateAdapter = true)
data class PresensiClassRekapData(
    val id: Int = 0,
    @NullToEmptyString val subject: String = "",
    @NullToEmptyString val classroom: String = "",
    @NullToEmptyString val password: String = "",
    @NullToEmptyString val time_plot: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val start_time: String = "",
    @NullToEmptyString val end_time: String = "",
    val att_presence: Int? = null,
    val att_not_presence: Int? = null,
    val att_total: Int? = null,
    val teacher: TeacherItem = TeacherItem(),
    val attendances: List<PresensiClassAttendance> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
data class PresensiClassAttendance(
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val nis: String = "",
    @NullToEmptyString val start_time: String = "",
    val is_late: Boolean? = true
)

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "schedule_detail",
    indices = [Index(name = "schedule_detail_idx", value = ["id"], unique = true)]
)
data class ScheduleDetailTable(
    @PrimaryKey @Json(name = "attendance_id") val id: Int = 0,
    val school_subject_schedule_id: Int = 0,
    val start_at: String = "",
    val end_at: String = "",
    val late_limit: Int = 0,
    val teacher_name: String = "",
    val teacher_nip: String = "",
    val school_name: String = "",
    val school_major_name: String = "",
    val subject_name: String = "",
    @NullToEmptyString @Json(name = "subject_icon_image") var subject_image: String = "",
    @NullToEmptyString val class_name: String = "",
    @NullToEmptyString val status: String = "",
    val grade: Int = 0,
    @NullToEmptyString val learning_objective: String = "",
    @NullToEmptyString val time_plot: String = "",
    @NullToEmptyString val plot_start_at: String = "",
    @NullToEmptyString val plot_end_at: String = "",
    @NullToEmptyString val total_student: String = "",
)

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "schedule_attendance",
    indices = [Index(name = "schedule_attendance_idx", value = ["detail_id"], unique = true)]
)
data class ScheduleAttendanceTable(
    @PrimaryKey @Json(name = "id") val detail_id: Int = 0,
    val attendance_id: Int = 0,
    val name: String = "",
    val nisn: String = "",
    @NullToEmptyString val attend_at: String = "",
    val is_attend: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
data class ScheduleListAttendance(
    val data: List<ScheduleAttendanceTable> = emptyList()
)

data class ScheduleDetailAttendance(
    @Embedded val detail: ScheduleDetailTable,
    @Relation(parentColumn = "id", entityColumn = "attendance_id")
    val data: List<ScheduleAttendanceTable> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
data class ScheduleDetailResponse(val data: ScheduleDetailTable = ScheduleDetailTable())

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "absensi",
    indices = [Index(name = "absensi_idx", unique = true, value = ["date"])]
)
data class AbsensiTable(
    @PrimaryKey val date: String = "",
    var month: Int = 0,
    var year: Int = 0,
    @NullToEmptyString var dateLabel: String = "",
    @NullToEmptyString var attend_at: String = "",
    @NullToEmptyString var leave_at: String = "",
    val is_holiday: Boolean = false,
    val attend_is_late: Boolean = false,
    val leave_is_early: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
data class AbsensiResponse(val data: List<AbsensiTable> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "rekap_absensi",
    indices = [Index(name = "rekap_absensi_idx", unique = true, value = ["date"])]
)
data class RekapAbsensiTable(
    @PrimaryKey var date: String = "",
    var month: String = "",
    var year: Int = 0,
    var ontime: String = "",
    var late: String = "",
    var order: Int = 0
)

@Keep
@JsonClass(generateAdapter = true)
data class RekapAbsensiResponse(val data: List<RekapAbsensiTable> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class CheckAbsenResponse(
    @NullToEmptyString val message: String = "",
    val data: CheckAbsenResponseData = CheckAbsenResponseData()
)

@Keep
@JsonClass(generateAdapter = true)
data class CheckAbsenResponseData(
    val allow_attendance: Boolean = false,
    @NullToEmptyString val type_attendance: String = "",
    @NullToEmptyString val attendanceResponseText: String = "",
    @NullToEmptyString val attendanceResponseTextButton: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class ListClassItemResponse(val data: List<ClassItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class ListSubjectItemResponse(val data: List<SubjectItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class ListTeacherItemResponse(val data: List<TeacherItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class ListPlotItemResponse(val data: List<PlotItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class ClassItem(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    val grade: Int = 0,
)

@Keep
@JsonClass(generateAdapter = true)
data class SubjectsItem(
    @PrimaryKey val id: Int = 0,
    val name: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class TeachersItem(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    @NullToEmptyString val nip: String = "",
    val user: TeacherDetail = TeacherDetail()
)

@Keep
@JsonClass(generateAdapter = true)
data class TeacherDetail(
    @PrimaryKey @Json(name = "id") val uuid: String = ""
)
@Keep
@JsonClass(generateAdapter = true)
data class PlotItem(
    @PrimaryKey val time_plot_id: Int = 0,
    val time_plot_day: String = "",
    val time_plot_start_at: String = "",
    val time_plot_end_at: String = ""
)