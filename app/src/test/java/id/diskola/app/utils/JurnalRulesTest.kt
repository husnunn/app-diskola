package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.JurnalAttendance
import id.diskola.app.dataclass.ResponData.JurnalPlot
import id.diskola.app.utils.JurnalRules.MethodOptions
import id.diskola.app.utils.JurnalRules.StudentAction
import id.diskola.app.utils.JurnalRules.TeacherAction
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JurnalRulesTest {

    private fun att(id: Int, status: String = "", present: Boolean = false, late: String? = null, start: String = "", end: String = "") =
        JurnalAttendance(attendanceId = id, plot_start_at = start, plot_end_at = end, status = status, is_present = present, late_at = late, subject_name = "Mapel $id")

    private fun plot(id: Int, start: String, end: String, vararg attendances: JurnalAttendance) =
        JurnalPlot(id = id, start_at = start, end_at = end, school_attendances = attendances.toList())

    private fun at(h: Int, m: Int = 0) = LocalTime.of(h, m)

    // ---- time ----

    @Test
    fun withinPlot_startInclusiveEndExclusive() {
        assertTrue(JurnalRules.withinPlot("07:00:00", "08:00:00", at(7)))
        assertTrue(JurnalRules.withinPlot("07:00", "08:00", at(7, 59)))
        assertFalse(JurnalRules.withinPlot("07:00", "08:00", at(8)))
        assertFalse(JurnalRules.withinPlot("07:00", "08:00", at(6, 59)))
        assertFalse(JurnalRules.withinPlot("", "08:00", at(7, 30)))
    }

    @Test
    fun shortClock_dropsSeconds() {
        assertEquals("07:00", JurnalRules.shortClock("07:00:00"))
        assertEquals("kemarin", JurnalRules.shortClock("kemarin"))
    }

    // ---- rows ----

    @Test
    fun emptyHourBecomesSyntheticKosongRow() {
        val rows = JurnalRules.buildRows(listOf(plot(1, "07:00:00", "08:00:00"), plot(2, "08:00:00", "09:00:00")), isStudent = false, now = at(7, 30))
        assertEquals(listOf("Jam Pelajaran - 1", "Jam Pelajaran - 2"), rows.map { it.subjectName })
        assertTrue(rows.all { it.isSynthetic && it.teacherName == "Kosong" && it.status == "Kosong" })
    }

    @Test
    fun rowBeforeItsHourReadsKelasBelumDimulai() {
        val rows = JurnalRules.buildRows(listOf(plot(1, "09:00:00", "10:00:00", att(10, status = "", start = "09:00:00", end = "10:00:00"))), false, at(7))
        assertEquals("Kelas belum dimulai", rows.single().status)
    }

    @Test
    fun runningHourKeepsServerStatus() {
        val rows = JurnalRules.buildRows(listOf(plot(1, "07:00:00", "08:00:00", att(10, status = "Terlaksana", start = "07:00:00", end = "08:00:00"))), false, at(7, 30))
        assertEquals("Terlaksana", rows.single().status)
    }

    @Test
    fun student_lockStartsAfterFirstBlankHour() {
        val plots = listOf(
            plot(1, "07:00:00", "08:00:00", att(1, status = "Terlaksana", start = "07:00:00", end = "08:00:00")),
            plot(2, "08:00:00", "09:00:00"), // nothing real here
            plot(3, "09:00:00", "10:00:00", att(3, status = "", start = "09:00:00", end = "10:00:00")),
            plot(4, "10:00:00", "11:00:00", att(4, status = "Terlaksana", start = "10:00:00", end = "11:00:00")),
        )
        val rows = JurnalRules.buildRows(plots, isStudent = true, now = at(12))
        assertEquals(listOf("Terlaksana", "Kosong", "Kelas belum dimulai", "Kelas belum dimulai"), rows.map { it.status })
    }

    @Test
    fun student_lockIsPerHourEvenWhenAnEarlierHourHasTwoClasses() {
        val plots = listOf(
            plot(
                1, "07:00:00", "08:00:00",
                att(1, status = "Terlaksana", start = "07:00:00", end = "08:00:00"),
                att(2, status = "Terlaksana", start = "07:00:00", end = "08:00:00"),
            ),
            plot(2, "08:00:00", "09:00:00", att(3, status = "", start = "08:00:00", end = "09:00:00")),
        )
        // Legacy indexed the flattened list, so row 3 compared itself with row 2; per hour both hours are open.
        val rows = JurnalRules.buildRows(plots, isStudent = true, now = at(10))
        assertEquals(listOf("Terlaksana", "Terlaksana", ""), rows.map { it.status })
    }

    @Test
    fun teacher_hasNoLock() {
        val plots = listOf(plot(1, "07:00:00", "08:00:00"), plot(2, "08:00:00", "09:00:00", att(2, status = "Terlaksana", start = "08:00:00", end = "09:00:00")))
        val rows = JurnalRules.buildRows(plots, isStudent = false, now = at(10))
        assertEquals(listOf("Kosong", "Terlaksana"), rows.map { it.status })
    }

    @Test
    fun currentRow_isFirstRealRowInsideItsHour() {
        val rows = JurnalRules.buildRows(
            listOf(plot(1, "07:00:00", "08:00:00"), plot(2, "08:00:00", "09:00:00", att(7, start = "08:00:00", end = "09:00:00"))),
            isStudent = false,
            now = at(8, 15),
        )
        assertEquals(7, JurnalRules.currentRow(rows, at(8, 15))?.attendanceId)
        assertNull(JurnalRules.currentRow(rows, at(7, 15)))
    }

    // ---- buttons ----

    private fun row(status: String, present: Boolean = false, late: String? = null) = JurnalRows.make(status, present, late)

    @Test
    fun studentButtons() {
        assertEquals(StudentAction.HADIRI, JurnalRules.studentUi(row("")).action)
        assertEquals(StudentAction.ISI_JURNAL, JurnalRules.studentUi(row("Kosong")).action)
        assertTrue(JurnalRules.studentUi(row("Terlaksana", present = true)).presentLabel)
        assertEquals("Terlaksana", JurnalRules.studentUi(row("Terlaksana")).statusText)
        assertEquals(StudentAction.NONE, JurnalRules.studentUi(row("Kelas belum dimulai")).action)
    }

    @Test
    fun teacherButtons() {
        assertEquals(TeacherAction.ISI_JURNAL, JurnalRules.teacherAction(row("Kosong")))
        assertEquals(TeacherAction.DETAIL, JurnalRules.teacherAction(row("Terlaksana", late = "07:10")))
        assertEquals(TeacherAction.MULAI_KELAS, JurnalRules.teacherAction(row("", late = null)))
        assertEquals(TeacherAction.NONE, JurnalRules.teacherAction(row("", late = "")))
        assertEquals(TeacherAction.NONE, JurnalRules.teacherAction(row("Kelas belum dimulai", late = null)))
    }

    @Test
    fun methodDialog_qrOnlyInsideTheHourAndNotYetPresent() {
        val inside = JurnalRows.make("", false, null, start = "07:00", end = "08:00")
        assertEquals(MethodOptions(manual = true, qr = true), JurnalRules.methodOptions(inside, at(7, 30)))
        assertEquals(MethodOptions(manual = true, qr = false), JurnalRules.methodOptions(inside, at(9)))
        assertEquals(MethodOptions(manual = true, qr = false), JurnalRules.methodOptions(inside.copy(isPresent = true), at(7, 30)))
    }

    // ---- QR ----

    @Test
    fun parseQr_readsIdsAtFixedIndexes() {
        val raw = "diskola_plot_11_subject_22_class_33_teacher_44_schedule_55_x_y"
        assertEquals(JurnalRules.QrPayload(11, 22, 33, 44, 55), JurnalRules.parseQr(raw))
    }

    @Test
    fun parseQr_rejectsShortOrNonNumeric() {
        assertNull(JurnalRules.parseQr("a_b_1_c_2"))
        assertNull(JurnalRules.parseQr("a_b_x_c_2_d_3_e_4_f_5_g_h"))
    }

    // ---- forms ----

    @Test
    fun teacherForm_photoComesFirstThenFields() {
        assertEquals("Foto suasana KBM wajib diambil", JurnalRules.validateTeacherForm("tujuan", 1, 1, listOf(1), captureRequired = true, hasPhoto = false))
        assertEquals("Data isian tidak lengkap", JurnalRules.validateTeacherForm("  ", 1, 1, listOf(1), false, false))
        assertEquals("Data isian tidak lengkap", JurnalRules.validateTeacherForm("tujuan", null, 1, listOf(1), false, false))
        assertEquals("Data isian tidak lengkap", JurnalRules.validateTeacherForm("tujuan", 1, 1, emptyList(), false, false))
        assertNull(JurnalRules.validateTeacherForm("tujuan", 1, 1, listOf(1), true, true))
    }

    @Test
    fun studentForm_rules() {
        assertEquals("Foto suasana KBM wajib diambil", JurnalRules.validateStudentForm("uuid", 1, "Terlaksana", true, false))
        assertEquals("Mohon isi keterangan terlebih dahulu", JurnalRules.validateStudentForm(null, 1, "Terlaksana", false, false))
        assertNull(JurnalRules.validateStudentForm("uuid", 1, "Terlaksana", false, false))
    }

    @Test
    fun chips_mustBeConsecutive() {
        assertEquals(listOf(2), JurnalRules.toggleChip(emptyList(), 2))
        assertEquals(listOf(2, 3), JurnalRules.toggleChip(listOf(2), 3))
        assertEquals(listOf(2), JurnalRules.toggleChip(listOf(2, 3), 3))
        assertNull(JurnalRules.toggleChip(listOf(2), 5))
        assertNull(JurnalRules.toggleChip(listOf(2, 3), 2))
    }

    // ---- errors ----

    @Test
    fun journalErrors() {
        assertEquals("Jurnal telah dibuat untuk jadwal ini", JurnalRules.journalErrorMessage(307, null, false, "x"))
        assertEquals("Kombinasi kelas/mapel/jam tidak ada di jadwal Anda", JurnalRules.journalErrorMessage(422, "Data tidak lolos validasi", true, "x"))
        assertEquals("Data tidak lolos validasi", JurnalRules.journalErrorMessage(422, "Data tidak lolos validasi", false, "x"))
        assertEquals("Jurnal atau jadwal tidak ditemukan", JurnalRules.journalErrorMessage(404, null, false, "x"))
        assertEquals("gagal", JurnalRules.journalErrorMessage(500, "null", false, "gagal"))
    }

    @Test
    fun existingJournalIds_readsMisspelledKeyAndDoubles() {
        val ids = JurnalRules.existingJournalIds(mapOf("attendance_id" to 9.0, "subject_schedule_id_squence" to "9, 12,13"))
        assertEquals(listOf(9, 12, 13), ids)
        assertEquals(listOf(5), JurnalRules.existingJournalIds(mapOf("attendance_id" to 5.0)))
        assertEquals(emptyList<Int>(), JurnalRules.existingJournalIds(emptyMap()))
    }

    // ---- detail ----

    @Test
    fun detailRules() {
        assertEquals("alpha", JurnalRules.normalizeStudentStatus(null))
        assertEquals("alpha", JurnalRules.normalizeStudentStatus("terlambat"))
        assertEquals("izin", JurnalRules.normalizeStudentStatus(" Izin "))
        assertEquals("Dari izin", JurnalRules.statusSourceLabel("leave_request"))
        assertNull(JurnalRules.statusSourceLabel("lainnya"))
        assertTrue(JurnalRules.canEditDetail(true, true, 5))
        assertFalse(JurnalRules.canEditDetail(true, false, 5))
        assertFalse(JurnalRules.canEditDetail(false, true, 5))
        assertFalse(JurnalRules.canEditDetail(true, true, 0))
    }
}

private object JurnalRows {
    fun make(status: String, present: Boolean, late: String?, start: String = "07:00", end: String = "08:00") = JurnalRow(
        key = "k", plotId = 1, plotIndex = 0, attendanceId = 1, subjectName = "Mapel", teacherName = "Guru", className = "X",
        iconUrl = "", start = start, end = end, status = status, isPresent = present, lateAt = late, createdAt = "",
    )
}
