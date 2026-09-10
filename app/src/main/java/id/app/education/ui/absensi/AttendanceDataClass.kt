package id.app.education.ui.absensi

data class AttendanceScheduleUiModel(
    val subjectScheduleId: Int,
    val subjectName: String,
    val teacherName: String,
    val className: String,
    val timeStart: String,
    val timeEnd: String,
    val status: String,
    val lateAt: String,
    val attendAt: String,
    val leaveAt: String
)