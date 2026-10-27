package id.diskola.app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AssignmentInput(
    val restricted: Boolean? = null,
    val classId: Int = 0,
    val subjectId: Int = 0,
    val day: String = "",
    val scheduleId: Int = 0,
    val title: String = "",
    val description: String = "",
    val dueAt: Long? = null,
    val link: String = "",
    val needDownload: Boolean = false,
    val needUpload: Boolean = false,
    val uploadFile: Boolean = false,
    val hasFile: Boolean = false,
    val fileSizeBytes: Long? = null,
)

object AssignmentPayload {

    const val MAX_FILE_BYTES = 12L * 1024 * 1024

    fun fields(input: AssignmentInput, timeZone: TimeZone = TimeZone.getDefault()): Map<String, String> {
        val fields = LinkedHashMap<String, String>()
        fields["title"] = input.title
        fields["description"] = input.description
        if (input.restricted == false) {
            fields["school_class_id"] = input.classId.toString()
            fields["school_subject_id"] = input.subjectId.toString()
        } else {
            fields["school_subject_schedule_id"] = input.scheduleId.toString()
        }
        fields["end_at"] = input.dueAt?.let { AssignmentDates.formatWire(it, timeZone) }.orEmpty()
        fields["checked"] = "1"
        fields["downloaded"] = if (input.needDownload) "1" else "0"
        fields["uploaded"] = if (input.needUpload) "1" else "0"
        input.link.trim().takeIf { it.isNotEmpty() }?.let { fields["link"] = it }
        return fields
    }

    /** Key = nama field backend (plus `scope` dan `day` yang tidak punya padanan di payload). */
    fun validate(input: AssignmentInput): Map<String, String> {
        val errors = LinkedHashMap<String, String>()
        if (input.restricted == null) {
            errors["scope"] = "Data akses guru belum siap, silakan coba lagi"
            return errors
        }
        if (input.classId <= 0) errors["school_class_id"] = "Kelas wajib dipilih"
        if (input.restricted) {
            if (input.day.isEmpty()) errors["day"] = "Hari Mata Pelajaran wajib dipilih"
            if (input.scheduleId <= 0) errors["school_subject_schedule_id"] = "Mata Pelajaran wajib dipilih"
        } else {
            if (input.subjectId <= 0) errors["school_subject_id"] = "Mata Pelajaran wajib dipilih"
        }
        if (input.title.isBlank()) errors["title"] = "Judul tugas wajib diisi"
        if (input.description.isBlank()) errors["description"] = "Deskripsi tugas wajib diisi"
        if (input.dueAt == null) errors["end_at"] = "Waktu berakhir wajib dipilih"
        if (input.uploadFile && !input.hasFile && input.link.isBlank())
            errors["file"] = "Lampirkan file atau tempel link"
        if (input.fileSizeBytes != null && input.fileSizeBytes > MAX_FILE_BYTES)
            errors["file"] = "Ukuran file maksimal 12 MB"
        return errors
    }
}

object AssignmentDates {

    private val months: Map<String, Int> = HashMap<String, Int>().apply {
        listOf(
            listOf("januari", "jan", "january"),
            listOf("februari", "feb", "february"),
            listOf("maret", "mar", "march"),
            listOf("april", "apr"),
            listOf("mei", "may"),
            listOf("juni", "jun", "june"),
            listOf("juli", "jul", "july"),
            listOf("agustus", "agu", "agt", "ags", "aug", "august"),
            listOf("september", "sep", "sept"),
            listOf("oktober", "okt", "oct", "october"),
            listOf("november", "nov"),
            listOf("desember", "des", "dec", "december"),
        ).forEachIndexed { index, names -> names.forEach { put(it, index) } }
    }

    private val labelPattern =
        Regex("""^(\d{1,2})\s+([\p{L}.]+)\s+(\d{4})(?:\s*,\s*|\s+)(\d{1,2})[:.](\d{2})(?::(\d{2}))?$""")

    fun formatWire(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { this.timeZone = timeZone }
            .format(Date(millis))

    fun formatDisplay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale("id"))
            .apply { this.timeZone = timeZone }
            .format(Date(millis))

    /** Membaca `end_at_label` (`d M Y H:i`); null bila format/tanggalnya tidak valid. */
    fun parseEndAtLabel(label: String?, timeZone: TimeZone = TimeZone.getDefault()): Long? {
        val match = labelPattern.matchEntire(label.orEmpty().trim()) ?: return null
        val (day, monthName, year, hour, minute, second) = match.destructured
        val month = months[monthName.trimEnd('.').lowercase(Locale.ROOT)] ?: return null
        return try {
            Calendar.getInstance(timeZone, Locale.US).run {
                isLenient = false
                clear()
                set(
                    year.toInt(), month, day.toInt(),
                    hour.toInt(), minute.toInt(), second.ifEmpty { "0" }.toInt()
                )
                timeInMillis
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

/**
 * Who gets the teacher side of Tugas (create, edit, delete, score). The backend answers the teacher endpoints with 400 for an account that has no
 * teacher data (spec "Form Tugas Guru", §2 and §6), so such an account is stopped before it fills in a form that can only fail. What the account
 * is called ("Janitor", "Guru", …) is not looked at: `rule.is_teacher` and the teacher record come from the backend, which also decides what
 * a staff member with a teacher record may do (the scope's `restricted` flag).
 */
object AssignmentAccess {
    const val NO_TEACHER_DATA_TITLE = "Tugas guru tidak tersedia"
    const val NO_TEACHER_DATA_BODY = "Akun ini tidak memiliki data guru, jadi tidak dapat membuat atau menilai tugas. Hubungi admin sekolah bila ini keliru."
    const val NO_TEACHER_DATA_TOAST = "Akun ini tidak memiliki data guru, tugas tidak dapat dibuat"

    /** A real teacher record has a positive id; the session stores 0 when `check-account` returned none. */
    fun hasTeacherData(teacherId: Int): Boolean = teacherId > 0

    fun canManage(isTeacher: Boolean, isStudent: Boolean, teacherId: Int): Boolean = isTeacher && !isStudent && hasTeacherData(teacherId)
}

/** The query of the teacher's own task list (`teachers/backlog`). */
object AssignmentFilter {

    /**
     * `filter[0]` is always the teacher. Class and subject are filters only when they are chosen (> 0): "Semua" is **not sent**, and the
     * indices stay in sequence (`filter[1]` "and", `filter[2][..]`, then the next pair) — the old way sent a literal `0` and fixed indices
     * (decision 2026-10-22: follow the legacy app, which checked that a request without the filter returns 200 and data).
     */
    fun teacherListQuery(teacherId: Int, classId: Int?, subjectId: Int?, take: Int, skip: Int): Map<String, String> {
        val query = LinkedHashMap<String, String>()
        query["take"] = take.toString()
        query["skip"] = skip.toString()
        query["filter[0][0]"] = "teacher_id"
        query["filter[0][1]"] = "="
        query["filter[0][2]"] = teacherId.toString()
        var next = 1
        listOf("school_classes_id" to classId, "school_subject_id" to subjectId).forEach { (field, value) ->
            if (value != null && value > 0) {
                query["filter[$next]"] = "and"
                query["filter[${next + 1}][0]"] = field
                query["filter[${next + 1}][1]"] = "="
                query["filter[${next + 1}][2]"] = value.toString()
                next += 2
            }
        }
        return query
    }
}
