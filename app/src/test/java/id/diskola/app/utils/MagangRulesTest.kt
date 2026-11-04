package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.MagangAttendanceItem
import id.diskola.app.dataclass.ResponData.MagangAttendanceState
import id.diskola.app.dataclass.ResponData.MagangCompany
import id.diskola.app.dataclass.ResponData.MagangLeaveRef
import id.diskola.app.dataclass.ResponData.MagangLocation
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.dataclass.ResponData.MagangScheduleTime
import id.diskola.app.utils.MagangRules.CardAction
import id.diskola.app.utils.MagangRules.LeaveField
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagangRulesTest {

    private fun schedule(
        id: Long,
        date: String,
        state: MagangAttendanceState? = null,
        company: String = "PT. Testing Magang",
        start: String = "15:00:00",
        end: String = "19:15:00",
    ) = MagangScheduleItem(id, MagangCompany(name = company, address = "Jl. Uji 1"), MagangScheduleTime(date, start, end), state)

    private fun report(id: Long, checkIn: String, checkOut: String = "", type: String? = null, date: String = "", status: String = "") =
        MagangAttendanceItem(record_type = type, id = id, date = date, status = status, check_in_at = checkIn, check_out_at = checkOut)

    private val june4 = LocalDate.of(2026, 6, 4)

    // ---- dates ----

    @Test
    fun dateOf_readsTheDatePartOfADateOrADateTimeAndNothingElse() {
        assertEquals(june4, MagangRules.dateOf("2026-06-04"))
        assertEquals(june4, MagangRules.dateOf("2026-06-04 14:50:00"))
        assertEquals(june4, MagangRules.dateOf(" 2026-06-04T14:50:00Z "))
        assertNull(MagangRules.dateOf("14:50:00"))
        assertNull(MagangRules.dateOf(""))
        assertNull(MagangRules.dateOf(null))
        assertNull(MagangRules.dateOf("kemarin"))
    }

    @Test
    fun dateLabel_isIndonesianAndFallsBackToTheServersText() {
        assertEquals("04 Jun 2026", MagangRules.dateLabel("2026-06-04"))
        assertEquals("kemarin", MagangRules.dateLabel("kemarin"))
        assertEquals("", MagangRules.dateLabel(null))
    }

    @Test
    fun timeRange_dropsTheSeconds() {
        assertEquals("15:00 – 19:15", MagangRules.timeRange("15:00:00", "19:15:00"))
        assertEquals("08:00 – 17:00", MagangRules.timeRange("08:00", "17:00"))
    }

    @Test
    fun daysOf_coversTheWholeMonth() {
        val days = MagangRules.daysOf(YearMonth.of(2026, 2))
        assertEquals(28, days.size)
        assertEquals(LocalDate.of(2026, 2, 1), days.first())
        assertEquals(LocalDate.of(2026, 2, 28), days.last())
        assertEquals(31, MagangRules.daysOf(YearMonth.of(2026, 10)).size)
    }

    // ---- schedule ----

    @Test
    fun schedulesOn_matchesTheDayWhateverTheFormat() {
        val items = listOf(schedule(1, "2026-06-04"), schedule(2, "2026-06-05"), schedule(3, "2026-06-04 00:00:00"), schedule(4, ""))
        assertEquals(listOf(1L, 3L), MagangRules.schedulesOn(items, june4).map { it.id })
        assertEquals(emptyList<Long>(), MagangRules.schedulesOn(items, LocalDate.of(2026, 11, 4)).map { it.id })
    }

    @Test
    fun actions_offerWhatTheFlagsAllow() {
        assertEquals(listOf(CardAction.Masuk), MagangRules.actions(MagangAttendanceState(can_attend = true)))
        assertEquals(listOf(CardAction.LaporanHarian), MagangRules.actions(MagangAttendanceState(can_leave = true)))
        assertEquals(emptyList<CardAction>(), MagangRules.actions(MagangAttendanceState(can_attend = false, can_leave = null)))
        assertEquals(emptyList<CardAction>(), MagangRules.actions(null))
    }

    @Test
    fun actions_bothFlagsGiveBothButtonsReportFirst() {
        // legacy drew both in the same spot, one over the other
        assertEquals(
            listOf(CardAction.LaporanHarian, CardAction.Masuk),
            MagangRules.actions(MagangAttendanceState(can_attend = true, can_leave = true)),
        )
    }

    @Test
    fun statusText_isShownOnlyWhenThereIsNoButton() {
        assertEquals("Sudah presensi", MagangRules.statusText(MagangAttendanceState(status = " Sudah presensi ")))
        assertNull(MagangRules.statusText(MagangAttendanceState(status = "Sudah presensi", can_attend = true)))
        assertNull(MagangRules.statusText(MagangAttendanceState(status = "")))
        assertNull(MagangRules.statusText(null))
    }

    // ---- reports ----

    @Test
    fun reportsOf_dropsIzinRowsAndSortsNewestCheckInFirst() {
        val items = listOf(
            report(1, "2026-06-04 14:50:00"),
            report(2, "2026-06-05 08:02:00", type = "attendance"),
            report(3, "", type = MagangRules.RECORD_LEAVE, date = "2026-06-06", status = "sakit"),
        )
        assertEquals(listOf(2L, 1L), MagangRules.reportsOf(items).map { it.id })
    }

    @Test
    fun reportsIn_filtersByTheCheckInDayInclusive() {
        val reports = listOf(report(1, "2026-06-04 14:50:00"), report(2, "2026-06-10 09:00:00"), report(3, "09:00"))
        val all = MagangRules.reportsIn(reports, null, null)
        assertEquals(listOf(1L, 2L, 3L), all.map { it.id })
        assertEquals(listOf(1L), MagangRules.reportsIn(reports, june4, june4).map { it.id })
        assertEquals(listOf(1L, 2L), MagangRules.reportsIn(reports, june4, LocalDate.of(2026, 6, 10)).map { it.id })
        assertEquals(listOf(2L), MagangRules.reportsIn(reports, LocalDate.of(2026, 6, 5), null).map { it.id })
    }

    @Test
    fun longReports_andImageLinks() {
        assertFalse(MagangRules.isLongReport("x".repeat(121)))
        assertTrue(MagangRules.isLongReport("x".repeat(122)))
        assertEquals("https://a.id/f/foto.JPG?x=1", MagangRules.firstImageUrl("lihat https://a.id/f/foto.JPG?x=1 ya"))
        assertNull(MagangRules.firstImageUrl("dokumen https://a.id/f/surat.pdf"))
        val withFile = MagangAttendanceItem(file = "magang/laporan.png", daily_report = "https://b.id/x.jpg")
        assertEquals("magang/laporan.png", MagangRules.attachmentOf(withFile))
        assertEquals("https://b.id/x.jpg", MagangRules.attachmentOf(MagangAttendanceItem(daily_report = "https://b.id/x.jpg")))
        assertNull(MagangRules.attachmentOf(MagangAttendanceItem(daily_report = "tanpa lampiran")))
    }

    @Test
    fun reportLatitude_readsEitherSpelling() {
        assertEquals(-7.25, MagangRules.reportLatitude(MagangAttendanceItem(location = MagangLocation(latitide = "-7.25"))), 0.0)
        assertEquals(-7.3, MagangRules.reportLatitude(MagangAttendanceItem(location = MagangLocation(latitide = "0", latitude = "-7.3"))), 0.0)
        assertEquals(0.0, MagangRules.reportLatitude(MagangAttendanceItem()), 0.0)
    }

    // ---- writing a report ----

    @Test
    fun validateReport_followsTheLegacyOrder() {
        val enough = "a".repeat(100)
        assertEquals("Harap unggah gambar terlebih dahulu", MagangRules.validateReport(false, 0, ""))
        assertEquals("Foto lebih dari 5 MB", MagangRules.validateReport(true, MagangRules.REPORT_IMAGE_MAX_BYTES + 1, enough))
        assertEquals("Catatan minimal 100 karakter (saat ini 3 karakter)", MagangRules.validateReport(true, 10, "  abc  "))
        assertNull(MagangRules.validateReport(true, MagangRules.REPORT_IMAGE_MAX_BYTES, enough))
    }

    @Test
    fun reportButton_andCounter() {
        assertEquals("0 / min. 100 karakter", MagangRules.reportCounter(""))
        assertEquals("100 / min. 100 karakter", MagangRules.reportCounter("a".repeat(100)))
        assertFalse(MagangRules.reportCounterReached("a".repeat(99)))
        assertTrue(MagangRules.reportCounterReached("a".repeat(100)))
        assertFalse(MagangRules.canSubmitReport(false, "a".repeat(200)))
        assertFalse(MagangRules.canSubmitReport(true, " ".repeat(150) + "a"))
        assertTrue(MagangRules.canSubmitReport(true, "a".repeat(100)))
    }

    // ---- izin ----

    @Test
    fun leaves_fromBothSourcesMergeByIdAndTheAttendanceListWins() {
        val attendance = listOf(
            report(7, "", type = MagangRules.RECORD_LEAVE, date = "2026-11-03", status = "sakit"),
            report(9, "2026-06-04 14:50:00"),
        )
        val schedules = listOf(
            schedule(1, "2026-11-03", MagangAttendanceState(leave_request = MagangLeaveRef(id = 7, status = "izin", note = "dari jadwal"))),
            schedule(2, "2026-11-04", MagangAttendanceState(leave_request = MagangLeaveRef(id = 8, status = "izin", note = "izin hari ini"))),
            schedule(3, "2026-11-05", MagangAttendanceState(leave_request = MagangLeaveRef(id = 0))),
            schedule(4, "2026-11-06", MagangAttendanceState()),
        )
        val merged = MagangRules.mergeLeaves(MagangRules.leavesFromAttendance(attendance), MagangRules.leavesFromSchedules(schedules))
        assertEquals(listOf(8L, 7L), merged.map { it.id })
        assertEquals("sakit", merged.first { it.id == 7L }.type)
        assertEquals("PT. Testing Magang", merged.first { it.id == 8L }.companyName)
    }

    @Test
    fun leavesIn_filtersByMonthAndType() {
        val leaves = listOf(
            MagangRules.Leave(1, "2026-11-03", "sakit", "", "", "A", ""),
            MagangRules.Leave(2, "2026-11-20", "izin", "", "", "A", ""),
            MagangRules.Leave(3, "2026-10-31", "izin", "", "", "A", ""),
            MagangRules.Leave(4, "", "izin", "", "", "A", ""),
        )
        val november = YearMonth.of(2026, 11)
        assertEquals(listOf(1L, 2L), MagangRules.leavesIn(leaves, november, null).map { it.id })
        assertEquals(listOf(2L), MagangRules.leavesIn(leaves, november, MagangRules.TYPE_IZIN).map { it.id })
        assertEquals(listOf(1L), MagangRules.leavesIn(leaves, november, "SAKIT").map { it.id })
        assertEquals(listOf(3L), MagangRules.leavesIn(leaves, YearMonth.of(2026, 10), null).map { it.id })
    }

    @Test
    fun typeLabel_capitalisesAndNeverReturnsBlank() {
        assertEquals("Sakit", MagangRules.typeLabel("sakit"))
        assertEquals("Izin", MagangRules.typeLabel(" IZIN "))
        assertEquals("Pending", MagangRules.typeLabel("pending"))
        assertEquals("Pengajuan", MagangRules.typeLabel(""))
    }

    @Test
    fun leaveBanner_saysWhyTheFormIsClosed() {
        val today = LocalDate.of(2026, 11, 4)
        val filed = MagangRules.Leave(5, "2026-11-04", "sakit", "", "", "A", "")
        val ok = schedule(1, "2026-11-04", MagangAttendanceState(can_submit_leave_request = true))
        val no = schedule(2, "2026-11-04", MagangAttendanceState(can_submit_leave_request = false))
        assertEquals("Anda sudah mengajukan sakit hari ini.", MagangRules.leaveBanner(filed, listOf(ok)))
        assertEquals("Tidak ada jadwal magang yang dapat diajukan izin hari ini.", MagangRules.leaveBanner(null, emptyList()))
        assertNull(MagangRules.leaveBanner(null, listOf(ok)))
        assertEquals(listOf(1L), MagangRules.submittableToday(listOf(ok, no, schedule(3, "2026-11-05", MagangAttendanceState(can_submit_leave_request = true))), today).map { it.id })
        assertEquals(5L, MagangRules.todayLeave(listOf(filed, MagangRules.Leave(6, "2026-11-05", "izin", "", "", "A", "")), today)?.id)
    }

    @Test
    fun scheduleChoiceLabel_showsDayAndTime() {
        assertEquals("04 Jun 2026 • 15:00 – 19:15", MagangRules.scheduleChoiceLabel(schedule(1, "2026-06-04")))
    }

    @Test
    fun validateLeave_followsTheLegacyOrder() {
        val note = "Demam sejak semalam"
        val ok = 1_000L
        assertEquals(LeaveField.Schedule, MagangRules.validateLeave(0, true, "sakit", note, ok)?.field)
        assertEquals(LeaveField.File, MagangRules.validateLeave(1, false, "sakit", note, ok)?.field)
        assertEquals(LeaveField.Type, MagangRules.validateLeave(1, true, null, note, ok)?.field)
        assertEquals("Jenis pengajuan wajib dipilih", MagangRules.validateLeave(1, true, "cuti", note, ok)?.message)
        assertEquals("Keterangan minimal 10 karakter", MagangRules.validateLeave(1, true, "izin", "  pendek  ", ok)?.message)
        assertEquals("Keterangan maksimal 5000 karakter", MagangRules.validateLeave(1, true, "izin", "a".repeat(5001), ok)?.message)
        assertEquals("Ukuran file maksimal 2 MB", MagangRules.validateLeave(1, true, "izin", note, MagangRules.LEAVE_IMAGE_MAX_BYTES + 1)?.message)
        assertNull(MagangRules.validateLeave(1, true, "sakit", "a".repeat(5000), MagangRules.LEAVE_IMAGE_MAX_BYTES))
    }

    @Test
    fun leaveFile_mustBeAnImage() {
        assertNull(MagangRules.leaveFileError("image/png"))
        assertEquals("File harus berupa gambar", MagangRules.leaveFileError("application/pdf"))
        assertEquals("File harus berupa gambar", MagangRules.leaveFileError(null))
    }

    // ---- coordinates and errors ----

    @Test
    fun coordinates_unparsableReadAsZero() {
        assertEquals(-7.29731, MagangRules.coordinate(" -7.29731 "), 0.0)
        assertEquals(0.0, MagangRules.coordinate("abc"), 0.0)
        assertEquals(0.0, MagangRules.coordinate(null), 0.0)
        assertFalse(MagangRules.hasCoordinates(0.0, 0.0))
        assertTrue(MagangRules.hasCoordinates(-7.3, 0.0))
    }

    @Test
    fun errorMessage_prefersTheFieldReasons() {
        assertEquals("a\nb", MagangRules.errorMessage("Data tidak valid", listOf("a", " ", "b")))
        assertEquals("Data tidak valid", MagangRules.errorMessage("Data tidak valid", emptyList()))
        assertEquals("Terjadi kesalahan, silakan coba lagi", MagangRules.errorMessage("null", emptyList()))
        assertEquals("gagal", MagangRules.errorMessage(null, emptyList(), "gagal"))
    }
}
