package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.JurnalPlot
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** One line of "Jurnal" — a real class session, or a synthetic placeholder for an hour with none. */
data class JurnalRow(
    val key: String,
    val plotId: Int,
    val plotIndex: Int,
    /** Null for a synthetic ("Kosong") row. */
    val attendanceId: Int?,
    val subjectName: String,
    val teacherName: String,
    val className: String,
    val iconUrl: String,
    val start: String,
    val end: String,
    val status: String,
    val isPresent: Boolean,
    val lateAt: String?,
    val createdAt: String,
) {
    val isSynthetic: Boolean get() = attendanceId == null
    val timeLabel: String get() = "$start - $end"
}

/**
 * Jurnal KBM decisions that are pure logic (doc `07` §7–8), kept Android-free so they are unit-tested.
 * Sources: legacy `PresensiViewModel.kt:480-581` (rows + lock), `JurnalPage.kt` (buttons, dialog, QR) and
 * the two journal forms. Time gating uses the device clock like the legacy app does.
 */
object JurnalRules {

    const val EMPTY = "Kosong"
    const val NOT_STARTED = "Kelas belum dimulai"
    const val CAPTURE_REQUIRED_MESSAGE = "Foto suasana KBM wajib diambil"
    const val TEACHER_FORM_INCOMPLETE = "Data isian tidak lengkap"
    const val STUDENT_FORM_INCOMPLETE = "Mohon isi keterangan terlebih dahulu"
    const val NOT_SEQUENTIAL = "pilih secara urut"

    private val CLOCK = DateTimeFormatter.ofPattern("H:mm[:ss]")

    fun parseClock(raw: String): LocalTime? = try {
        LocalTime.parse(raw.trim(), CLOCK)
    } catch (e: Exception) {
        null
    }

    /** `HH:mm` from `HH:mm:ss`; anything unparsable is returned as sent. */
    fun shortClock(raw: String): String = parseClock(raw)?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: raw

    /** `start <= now < end` — an unparsable bound means "cannot say", i.e. false. */
    fun withinPlot(start: String, end: String, now: LocalTime): Boolean {
        val s = parseClock(start) ?: return false
        val e = parseClock(end) ?: return false
        return !now.isBefore(s) && now.isBefore(e)
    }

    private fun beforePlot(start: String, now: LocalTime): Boolean {
        val s = parseClock(start) ?: return false
        return now.isBefore(s)
    }

    // ---- Rows ----

    /**
     * Flattens the schedule into rows. An hour with no class session gets a synthetic row
     * ("Jam Pelajaran - n" / teacher "Kosong"). Rows whose hour has not begun read "Kelas belum dimulai".
     * For students a second pass applies the lock: from the first hour that has nothing real yet
     * ("" or "Kosong" on all its rows) every later hour is locked. Legacy ran that pass over the flattened
     * list, so it misaligned as soon as one hour had more than one class; this runs per hour.
     */
    fun buildRows(plots: List<JurnalPlot>, isStudent: Boolean, now: LocalTime): List<JurnalRow> {
        val perPlot = plots.mapIndexed { plotIndex, plot ->
            val attendances = plot.school_attendances.orEmpty()
            if (attendances.isEmpty()) {
                listOf(
                    JurnalRow(
                        key = "plot-${plot.id}-$plotIndex",
                        plotId = plot.id,
                        plotIndex = plotIndex,
                        attendanceId = null,
                        subjectName = "Jam Pelajaran - ${plotIndex + 1}",
                        teacherName = "Kosong",
                        className = "",
                        iconUrl = "",
                        start = shortClock(plot.start_at),
                        end = shortClock(plot.end_at),
                        status = EMPTY,
                        isPresent = false,
                        lateAt = null,
                        createdAt = "",
                    ),
                )
            } else {
                attendances.map { a ->
                    val start = a.plot_start_at.ifBlank { plot.start_at }
                    val end = a.plot_end_at.ifBlank { plot.end_at }
                    JurnalRow(
                        key = "att-${a.attendanceId}",
                        plotId = plot.id,
                        plotIndex = plotIndex,
                        attendanceId = a.attendanceId,
                        subjectName = a.subject_name,
                        teacherName = a.teacher_name,
                        className = a.class_name,
                        iconUrl = a.subject_icon_image,
                        start = shortClock(start),
                        end = shortClock(end),
                        status = if (beforePlot(start, now) && !withinPlot(start, end, now)) NOT_STARTED else a.status,
                        isPresent = a.is_present == true,
                        lateAt = a.late_at,
                        createdAt = a.created_at,
                    )
                }
            }
        }
        if (!isStudent) return perPlot.flatten()

        val blank = setOf("", EMPTY)
        val firstBlank = perPlot.indexOfFirst { rows -> rows.all { it.status in blank } }
        return perPlot.mapIndexed { index, rows ->
            when {
                firstBlank in 0 until index -> rows.map { it.copy(status = NOT_STARTED) }
                else -> rows
            }
        }.flatten()
    }

