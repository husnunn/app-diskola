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

// `MajorItem` (dataclass/ResponData/MapelResponse.kt) is the one `@Entity(tableName = "major")` —
// this file used to declare a second, unused entity class pointed at the same table name, which
// would have been a Room schema conflict the moment both got registered in `LocalDatabase`.

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
    val school: PembelajaranSekolahItem? = null,
    val subject: MapelItem = MapelItem(),
    val teacher: TeacherItem = TeacherItem(),
    @Json(name = "class") val classRoom: ClassRoomTable = ClassRoomTable(),
    val uri: MateriLink? = null,
    val uri_student: MateriLink? = null,
    val schedule: AssignmentSchedule = AssignmentSchedule(),
    // change to file_edited because backend not ready yet
//    val is_explanations: Boolean = false,
//    val explanations: Explanations = Explanations(),
    val file_edited: String = "",
    // Doc §5.5 "Jawaban: cache Room ... jika kosong & ada student_assignment_id ... GET
    // .../collect/{studentAssignmentId}" — not confirmed present on every list endpoint's JSON,
    // kept nullable; the Room-cached fallback (set at submit time) covers the case it's absent.
    val student_assignment_id: Int? = null,
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

/** Denormalized Room cache for Tugas (doc `05-pembelajaran-materi-tugas.md` §5.7/§6.6) — one table
 * serves both student tabs (`type` = backlog/done/scored) and the teacher's own list, same pattern
 * as `MateriTable`: flat columns, no `@Relation` joins, mapped via [fromHomeworkItem]. */
@Entity(
    tableName = "homework",
    indices = [Index(name = "homework_idx", value = ["id"], unique = true)]
)
data class HomeworkTable(
    @PrimaryKey val id: Long = 0L,
    /** 0 = backlog (Belum), 1 = done (Sudah), 2 = scored (Nilai) — doc §5.4: status comes purely
     * from which endpoint last loaded the row, not computed from dates. */
    val type: Int = 0,
    val title: String = "",
    val description: String = "",
    val is_overdue: Boolean = false,
    val checked: Int = 1,
    val downloded: Int = 0,
    val uploaded: Int = 0,
    val information_label: String = "",
    val message_label: String = "",
    val end_at_label: String = "",
    val upload_at_label: String = "",
    val score: Int = 0,
    val file_name: String = "",
    val file_path: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
    val explanation_file_path: String = "",
    val subject_id: Int = 0,
    val subject_name: String = "",
    val teacher_id: Int = 0,
    val teacher_name: String = "",
    val class_id: Int = 0,
    val class_name: String = "",
    val schedule_id: Int = 0,
    val link: String = "",
    val link_student: String = "",
    /** Doc §5.4: only rows whose `school.uuid` matches the current session school are kept. */
    val school_uuid: String = "",
    val student_assignment_id: Int = 0,
) {
    companion object {
        const val TYPE_BACKLOG = 0
        const val TYPE_DONE = 1
        const val TYPE_SCORED = 2

        fun fromHomeworkItem(it: HomeworkItem, type: Int): HomeworkTable = HomeworkTable(
            id = it.id.toLong(),
            type = type,
            title = it.title,
            description = it.description,
            is_overdue = it.is_overdue,
            checked = it.checked,
            downloded = it.downloded,
            uploaded = it.uploaded,
            information_label = it.information_label,
            message_label = it.message_label,
            end_at_label = it.end_at_label,
            upload_at_label = it.upload_at_label,
            score = it.score,
            file_name = it.file_name,
            file_path = it.file_path,
            file_format = it.file_format,
            file_type = it.file_type,
            file_size = it.file_size,
            explanation_file_path = it.file_edited,
            subject_id = it.subject.id,
            subject_name = it.subject.name,
            teacher_id = it.teacher.id,
            teacher_name = it.teacher.name,
            class_id = it.classRoom.id,
            class_name = it.classRoom.name,
            schedule_id = it.schedule.id,
            link = it.uri?.link?.firstOrNull().orEmpty(),
            link_student = it.uri_student?.link?.firstOrNull().orEmpty(),
            school_uuid = it.school?.uuid.orEmpty(),
            student_assignment_id = it.student_assignment_id ?: 0,
        )
    }
}

/** One file attached to a submitted answer (doc §5.5 "Jawaban terkumpul" — denormalized rows
 * instead of the legacy `homework_student_file` shape, same spirit as [HomeworkTable]). */
@Entity(tableName = "homework_answer_file", indices = [Index("homework_id")])
data class HomeworkAnswerFileTable(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val homework_id: Int = 0,
    val file_name: String = "",
    val file_path: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
) {
    companion object {
        fun from(homeworkId: Int, it: AssignmentFileItem): HomeworkAnswerFileTable = HomeworkAnswerFileTable(
            homework_id = homeworkId,
            file_name = it.file_name,
            file_path = it.file_path,
            file_format = it.file_format,
            file_type = it.file_type,
            file_size = it.file_size,
        )
    }
}

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

/** Tugas Terkumpul's per-student row (doc §6.5 `assignments[]`). Intentionally its own small shape
 * rather than reusing the heavier login-time `StudentItem` (which carries full-profile fields and
 * nested types — `student_class`/`UserResponseData` — that don't match this endpoint's simpler
 * `student{name,nisn,class,user.avatar}` shape). */
@Keep
@JsonClass(generateAdapter = true)
data class TugasStudentItem(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val `class`: String = "",
    val user: TugasStudentUser = TugasStudentUser(),
)

@Keep
@JsonClass(generateAdapter = true)
data class TugasStudentUser(@NullToEmptyString val avatar: String = "")

@Keep
@JsonClass(generateAdapter = true)
data class Assignment(
    val id: Int = 0,
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
    val student: TugasStudentItem = TugasStudentItem(),
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

/** One file in [AssignmentAnswerDetailData.files] — `getAssignmentAnswerDetail`'s shape (doc §5.6
 * last row), reused for both the teacher-attached "soal" file and a student's "jawaban" files. */
@Keep
@JsonClass(generateAdapter = true)
data class AssignmentFileItem(
    val file_name: String = "",
    val file_path: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
)

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentAnswerDetailResponse(val data: AssignmentAnswerDetailData = AssignmentAnswerDetailData())

@Keep
@JsonClass(generateAdapter = true)
data class AssignmentAnswerDetailData(val files: List<AssignmentFileItem> = emptyList())
