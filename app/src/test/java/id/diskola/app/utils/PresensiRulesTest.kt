package id.diskola.app.utils

import id.diskola.app.utils.PresensiRules.CardTone
import id.diskola.app.utils.PresensiRules.LeaveBlock
import id.diskola.app.utils.PresensiRules.MasukStatus
import id.diskola.app.utils.PresensiRules.Proximity
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

    // ---- Proximity (status card) ----

    @Test
    fun proximity_insideAndOutside() {
        assertEquals(Proximity.Inside(45f, 100f), PresensiRules.proximity(restricted = true, radius = 100f, distanceMeters = 45f))
        assertEquals(Proximity.Outside(240f, 100f), PresensiRules.proximity(restricted = true, radius = 100f, distanceMeters = 240f))
    }

    @Test
    fun proximity_exactlyOnTheRadiusIsInside() {
        assertEquals(Proximity.Inside(100f, 100f), PresensiRules.proximity(true, 100f, 100f))
    }

    @Test
    fun proximity_withoutEnforcementIsUnrestrictedWhateverTheDistance() {
        assertEquals(Proximity.Unrestricted, PresensiRules.proximity(restricted = false, radius = 100f, distanceMeters = 5000f))
        assertEquals(Proximity.Unrestricted, PresensiRules.proximity(restricted = false, radius = 0f, distanceMeters = null))
    }

    @Test
    fun proximity_cannotBeJudgedWithoutAFixOrARadius() {
        assertEquals(Proximity.Unknown, PresensiRules.proximity(true, 100f, null))
        assertEquals(Proximity.Unknown, PresensiRules.proximity(true, 0f, 5000f))
    }

    /** The card must never say "outside" while the button is open, or the other way round. */
    @Test
    fun proximity_outsideMatchesTheOutsideRadiusStatus() {
        for (restricted in listOf(true, false)) for (radius in listOf(0f, 100f)) for (distance in listOf(null, 20f, 100f, 500f)) {
            val outsideCard = PresensiRules.proximity(restricted, radius, distance) is Proximity.Outside
            val outsideStatus = masuk(restricted = restricted, radius = radius, distance = distance) == MasukStatus.OutsideRadius
            assertEquals("restricted=$restricted radius=$radius distance=$distance", outsideStatus, outsideCard)
        }
    }

    @Test
    fun formatDistance_wholeMetersBelowOneKilometer() {
        assertEquals("0 m", PresensiRules.formatDistance(0f))
        assertEquals("85 m", PresensiRules.formatDistance(84.6f))
        assertEquals("999 m", PresensiRules.formatDistance(999.4f))
        assertEquals("0 m", PresensiRules.formatDistance(-3f))
    }

    @Test
    fun formatDistance_kilometersWithDecimalComma() {
        assertEquals("1,2 km", PresensiRules.formatDistance(1234f))
        assertEquals("12,3 km", PresensiRules.formatDistance(12_300f))
    }

    /** The server's default-ish radius is 9999 m; "10,0 km" read like a typo on the card. */
    @Test
    fun formatDistance_aRoundKilometerDropsTheDecimal() {
        assertEquals("1 km", PresensiRules.formatDistance(999.6f))
        assertEquals("10 km", PresensiRules.formatDistance(9999f))
        assertEquals("12 km", PresensiRules.formatDistance(12_000f))
        assertEquals("1,5 km", PresensiRules.formatDistance(1500f))
    }

    // ---- Status card ----

    private fun card(
        status: MasukStatus,
        locationOff: Boolean = false,
        proximity: Proximity = Proximity.Unknown,
    ) = PresensiRules.masukCard(status, locationOff, proximity)

    @Test
    fun card_loadingHasNoTextYet() {
        assertNull(card(MasukStatus.Loading))
    }

    @Test
    fun card_readyInsideRadiusIsSuccessWithTheDistance() {
        val card = card(MasukStatus.Ready("Absen Masuk"), proximity = Proximity.Inside(45f, 100f))!!
        assertEquals(CardTone.Success, card.tone)
        assertEquals("Anda berada di wilayah absensi", card.title)
        assertEquals("45 m dari titik sekolah · radius 100 m", card.detail)
    }

    @Test
    fun card_outsideRadiusIsErrorWithTheDistance() {
        val card = card(MasukStatus.OutsideRadius, proximity = Proximity.Outside(1234f, 100f))!!
        assertEquals(CardTone.Error, card.tone)
        assertEquals("Anda berada di luar wilayah absensi", card.title)
        assertEquals("1,2 km dari titik sekolah · radius 100 m", card.detail)
    }

    @Test
    fun card_readyWithoutEnforcementKeepsTheLegacyLine() {
        val card = card(MasukStatus.Ready("Absen Masuk"), proximity = Proximity.Unrestricted)!!
        assertEquals(CardTone.Neutral, card.tone)
        assertEquals("Absensi bisa dilakukan dimanapun", card.title)
        assertNull(card.detail)
    }

    @Test
    fun card_readyThatCannotBeJudgedStaysNeutral() {
        assertEquals(CardTone.Neutral, card(MasukStatus.Ready("Absen Masuk"), proximity = Proximity.Unknown)!!.tone)
    }

    @Test
    fun card_waitingForLocationOffersTheSettingsButtonOnlyWhenLocationIsOff() {
        val waiting = card(MasukStatus.WaitingLocation)!!
        assertEquals(CardTone.Info, waiting.tone)
        assertEquals("Mengambil lokasi…", waiting.title)
        assertFalse(waiting.offersEnableLocation)

        val off = card(MasukStatus.WaitingLocation, locationOff = true)!!
        assertEquals(CardTone.Warning, off.tone)
        assertEquals("Lokasi perangkat nonaktif", off.title)
        assertTrue(off.offersEnableLocation)
    }

    @Test
    fun card_locationOffOnlyMattersWhileWaiting() {
        assertFalse(card(MasukStatus.Ready("Absen Masuk"), locationOff = true, proximity = Proximity.Inside(1f, 50f))!!.offersEnableLocation)
    }

    @Test
    fun card_failuresAreErrorsAndTheServerTextIsShownAsSent() {
        assertEquals(CardTone.Error, card(MasukStatus.CheckFailed)!!.tone)
        assertEquals(CardTone.Error, card(MasukStatus.ScheduleNotSet)!!.tone)
        assertEquals("Jadwal presensi belum diatur", card(MasukStatus.ScheduleNotSet)!!.title)
        assertEquals(CardTone.Error, card(MasukStatus.Mock)!!.tone)
        val server = card(MasukStatus.ServerMessage("Sudah absen hari ini"))!!
        assertEquals(CardTone.Info, server.tone)
        assertEquals("Sudah absen hari ini", server.title)
    }

    @Test
    fun scheduleSource_isWordedLikeLegacyAndBlankStaysBlank() {
        assertEquals("Pengaturan jam", PresensiRules.scheduleSourceLabel("settings"))
        assertEquals("Kalender", PresensiRules.scheduleSourceLabel(" Calendar "))
        assertEquals("Workgroup", PresensiRules.scheduleSourceLabel("Workgroup"))
        assertEquals("", PresensiRules.scheduleSourceLabel("  "))
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

    @Test
    fun typeVerb_namesTheTypeOnlyWhenTheServerSaidWhich() {
        assertEquals("Masuk", PresensiRules.typeVerb("checkin"))
        assertEquals("Masuk", PresensiRules.typeVerb(" CheckIn "))
        assertEquals("Pulang", PresensiRules.typeVerb("checkout"))
        assertEquals(null, PresensiRules.typeVerb(""))
        assertEquals(null, PresensiRules.typeVerb("lainnya"))
    }
}