    /** The class session the Hub's "Kelas Berlangsung" card shows: the first real row whose hour is running. */
    fun currentRow(rows: List<JurnalRow>, now: LocalTime): JurnalRow? =
        rows.firstOrNull { !it.isSynthetic && withinPlot(it.start, it.end, now) }

    // ---- Buttons ----

    enum class StudentAction { NONE, HADIRI, ISI_JURNAL }

    /** [statusText] null = hidden; [presentLabel] shows "Anda Hadir Dikelas Ini". */
    data class StudentUi(val statusText: String?, val action: StudentAction, val presentLabel: Boolean)

    fun studentUi(row: JurnalRow): StudentUi = when {
        row.status == EMPTY -> StudentUi(null, StudentAction.ISI_JURNAL, row.isPresent)
        row.status.isNotEmpty() && row.isPresent -> StudentUi(null, StudentAction.NONE, true)
        // legacy never offered a button here although its comment says it should
        row.status.isNotEmpty() -> StudentUi(row.status, StudentAction.NONE, false)
        else -> StudentUi(null, StudentAction.HADIRI, false)
    }

    enum class TeacherAction { NONE, ISI_JURNAL, DETAIL, MULAI_KELAS }

    /** "Mulai kelas" is hidden before the hour begins (legacy showed it, and the server would refuse). */
    fun teacherAction(row: JurnalRow): TeacherAction = when {
        row.status == EMPTY -> TeacherAction.ISI_JURNAL
        !row.lateAt.isNullOrEmpty() -> TeacherAction.DETAIL
        row.lateAt == null && row.status != NOT_STARTED -> TeacherAction.MULAI_KELAS
        else -> TeacherAction.NONE
    }

    /** The "Pilih Metode Presensi" dialog opens only for a row without status. */
    data class MethodOptions(val manual: Boolean, val qr: Boolean)

    fun methodOptions(row: JurnalRow, now: LocalTime): MethodOptions {
        val noStatus = row.status.isEmpty()
        return MethodOptions(manual = noStatus, qr = noStatus && !row.isPresent && withinPlot(row.start, row.end, now))
    }

    // ---- QR ----

    data class QrPayload(val timePlotId: Int, val subjectId: Int, val classId: Int, val teacherId: Int, val scheduleId: Int)

    /** `a_b_<plot>_c_<subject>_d_<class>_e_<teacher>_f_<schedule>_…` — at least 12 parts, ids at 2/4/6/8/10. */
    fun parseQr(raw: String): QrPayload? {
        val parts = raw.trim().split("_")
        if (parts.size < 12) return null
        fun id(index: Int) = parts[index].trim().toIntOrNull()
        return QrPayload(
            timePlotId = id(2) ?: return null,
            subjectId = id(4) ?: return null,
            classId = id(6) ?: return null,
            teacherId = id(8) ?: return null,
            scheduleId = id(10) ?: return null,
        )
    }

