package id.diskola.app.dataclass.ResponData

import id.diskola.app.di.module.NullToEmptyString

import androidx.annotation.Keep
import androidx.room.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "classroom",
    indices = [Index(name = "classroom_idx", value = ["id"], unique = true)]
)
data class ClassRoomTable(
    @PrimaryKey val id: Int = 0,
    var name: String = "",
    val grade: Int = 0,
    val majorId: Int = 0
)

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "major",
    indices = [Index(name = "major_idx", value = ["id"], unique = true)]
)
data class MajorTable(
    @PrimaryKey val id: Int = 0,
    val name: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class HomeworkItem(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val is_overdue: Boolean = false,
    val checked: Int = 1,
    val downloded: Int = 0,
    val uploaded: Int = 0,
    val information_label: String = "",
    val file_name: String = "",
    val file_path: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
    val message_label: String = "",
    val end_at_label: String = "",
    val upload_at_label: String = "",
    val file_student_name: String = "",
    val file_student_path: String = "",
    val file_student_format: String = "",
    val file_student_type: String = "",
    val file_student_size: String = "",
    val score: Int = 0,
    val school: PembelajaranSekolahItem = PembelajaranSekolahItem(),
    val subject: MapelItem = MapelItem(),
    val teacher: TeacherItem = TeacherItem(),
    @Json(name = "class") val classRoom: ClassRoomTable = ClassRoomTable(),
    val uri: MateriLink? = null,
    val uri_student: MateriLink? = null,
    val schedule: AssignmentSchedule = AssignmentSchedule(),
    // change to file_edited because backend not ready yet
//    val is_explanations: Boolean = false,
//    val explanations: Explanations = Explanations(),
    val file_edited: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class Explanations(
    val id: Int = 0,
    val explanation_id: Int = 0,
    val class_id: Int = 0,
    val class_name: String = "",
    val major_id: Int = 0,
    val major: String = "",
    val grade: String = "",
    val teacher_id: Int = 0,
    val teacher_name: String = "",
    val subject_name: String = "",
    val file_name: String = "",
    val file_size: Int = 0,
    val file_type: String = "",
    val file_path: String = ""
)

@Entity(
    tableName = "homework",
    indices = [Index(name = "homework_idx", value = ["id"], unique = true)]
)
data class HomeworkTable(
    @PrimaryKey val id: Int = 0,
    val title: String = "",
    val description: String = "",
    var type: Int = 0,
    var is_overdue: Boolean = false,
    val checked: Int = 1,
    val downloded: Int = 0,
    val uploaded: Int = 0,
    val information_label: String = "",
    val file_name: String = "",
    val file_path: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
    val message_label: String = "",
    val end_at_label: String = "",
    val upload_at_label: String = "",
    val file_student_name: String = "",
    val file_student_path: String = "",
    val file_student_format: String = "",
    val file_student_type: String = "",
    val file_student_size: String = "",
    // change to file_edited because backend not ready yet
//    val explanation_id: Int = 0,
//    val explanation_file_name: String = "",
//    val explanation_file_size: Int = 0,
//    val explanation_file_type: String = "",
    val explanation_file_path: String = "",
    val score: Int = 0,
    val subjectId: Int = 0,
    val teacherId: Int = 0,
    val sosmedUserId: Int = 0,
    val classRoomId: Int = 0,
    val scheduleId: Int = 0,
    val scheduleDay: String = "",
    val scheduleTimePlot: String = ""
)

object HomeworkLinkSource {
    val SRC_TEACHER = 0
    val SRC_STUDENT = 1
}

@Entity(tableName = "homework_link", indices = [Index("homework_id", "link", unique = true)])
data class HomeworkLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val homework_id: Int = 0,
    val link: String = "",
    val src: Int = HomeworkLinkSource.SRC_TEACHER
)

data class HomeworkItemTable(
    @Embedded val homework: HomeworkTable,
    @Relation(parentColumn = "teacherId", entityColumn = "id")
    val teacher: TeacherTable?,
    @Relation(parentColumn = "sosmedUserId", entityColumn = "id")
    val userTable: UserTable?,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val mapel: MapelTable?,
    @Relation(parentColumn = "id", entityColumn = "homework_id")
    val links: List<HomeworkLink> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "homework_collected",
    indices = [Index(name = "hc_idx", value = ["id"], unique = true)]
)
data class HomeworkCollected(
    @PrimaryKey val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val upload_at_label: String = "",
    val end_at_label: String = "",
    val count_assignment_collected_all: String = "",
    val count_assignment_collected_scored: String = "",
    val count_assignment_collected: String = "",
    val message_label: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class Assignment(
    @PrimaryKey val id: Int = 0,
    val upload_at: String = "",
    var score: Int = 0,
    var scored: Int = 0,
    val uploaded: Int = 0,
    var scored_at: String = "",
    val file_name: String = "",
    val file_path: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
    val student: StudentItem = StudentItem(),
    val subject_assignment: HomeworkItem = HomeworkItem(),
    val uri_student: MateriLink? = MateriLink()
)

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentData(
    val id: Int = 0,
    val upload_at_label: String = "",
    val end_at_label: String = "",
    val count_assignment_collected_all: Int = 0,
    val count_assignment_collected_scored: Int = 0,
    val count_assignment_collected: Int = 0,
    val assignments: List<Assignment> = emptyList(),
    val subject: MapelItem = MapelItem(),
    val `class`: ClassRoomTable = ClassRoomTable()
)

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentResponse(val data: AssignmentData = AssignmentData())

@Keep
@JsonClass(generateAdapter = true)
data class HomeworkCollectedResponse(
    val data: List<HomeworkCollected> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
data class HomeworkResponse(
    val data: List<HomeworkItem> = emptyList()
)

@Keep
@JsonClass(generateAdapter = true)
data class HomeworkCreateResponse(
    val data: HomeworkItem = HomeworkItem()
)

@Keep
@JsonClass(generateAdapter = true)
data class ClassRoomResponse(val data: List<ClassRoomTable> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentDayResp(val data: List<AssignmentDay> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentDay(
    @NullToEmptyString val key: String = "",
    @NullToEmptyString val lable: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentScheduleResp(val data: List<AssignmentSchedule> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentSchedule(
    val id: Int = 0,
    val day: String = "",
    val class_room: ClassRoomTable = ClassRoomTable(),
    val time_plot: AssignmentTimePlot = AssignmentTimePlot(),
    val subject: MapelItem = MapelItem()
)

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentTimePlot(
    val id: Int = 0,
    val day: String = "",
    val start_at: String = "",
    val end_at: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class CollectHomeworkResponse(val data: CollectHomework = CollectHomework())

@Keep
@JsonClass(generateAdapter = true)
data class CollectHomework(val id: Int = 0)