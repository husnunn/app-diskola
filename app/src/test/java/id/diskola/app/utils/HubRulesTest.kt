package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.CurrentSubjectData
import id.diskola.app.dataclass.ResponData.CurrentUserResponse
import id.diskola.app.dataclass.ResponData.PresensiCheckData
import id.diskola.app.dataclass.ResponData.PresensiSchedule
import id.diskola.app.dataclass.ResponData.TodaySettingData
import id.diskola.app.dataclass.ResponData.TodaySettingEvent
import id.diskola.app.dataclass.ResponData.UserProgressData
import id.diskola.app.utils.HubRules.BannerState
import id.diskola.app.utils.HubRules.ClassAction
import id.diskola.app.utils.HubRules.VerifyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HubRulesTest {

    private fun progress(status: String, message: String = "") = UserProgressData(status = status, message = message)

    // ---- verify state ----

    @Test
    fun `approved always shows the content`() {
        assertEquals(VerifyState.ShowContent, HubRules.verifyState(progress("approved"), isStudent = false, isHavingClass = false))
        assertEquals(VerifyState.ShowContent, HubRules.verifyState(progress("APPROVED"), isStudent = true, isHavingClass = false))
    }

    @Test
    fun `clear status without a class asks to verify, students are told to contact the admin`() {
        assertEquals(VerifyState.VerifyAvailable, HubRules.verifyState(null, isStudent = false, isHavingClass = false))
        assertEquals(VerifyState.VerifyAvailable, HubRules.verifyState(progress(""), isStudent = false, isHavingClass = false))
        assertEquals(VerifyState.StudentNoClass, HubRules.verifyState(progress(""), isStudent = true, isHavingClass = false))
    }

    @Test
    fun `clear status with a class shows the content for students and teachers`() {
        assertEquals(VerifyState.ShowContent, HubRules.verifyState(null, isStudent = true, isHavingClass = true))
        assertEquals(VerifyState.ShowContent, HubRules.verifyState(null, isStudent = false, isHavingClass = true))
    }

    @Test
    fun `in review and rejected block the button with the server message`() {
        assertEquals(
            VerifyState.VerifyBlocked("Sedang ditinjau admin", rejected = false),
            HubRules.verifyState(progress("in_review", "Sedang ditinjau admin"), isStudent = false, isHavingClass = false),
        )
        assertEquals(
            VerifyState.VerifyBlocked("Data tidak cocok", rejected = true),
            HubRules.verifyState(progress("rejected", "Data tidak cocok"), isStudent = true, isHavingClass = false),
        )
    }

    @Test
    fun `a blocked state without a message falls back to the button label`() {
        assertEquals(
            VerifyState.VerifyBlocked(HubRules.VERIFY_LABEL, rejected = false),
            HubRules.verifyState(progress("in_review"), isStudent = false, isHavingClass = false),
        )
    }

    @Test
    fun `in review with a class does not block`() {
        assertEquals(VerifyState.ShowContent, HubRules.verifyState(progress("in_review", "x"), isStudent = true, isHavingClass = true))
    }

    // ---- class card ----

    private fun subject(id: Int? = 7, attendanceId: Int? = null, status: String? = null, name: String = "Matematika", start: String = "07:00", end: String = "08:00") =
        CurrentSubjectData(id = id, attendance_id = attendanceId, status = status, subject_name = name, plot_start_at = start, plot_end_at = end)

    @Test
    fun `a running class without attendance offers Isi Jurnal`() {
        assertEquals(ClassAction.ISI_JURNAL, HubRules.classAction(subject(attendanceId = null), isStudent = true, isHavingClass = true))
    }

    @Test
    fun `attendance without a status offers Check In, with a status nothing`() {
        assertEquals(ClassAction.CHECK_IN, HubRules.classAction(subject(attendanceId = 5, status = null), true, true))
        assertEquals(ClassAction.CHECK_IN, HubRules.classAction(subject(attendanceId = 5, status = ""), true, true))
        assertEquals(ClassAction.NONE, HubRules.classAction(subject(attendanceId = 5, status = "Terlaksana"), true, true))
    }

    @Test
    fun `no class, no id, or the wrong role means no button`() {
        assertEquals(ClassAction.NONE, HubRules.classAction(null, true, true))
        assertEquals(ClassAction.NONE, HubRules.classAction(subject(id = 0), true, true))
        assertEquals(ClassAction.NONE, HubRules.classAction(subject(id = null), true, true))
        assertEquals(ClassAction.NONE, HubRules.classAction(subject(), isStudent = false, isHavingClass = true))
        assertEquals(ClassAction.NONE, HubRules.classAction(subject(), isStudent = true, isHavingClass = false))
    }

    @Test
    fun `class name and time follow the legacy card`() {
        assertEquals("Matematika", HubRules.className(subject()))
        assertEquals(HubRules.NO_SCHEDULE, HubRules.className(subject(name = "")))
        assertEquals(HubRules.NO_SCHEDULE, HubRules.className(null))
        assertEquals("07:00 - 08:00", HubRules.classTime(subject()))
        assertNull(HubRules.classTime(subject(start = "")))
        assertNull(HubRules.classTime(null))
    }

    // ---- header ----

    @Test
    fun `class line is the e-mail for teachers and the class for everyone else`() {
        assertEquals("guru@sekolah.id", HubRules.classLine(true, "guru@sekolah.id", "ignored"))
        assertEquals("XII RPL 1", HubRules.classLine(false, "x@y.z", "XII RPL 1"))
        assertEquals(HubRules.NO_CLASS_YET, HubRules.classLine(false, "x@y.z", ""))
    }

    @Test
    fun `bell badge hides at zero and caps at 99+`() {
        assertNull(HubRules.bellBadge(0))
        assertNull(HubRules.bellBadge(-3))
        assertEquals("1", HubRules.bellBadge(1))
        assertEquals("98", HubRules.bellBadge(98))
        assertEquals("99+", HubRules.bellBadge(99))
        assertEquals("99+", HubRules.bellBadge(1000))
    }

    // ---- session guard ----

    @Test
    fun `device mismatch needs enforcement and a different device`() {
        assertTrue(HubRules.deviceMismatch(CurrentUserResponse(device_id = "A", single_device_login_enabled = true), "B"))
        assertFalse(HubRules.deviceMismatch(CurrentUserResponse(device_id = "A", single_device_login_enabled = true), "A"))
        assertFalse(HubRules.deviceMismatch(CurrentUserResponse(device_id = "A", single_device_login_enabled = false), "B"))
        assertFalse(HubRules.deviceMismatch(CurrentUserResponse(device_id = null, single_device_login_enabled = true), "B"))
    }

    // ---- banner ----

    private fun today(status: String = "hari_kerja", jamMasuk: String? = null, events: List<TodaySettingEvent>? = null) =
        TodaySettingData(status = status, jam_masuk = jamMasuk, events = events)

    private fun check(allow: Boolean?, type: String = "checkin", text: String = "", schedule: PresensiSchedule? = null) =
        PresensiCheckData(allow_attendance = allow, type_attendance = type, attendance_response_text = text, schedule = schedule)

    @Test
    fun `no answer or no agenda says the schedule is not set`() {
        val none = HubRules.attendanceBanner(null, check(true))
        assertEquals(BannerState.NO_AGENDA, none.state)
        assertEquals("Jadwal absensi belum diatur", none.title)
        assertFalse(none.showCta)
        assertEquals(BannerState.NO_AGENDA, HubRules.attendanceBanner(today("no_agenda"), check(true)).state)
        assertEquals(BannerState.NO_AGENDA, HubRules.attendanceBanner(today(""), check(true)).state)
    }

    @Test
    fun `holiday shows the first day-off event as the info and no event row`() {
        val banner = HubRules.attendanceBanner(
            today("holiday", events = listOf(TodaySettingEvent(name = "Upacara", day_off = false), TodaySettingEvent(name = "Libur Nasional", day_off = true))),
            check(true),
        )
        assertEquals(BannerState.HOLIDAY, banner.state)
        assertEquals("Libur Nasional", banner.info)
        assertEquals("", banner.eventName)
        assertEquals("Libur", HubRules.attendanceBanner(today("holiday"), check(true)).info)
    }

    @Test
    fun `a missing check means outside hours`() {
        val banner = HubRules.attendanceBanner(today(), null)
        assertEquals(BannerState.OUTSIDE_HOURS, banner.state)
        assertEquals("Absensi hanya bisa dilakukan pada jam yang ditentukan.", banner.info)
    }

    @Test
    fun `allowed attendance asks to check in or out now, with the source row and the CTA`() {
        val schedule = PresensiSchedule(start_at = "07:00", end_at = "15:00", source = "kalender_sekolah")
        val checkIn = HubRules.attendanceBanner(today(), check(true, "checkin", schedule = schedule))
        assertEquals(BannerState.CHECK_IN, checkIn.state)
        assertEquals("Wajib absen masuk sekarang", checkIn.title)
        assertEquals("Dari Kalender sekolah", checkIn.source)
        assertEquals("Jam kerja hari ini · 07:00 – 15:00", checkIn.workHours)
        assertTrue(checkIn.showCta)
        val checkOut = HubRules.attendanceBanner(today(), check(true, "CheckOut", schedule = schedule))
        assertEquals(BannerState.CHECK_OUT, checkOut.state)
        assertEquals("Wajib absen pulang sekarang", checkOut.title)
    }

    @Test
    fun `not allowed and already done reads complete, otherwise outside hours`() {
        assertEquals(BannerState.COMPLETE, HubRules.attendanceBanner(today(), check(false, "checkout", "Absensi hari ini sudah lengkap")).state)
        assertEquals(BannerState.COMPLETE, HubRules.attendanceBanner(today(), check(false, "", "")).state)
        assertEquals(BannerState.COMPLETE, HubRules.attendanceBanner(today(), check(false, "checkin", "Anda sudah melakukan presensi")).state)
        val outside = HubRules.attendanceBanner(today(), check(false, "checkin", "Absen masuk dibuka pukul 06:30"))
        assertEquals(BannerState.OUTSIDE_HOURS, outside.state)
        assertFalse(outside.showCta)
        assertEquals("", outside.source)
    }

    @Test
    fun `work hours fall back to jam_masuk and the first working event`() {
        assertEquals("Jam kerja hari ini · 07:00", HubRules.workHours(today(jamMasuk = "07:00"), null))
        val events = listOf(
            TodaySettingEvent(name = "Libur", start_time = "00:00", end_time = "23:59", day_off = true),
            TodaySettingEvent(name = "Apel", start_time = "07:15:00", end_time = "07:30:00", day_off = false),
        )
        assertEquals("Jam kerja hari ini · 07:15 – 07:30", HubRules.workHours(today(events = events), null))
        assertEquals("", HubRules.workHours(today(), null))
        assertEquals("Jam kerja hari ini · 08:00", HubRules.workHours(today(), PresensiSchedule(start_at = "2026-10-07 08:00:00")))
    }

    @Test
    fun `event name prefers a working event and ignores blanks`() {
        val events = listOf(TodaySettingEvent(name = " ", day_off = false), TodaySettingEvent(name = "Rapat", day_off = false))
        assertEquals("Rapat", HubRules.eventName(today(events = events)))
        assertEquals("", HubRules.eventName(today()))
        assertEquals("Hari Guru", HubRules.eventName(today(events = listOf(TodaySettingEvent(name = "Hari Guru", day_off = true)))))
    }

    @Test
    fun `source label names the calendar, the setting, or echoes an unknown source`() {
        assertEquals("Dari Kalender sekolah", HubRules.sourceLabel(PresensiSchedule(source = "calendar")))
        assertEquals("Dari Pengaturan jam", HubRules.sourceLabel(PresensiSchedule(source = "jam_kerja")))
        assertEquals("Dari Shift Malam", HubRules.sourceLabel(PresensiSchedule(source = "Shift Malam")))
        assertEquals("", HubRules.sourceLabel(PresensiSchedule(source = "")))
        assertEquals("", HubRules.sourceLabel(null))
    }
}