    // ---- Forms ----

    /** Teacher form: photo (when the scope requires it) first, then every field. */
    fun validateTeacherForm(
        objective: String,
        classId: Int?,
        subjectId: Int?,
        plotIds: List<Int>,
        captureRequired: Boolean,
        hasPhoto: Boolean,
    ): String? = when {
        captureRequired && !hasPhoto -> CAPTURE_REQUIRED_MESSAGE
        objective.isBlank() || classId == null || subjectId == null || plotIds.isEmpty() -> TEACHER_FORM_INCOMPLETE
        else -> null
    }

    fun validateStudentForm(
        teacherUuid: String?,
        subjectId: Int?,
        status: String?,
        captureRequired: Boolean,
        hasPhoto: Boolean,
    ): String? = when {
        captureRequired && !hasPhoto -> CAPTURE_REQUIRED_MESSAGE
        teacherUuid.isNullOrBlank() || subjectId == null || status.isNullOrBlank() -> STUDENT_FORM_INCOMPLETE
        else -> null
    }

    /**
     * Hour chips must be consecutive. Tapping the one after the last selection extends it, tapping the last
     * selection removes it; anything else returns null (legacy cleared the whole selection in that case).
     */
    fun toggleChip(selected: List<Int>, tapped: Int): List<Int>? = when {
        selected.isEmpty() -> listOf(tapped)
        tapped == selected.last() + 1 -> selected + tapped
        tapped == selected.last() -> selected.dropLast(1)
        else -> null
    }

    // ---- Errors ----

    /** Doc `07` §14.3 (`journalErrorMessage`). [scopeAssigned] = the teacher is limited to their own schedule. */
    fun journalErrorMessage(code: Int?, message: String?, scopeAssigned: Boolean, fallback: String): String {
        val text = message?.takeIf { it.isNotBlank() && it != "null" }
        return when (code) {
            307 -> text ?: "Jurnal telah dibuat untuk jadwal ini"
            422 -> if (text != null && text.contains("validasi", ignoreCase = true) && scopeAssigned) {
                "Kombinasi kelas/mapel/jam tidak ada di jadwal Anda"
            } else {
                text ?: fallback
            }
            400 -> text ?: fallback
            404 -> text ?: "Jurnal atau jadwal tidak ditemukan"
            else -> text ?: fallback
        }
    }

    /**
     * The 307 body names the journal that already exists: `attendance_id` plus a CSV under the misspelled
     * `subject_schedule_id_squence` (or `_sequence`). Numbers arrive as Double. Result: attendance id first.
     */
    fun existingJournalIds(data: Map<String, Any>): List<Int> {
        val attendanceId = (data["attendance_id"] as? Number)?.toInt()
        val csv = (data["subject_schedule_id_squence"] ?: data["subject_schedule_id_sequence"]) as? String
        val others = csv.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }
        return (listOfNotNull(attendanceId) + others).distinct()
    }

    // ---- Detail ----

    const val SESSION_DONE = "Terlaksana"
    const val SESSION_ASSIGNMENT = "Penugasan"
    const val SESSION_NOT_DONE = "Tidak Terlaksana"
    val STUDENT_STATUSES = listOf("hadir", "izin", "sakit", "alpha")

    /** Unknown or missing student statuses read as `alpha` (legacy normalised at bind time). */
    fun normalizeStudentStatus(raw: String?): String = raw.orEmpty().trim().lowercase().takeIf { it in STUDENT_STATUSES } ?: "alpha"

    fun statusSourceLabel(source: String?): String? = when (source) {
        "self" -> "Absen mandiri"
        "leave_request" -> "Dari izin"
        "teacher" -> "Ditetapkan guru"
        else -> null
    }

    /** Editing needs a started class and a real schedule id (legacy `canEdit()`). */
    fun canEditDetail(isTeacher: Boolean, isStarted: Boolean, scheduleId: Int): Boolean = isTeacher && isStarted && scheduleId > 0
}
