package id.diskola.app.utils

import id.diskola.app.utils.AkmSessionRules.BackAction
import id.diskola.app.utils.AkmSessionRules.EndReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmSessionRulesTest {

    // ---- password gate ----

    @Test
    fun `an exam without a password never asks for one`() {
        assertTrue(AkmSessionRules.skipPassword(needsPassword = false, passwordChecked = false, markerActive = false))
        assertTrue(AkmSessionRules.skipPassword(needsPassword = false, passwordChecked = false, markerActive = true))
    }

    @Test
    fun `a password already passed is not asked again after a clean session`() {
        assertTrue(AkmSessionRules.skipPassword(needsPassword = true, passwordChecked = true, markerActive = false))
    }

    @Test
    fun `an unclosed session brings the password back even if it was passed`() {
        assertFalse(AkmSessionRules.skipPassword(needsPassword = true, passwordChecked = true, markerActive = true))
    }

    @Test
    fun `a password never passed is asked for`() {
        assertFalse(AkmSessionRules.skipPassword(needsPassword = true, passwordChecked = false, markerActive = false))
    }

    // ---- online check ----

    @Test
    fun `a strict exam needs real internet to start, a lenient one does not`() {
        assertFalse(AkmSessionRules.mayStart(strict = true, onlineValidated = false))
        assertTrue(AkmSessionRules.mayStart(strict = true, onlineValidated = true))
        assertTrue(AkmSessionRules.mayStart(strict = false, onlineValidated = false))
    }

    // ---- the marker ----

    @Test
    fun `only handing in, running out of time and an already uploaded exam close the session properly`() {
        assertTrue(EndReason.Submit.completes)
        assertTrue(EndReason.TimeUp.completes)
        assertTrue(EndReason.AlreadyUploaded.completes)
        assertFalse(EndReason.Abort.completes)
        assertFalse(EndReason.Left.completes)
    }

    @Test
    fun `a marker left over from another run is an unclosed session, the one running now is not`() {
        assertTrue(AkmSessionRules.isUnclosed(sessionAlreadyRunning = false, markerActive = true))
        assertFalse(AkmSessionRules.isUnclosed(sessionAlreadyRunning = true, markerActive = true))
        assertFalse(AkmSessionRules.isUnclosed(sessionAlreadyRunning = false, markerActive = false))
    }

    // ---- terms ----

    @Test
    fun `the strict terms mention the lock and the online start, the lenient ones allow offline`() {
        val strict = AkmSessionRules.termsBody(strict = true)
        assertTrue(strict.startsWith("• Layar akan dikunci selama ujian."))
        assertTrue(strict.contains("• Ujian harus dimulai dalam keadaan terhubung internet.\n"))
        assertFalse(strict.contains("tanpa koneksi internet diperbolehkan"))

        val lenient = AkmSessionRules.termsBody(strict = false)
        assertTrue(lenient.startsWith("• Pengerjaan ujian tanpa koneksi internet diperbolehkan.\n"))
        assertFalse(lenient.contains("Layar akan dikunci"))
    }

    @Test
    fun `both terms carry the fixed rules word for word and end with the consent line`() {
        listOf(true, false).forEach { strict ->
            val body = AkmSessionRules.termsBody(strict)
            assertTrue(body.contains("• Ujian akan dikenakan sanksi apabila Anda keluar dari halaman ujian atau berpindah aplikasi.\n"))
            assertTrue(body.contains("• Pastikan koneksi internet stabil saat mengunggah jawaban.\n"))
            assertTrue(body.contains("• Selama ujian berlangsung, tidak diperbolehkan mengangkat panggilan telepon.\n\n"))
            assertTrue(body.endsWith("Dengan menekan OK, Anda menyetujui ketentuan di atas. "))
        }
        assertEquals("Pemberitahuan !!!", AkmSessionRules.TERMS_TITLE)
    }

    @Test
    fun `the offline refusal says what to do`() {
        assertEquals("Butuh Koneksi Internet", AkmSessionRules.OFFLINE_TITLE)
        assertTrue(AkmSessionRules.OFFLINE_BODY.startsWith("Ujian ini harus dimulai dalam keadaan terhubung internet."))
        assertEquals("Mengerti", AkmSessionRules.OFFLINE_OK)
    }

    // ---- priming ----

    @Test
    fun `the priming screen lists five things that are recorded and two that only inform`() {
        assertEquals(5, AkmSessionRules.Priming.VIOLATIONS.size)
        assertEquals("Merekam layar", AkmSessionRules.Priming.VIOLATIONS.last())
        assertEquals(2, AkmSessionRules.Priming.NEUTRAL.size)
        assertEquals("Sebelum Memulai Ujian", AkmSessionRules.Priming.TITLE)
        assertEquals("Mulai Ujian", AkmSessionRules.Priming.START)
    }

    @Test
    fun `the priming screen names the cancelled mark the same way the other screens do`() {
        assertEquals(
            "Jadwal ini ditandai \"DIBATALKAN\" oleh sekolah. Kamu tetap dapat mengerjakan dan mengumpulkan jawaban.",
            AkmSessionRules.Priming.watermarkNotice("DIBATALKAN"),
        )
    }

    // ---- leaving the exam ----

    @Test
    fun `strict mode refuses to leave`() {
        val action = AkmSessionRules.backAction(strict = true, penaltyOn = true) as BackAction.Refuse
        assertEquals("Ujian Sedang Berlangsung", action.title)
        assertEquals("Mengerti", action.okLabel)
        assertTrue(action.message.startsWith("Kamu tidak dapat keluar selama ujian berlangsung."))
        assertEquals(action, AkmSessionRules.backAction(strict = true, penaltyOn = false))
    }

    @Test
    fun `lenient mode warns, and the warning depends on the penalty`() {
        val withPenalty = AkmSessionRules.backAction(strict = false, penaltyOn = true) as BackAction.Confirm
        val without = AkmSessionRules.backAction(strict = false, penaltyOn = false) as BackAction.Confirm
        assertEquals("Peringatan", withPenalty.title)
        assertEquals("Tetap mengerjakan", withPenalty.stayLabel)
        assertEquals("Ya, keluar", withPenalty.leaveLabel)
        assertTrue(withPenalty.message.endsWith("kamu terkena penalti saat membuka ujian ini lagi."))
        assertTrue(without.message.endsWith("dilaporkan ke pihak sekolah."))
        assertTrue(withPenalty.message.startsWith("Ujian baru dianggap selesai bila kamu menekan \"Kumpulkan\".\n\n"))
    }

    // ---- recovery dialog ----

    @Test
    fun `the recovery dialog keeps the legacy words and adds one way out`() {
        assertEquals("Mode Terkunci Terlepas", AkmSessionRules.RECOVERY_TITLE)
        assertEquals("Aktifkan", AkmSessionRules.RECOVERY_ACTIVATE)
        assertEquals("Memeriksa...", AkmSessionRules.RECOVERY_CHECKING)
        assertTrue(AkmSessionRules.RECOVERY_RETRY_MESSAGE.startsWith("Penyematan layar belum aktif."))
        assertEquals("Keluar dari ujian", AkmSessionRules.RECOVERY_ESCAPE)
    }
}
