package id.diskola.app.utils

import id.diskola.app.utils.PresensiRules.LeaveBlock
import id.diskola.app.utils.PresensiRules.MasukStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresensiRulesTest {

    // ---- clock + gate ----

    @Test
    fun toClock_formatsServerTimestamp() {
        assertEquals("07:42", PresensiRules.toClock("2026-10-07 07:42:15"))
    }

    @Test
    fun toClock_keepsUnparsableValueAsIs() {
        assertEquals("kemarin", PresensiRules.toClock("kemarin"))
    }

    @Test
    fun pendingOrApprovedLeaveClosesTheButton() {
        assertFalse(PresensiRules.canPerformAttendance("pending"))
        assertFalse(PresensiRules.canPerformAttendance("Approved"))
        assertTrue(PresensiRules.canPerformAttendance("rejected"))
        assertTrue(PresensiRules.canPerformAttendance(null))
        assertTrue(PresensiRules.canPerformAttendance(""))
    }

    @Test
    fun radiusFallsBackToFiftyMeters() {
        assertEquals(9999f, PresensiRules.radiusMeters("9999"))
        assertEquals(50f, PresensiRules.radiusMeters(""))
        assertEquals(50f, PresensiRules.radiusMeters("lima puluh"))
    }

    // ---- Masuk / Pulang ----

    private fun masuk(
        loading: Boolean = false,
        checkFailed: Boolean = false,
        allow: Boolean = true,
        label: String = "Absen Masuk",
        serverText: String = "",
        hasLocation: Boolean = true,
        mock: Boolean = false,
        restricted: Boolean = true,
        radius: Float = 100f,
        distance: Float? = 20f,
    ) = PresensiRules.masukStatus(loading, checkFailed, allow, label, serverText, hasLocation, mock, restricted, radius, distance)

    @Test
    fun masuk_insideRadius_isReady() {
        assertEquals(MasukStatus.Ready("Absen Masuk"), masuk())
    }

    @Test
    fun masuk_outsideRadius_isBlockedOnlyWhenRestricted() {
        assertEquals(MasukStatus.OutsideRadius, masuk(distance = 500f))
        assertEquals(MasukStatus.Ready("Absen Masuk"), masuk(distance = 500f, restricted = false))
    }

    @Test
    fun masuk_distanceEqualToRadiusIsInside() {
        assertEquals(MasukStatus.Ready("Absen Masuk"), masuk(radius = 100f, distance = 100f))
    }

    @Test
    fun masuk_zeroRadiusCannotBeCheckedAndIsPermissive() {
        assertEquals(MasukStatus.Ready("Absen Masuk"), masuk(radius = 0f, distance = 5000f))
    }

    @Test
    fun masuk_serverTextHidesTheButton() {
        assertEquals(MasukStatus.ServerMessage("Sudah absen"), masuk(serverText = "Sudah absen"))
    }

    @Test
    fun masuk_missingScheduleIsReportedAsNotConfigured() {
        assertEquals(MasukStatus.ScheduleNotSet, masuk(allow = false))
        assertEquals(MasukStatus.ScheduleNotSet, masuk(label = ""))
    }

    @Test
    fun masuk_failedCheckIsNotMistakenForMissingSchedule() {
        assertEquals(MasukStatus.CheckFailed, masuk(checkFailed = true, allow = false, label = ""))
    }

    @Test
    fun masuk_mockLocationWinsOverInsideRadius() {
        assertEquals(MasukStatus.Mock, masuk(mock = true))
    }

    @Test
    fun masuk_waitsForLocation() {
        assertEquals(MasukStatus.WaitingLocation, masuk(hasLocation = false, distance = null))
        assertEquals(MasukStatus.Loading, masuk(loading = true))
    }

    // ---- Dinas Luar ----

    @Test
    fun offsite_noteIsCountedTrimmed() {
        val errors = PresensiRules.validateOffsite("   abc   ", "Jl. Mawar", hasPhoto = true, requirePhoto = true)
        assertEquals("Catatan minimal 10 karakter", errors.note)
        assertFalse(PresensiRules.canSubmitOffsite(true, false, "          ", hasPhoto = true, requirePhoto = true))
    }

    @Test
    fun offsite_requiresAddressAndPhoto() {
        val errors = PresensiRules.validateOffsite("kunjungan dinas ke dinas pendidikan", " ", hasPhoto = false, requirePhoto = true)
        assertNull(errors.note)
        assertEquals("Alamat wajib diisi", errors.address)
        assertEquals("Foto bukti wajib diunggah", errors.photo)
        assertNull(PresensiRules.validateOffsite("kunjungan dinas ke dinas pendidikan", "Jl. A", hasPhoto = false, requirePhoto = false).photo)
    }

    @Test
    fun offsite_noteOver500IsRejected() {
        val errors = PresensiRules.validateOffsite("x".repeat(501), "Jl. A", hasPhoto = true, requirePhoto = true)
        assertEquals("Catatan maksimal 500 karakter", errors.note)
    }

    @Test
    fun offsite_submitEnabledRule() {
        val note = "kunjungan dinas ke dinas pendidikan"
        assertTrue(PresensiRules.canSubmitOffsite(true, false, note, hasPhoto = true, requirePhoto = true))
        assertFalse(PresensiRules.canSubmitOffsite(false, false, note, hasPhoto = true, requirePhoto = true))
        assertFalse(PresensiRules.canSubmitOffsite(true, true, note, hasPhoto = true, requirePhoto = true))
        assertFalse(PresensiRules.canSubmitOffsite(true, false, note, hasPhoto = false, requirePhoto = true))
        assertTrue(PresensiRules.canSubmitOffsite(true, false, note, hasPhoto = false, requirePhoto = false))
    }

    @Test
    fun offsite_codeZeroIsFailure() {
        assertFalse(PresensiRules.offsiteSucceeded(0, "Tepat Waktu"))
        assertTrue(PresensiRules.offsiteSucceeded(200, null))
        assertTrue(PresensiRules.offsiteSucceeded(null, "Tepat Waktu"))
        assertFalse(PresensiRules.offsiteSucceeded(null, ""))
    }

    // ---- Izin ----

    @Test
    fun leave_blockedAfterCheckInOrWhilePendingOrApproved() {
        assertEquals(LeaveBlock.CHECKED_IN, PresensiRules.leaveBlock(true, null))
        assertEquals(LeaveBlock.PENDING, PresensiRules.leaveBlock(false, "pending"))
        assertEquals(LeaveBlock.APPROVED, PresensiRules.leaveBlock(false, "approved"))
        assertNull(PresensiRules.leaveBlock(false, "rejected"))
        assertNull(PresensiRules.leaveBlock(false, null))
    }

    @Test
    fun leave_validationReportsFileFirst() {
        assertEquals("File bukti wajib diunggah", PresensiRules.validateLeave(false, null, "", 0))
        assertEquals("Jenis pengajuan wajib dipilih", PresensiRules.validateLeave(true, null, "demam tinggi sejak semalam", 100))
        assertEquals("Keterangan minimal 10 karakter", PresensiRules.validateLeave(true, "sakit", "demam", 100))
        assertEquals("Keterangan maksimal 500 karakter", PresensiRules.validateLeave(true, "sakit", "x".repeat(501), 100))
        assertEquals("Ukuran file maksimal 2 MB", PresensiRules.validateLeave(true, "sakit", "demam tinggi sejak semalam", 3L * 1024 * 1024))
        assertNull(PresensiRules.validateLeave(true, "sakit", "demam tinggi sejak semalam", 1024))
    }
}
