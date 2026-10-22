package id.diskola.app.utils

import id.diskola.app.apiservice.ApiException
import id.diskola.app.dataclass.akm.AkmStatus
import id.diskola.app.utils.AkmRules.DeadlineAction
import id.diskola.app.utils.AkmRules.Failure
import id.diskola.app.utils.AkmRules.LocalExam
import id.diskola.app.utils.AkmRules.SubmissionState
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmRulesTest {

    private fun api(code: Int?, message: String = "x") = ApiException(message, code)

    // ---- logout block ----

    @Test
    fun `a downloaded exam with a session blocks logout until its answers are confirmed`() {
        assertTrue(AkmRules.isPending(LocalExam("DOWNLOADED", 10, null)))
        assertTrue(AkmRules.isPending(LocalExam("DOWNLOADED", 10, SubmissionState.QUEUED)))
        assertFalse(AkmRules.isPending(LocalExam("DOWNLOADED", 10, SubmissionState.SENT)))
    }

    @Test
    fun `a final rejection by the server no longer blocks`() {
        assertFalse(AkmRules.isPending(LocalExam("DOWNLOADED", 10, SubmissionState.REJECTED)))
    }

    @Test
    fun `an exam that was never downloaded or has no session cannot be pending`() {
        assertFalse(AkmRules.isPending(LocalExam("NONE", 0, null)))
        assertFalse(AkmRules.isPending(LocalExam("DOWNLOADING", 10, null)))
        assertFalse(AkmRules.isPending(LocalExam("FAILED", 10, null)))
        assertFalse(AkmRules.isPending(LocalExam("DOWNLOADED", 0, null)))
    }

    @Test
    fun `a queued submission blocks even when the session id is missing`() {
        assertTrue(AkmRules.isPending(LocalExam("DOWNLOADED", 0, SubmissionState.QUEUED)))
    }

    // ---- status ----

    @Test
    fun `server status text wins and flags rank assessed over queued over done`() {
        assertEquals(AkmStatus.SudahDinilai, AkmRules.serverStatus("COMPLETED", false, false, false))
        assertEquals(AkmStatus.SesiTerlewat, AkmRules.serverStatus("sesi terlewat", true, true, true))
        assertEquals(AkmStatus.TidakMengerjakan, AkmRules.serverStatus("Tidak Mengerjakan", true, true, true))
        assertEquals(AkmStatus.SudahDinilai, AkmRules.serverStatus("", true, true, true))
        assertEquals(AkmStatus.SudahDikumpulkan, AkmRules.serverStatus("", false, true, true))
        assertEquals(AkmStatus.MenungguPenilaian, AkmRules.serverStatus("", false, false, true))
        assertEquals(AkmStatus.BelumSinkron, AkmRules.serverStatus("", false, false, false))
    }

    @Test
    fun `local state alone`() {
        assertEquals(AkmStatus.BelumSinkron, AkmRules.localStatus(null))
        assertEquals(AkmStatus.MengunduhSoal, AkmRules.localStatus(LocalExam("DOWNLOADING", 0, null)))
        assertEquals(AkmStatus.SiapDikerjakan, AkmRules.localStatus(LocalExam("DOWNLOADED", 3, null)))
        assertEquals(AkmStatus.ProsesUpload, AkmRules.localStatus(LocalExam("DOWNLOADED", 3, SubmissionState.QUEUED)))
        assertEquals(AkmStatus.SudahDikumpulkan, AkmRules.localStatus(LocalExam("DOWNLOADED", 3, SubmissionState.SENT)))
        assertEquals(AkmStatus.BelumSinkron, AkmRules.localStatus(LocalExam("FAILED", 3, null)))
    }

    @Test
    fun `an older local state never downgrades the server`() {
        assertEquals(AkmStatus.SudahDinilai, AkmRules.effectiveStatus(AkmStatus.SudahDinilai, LocalExam("DOWNLOADED", 3, null)))
        assertEquals(AkmStatus.MenungguPenilaian, AkmRules.effectiveStatus(AkmStatus.MenungguPenilaian, LocalExam("DOWNLOADED", 3, SubmissionState.SENT)))
    }

    @Test
    fun `a local state that is further along survives a list that has not caught up`() {
        assertEquals(AkmStatus.SiapDikerjakan, AkmRules.effectiveStatus(AkmStatus.BelumSinkron, LocalExam("DOWNLOADED", 3, null)))
        assertEquals(AkmStatus.ProsesUpload, AkmRules.effectiveStatus(AkmStatus.BelumSinkron, LocalExam("DOWNLOADED", 3, SubmissionState.QUEUED)))
        assertEquals(AkmStatus.SudahDikumpulkan, AkmRules.effectiveStatus(AkmStatus.BelumSinkron, LocalExam("DOWNLOADED", 3, SubmissionState.SENT)))
    }

    @Test
    fun `only a queued or graded exam proves the server holds the answers`() {
        assertTrue(AkmRules.serverAlreadyHasAnswers(AkmStatus.SudahDikumpulkan))
        assertTrue(AkmRules.serverAlreadyHasAnswers(AkmStatus.SudahDinilai))
        // isDone alone only means the window closed; the "never took it" verdicts mean nothing was received.
        assertFalse(AkmRules.serverAlreadyHasAnswers(AkmStatus.MenungguPenilaian))
        assertFalse(AkmRules.serverAlreadyHasAnswers(AkmStatus.TidakMengerjakan))
        assertFalse(AkmRules.serverAlreadyHasAnswers(AkmStatus.SesiTerlewat))
        assertFalse(AkmRules.serverAlreadyHasAnswers(AkmStatus.BelumSinkron))
        assertFalse(AkmRules.serverAlreadyHasAnswers(AkmStatus.SiapDikerjakan))
    }

    @Test
    fun `a closed window does not make unsent answers look handed in`() {
        assertEquals(AkmStatus.ProsesUpload, AkmRules.effectiveStatus(AkmStatus.MenungguPenilaian, LocalExam("DOWNLOADED", 3, null)))
        assertEquals(AkmStatus.ProsesUpload, AkmRules.effectiveStatus(AkmStatus.MenungguPenilaian, LocalExam("DOWNLOADED", 3, SubmissionState.QUEUED)))
        assertEquals(AkmStatus.MenungguPenilaian, AkmRules.effectiveStatus(AkmStatus.MenungguPenilaian, LocalExam("DOWNLOADED", 3, SubmissionState.SENT)))
        assertEquals(AkmStatus.MenungguPenilaian, AkmRules.effectiveStatus(AkmStatus.MenungguPenilaian, null))
    }

    // ---- failure classification ----

    @Test
    fun `offline and server trouble are retried`() {
        assertEquals(Failure.Retry, AkmRules.classifyFailure(api(0)))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(api(null)))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(api(500)))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(api(503)))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(api(429)))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(api(408)))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(SocketTimeoutException()))
        assertEquals(Failure.Retry, AkmRules.classifyFailure(IOException("reset")))
    }

    @Test
    fun `an expired session is its own case`() {
        assertEquals(Failure.AuthExpired, AkmRules.classifyFailure(api(401)))
    }

    @Test
    fun `other client errors are final and keep the server message`() {
        assertEquals(Failure.Permanent("Ujian sudah ditutup"), AkmRules.classifyFailure(api(422, "Ujian sudah ditutup")))
        assertEquals(Failure.Permanent("Dilarang"), AkmRules.classifyFailure(api(403, "Dilarang")))
        val blank = AkmRules.classifyFailure(api(404, "")) as Failure.Permanent
        assertTrue(blank.message.contains("404"))
    }

    @Test
    fun `an unknown exception is retried`() {
        assertEquals(Failure.Retry, AkmRules.classifyFailure(IllegalStateException("boom")))
    }

    @Test
    fun `attempts stop at the cap and the backoff is linear`() {
        assertTrue(AkmRules.mayRetry(0))
        assertTrue(AkmRules.mayRetry(9))
        assertFalse(AkmRules.mayRetry(10))
        assertEquals(10_000L, AkmRules.backoffMillis(1))
        assertEquals(30_000L, AkmRules.backoffMillis(3))
        assertEquals(10_000L, AkmRules.backoffMillis(0))
    }

    // ---- deadline ----

    @Test
    fun `a manual submit always goes out`() {
        assertEquals(DeadlineAction.Submit, AkmRules.deadlineAction(1_000, 9_999_999, manual = true))
        assertEquals(DeadlineAction.Submit, AkmRules.deadlineAction(1_000, null, manual = true))
    }

    @Test
    fun `an early deadline run reschedules for the remaining time`() {
        assertEquals(DeadlineAction.Reschedule(60_000), AkmRules.deadlineAction(40_000, 100_000, manual = false))
    }

    @Test
    fun `within the grace or after the end the deadline run submits`() {
        assertEquals(DeadlineAction.Submit, AkmRules.deadlineAction(95_000, 100_000, manual = false))
        assertEquals(DeadlineAction.Submit, AkmRules.deadlineAction(200_000, 100_000, manual = false))
    }

    @Test
    fun `just outside the grace it still reschedules`() {
        assertEquals(DeadlineAction.Reschedule(5_001), AkmRules.deadlineAction(94_999, 100_000, manual = false))
    }

    @Test
    fun `an unreadable end time is never guessed`() {
        assertEquals(DeadlineAction.Skip, AkmRules.deadlineAction(1_000, null, manual = false))
    }

    @Test
    fun `date and time are parsed in the given zone and rejected when malformed`() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals(1_776_168_000_000L, AkmRules.parseDateTime("2026-04-14", "12:00", utc))
        val jakarta = TimeZone.getTimeZone("Asia/Jakarta")
        assertEquals(1_776_168_000_000L - 7 * 3_600_000L, AkmRules.parseDateTime("2026-04-14", "12:00", jakarta))
        assertNull(AkmRules.parseDateTime("", "12:00", utc))
        assertNull(AkmRules.parseDateTime("2026-04-14", "", utc))
        assertNull(AkmRules.parseDateTime("14/04/2026", "12:00", utc))
        assertNull(AkmRules.parseDateTime("2026-02-31", "12:00", utc))
        assertNull(AkmRules.parseDateTime("2026-04-14", "25:99", utc))
    }

    @Test
    fun `time is up on the full timestamp, not on hours and minutes`() {
        assertTrue(AkmRules.isTimeUp(100, 100))
        assertTrue(AkmRules.isTimeUp(101, 100))
        assertFalse(AkmRules.isTimeUp(99, 100))
        assertFalse(AkmRules.isTimeUp(Long.MAX_VALUE, null))
    }

    // ---- re-upload ----

    @Test
    fun `cooldown counts whole seconds up`() {
        assertEquals(0, AkmRules.cooldownSecondsLeft(1_000, 1_000))
        assertEquals(0, AkmRules.cooldownSecondsLeft(2_000, 1_000))
        assertEquals(1, AkmRules.cooldownSecondsLeft(1_000, 1_001))
        assertEquals(60, AkmRules.cooldownSecondsLeft(0, 60_000))
        assertEquals(60, AkmRules.cooldownSecondsLeft(0, 59_001))
    }

    @Test
    fun `reupload is offered for an upload that is waiting or done unless the exam was never taken`() {
        assertTrue(AkmRules.canReupload(AkmStatus.ProsesUpload, ""))
        assertTrue(AkmRules.canReupload(AkmStatus.SudahDikumpulkan, "Menunggu penilaian"))
        assertTrue(AkmRules.canReupload(AkmStatus.MenungguPenilaian, ""))
        assertFalse(AkmRules.canReupload(AkmStatus.MenungguPenilaian, "Tidak Mengerjakan"))
        assertFalse(AkmRules.canReupload(AkmStatus.SudahDikumpulkan, "SESI TERLEWAT"))
        assertFalse(AkmRules.canReupload(AkmStatus.SudahDinilai, ""))
        assertFalse(AkmRules.canReupload(AkmStatus.SiapDikerjakan, ""))
    }

    // ---- answers ----

    @Test
    fun `essay cleaning depends on the question type`() {
        assertEquals("satukata", AkmRules.cleanEssay("SHORT_ESSAY_WORD", " satu kata\n"))
        assertEquals("123", AkmRules.cleanEssay("SHORT_ESSAY_NUM", "1a2 3"))
        assertEquals(" bebas  teks ", AkmRules.cleanEssay("ESSAY", " bebas  teks "))
    }

    @Test
    fun `submit needs every question answered`() {
        assertTrue(AkmRules.allAnswered(listOf(1, 2, 3), setOf(1, 2, 3, 9)))
        assertFalse(AkmRules.allAnswered(listOf(1, 2, 3), setOf(1, 2)))
        assertFalse(AkmRules.allAnswered(emptyList(), setOf(1)))
    }

    // ---- watermark ----

    @Test
    fun `a server value wins and false restores a cancelled schedule`() {
        val known = AkmRules.AkmWatermark(true, "DIBATALKAN")
        assertEquals(AkmRules.AkmWatermark.None, AkmRules.watermark(false, "", known))
        assertEquals(AkmRules.AkmWatermark.None, AkmRules.watermark(false, "apa saja", null))
        assertEquals(AkmRules.AkmWatermark(true, "SESI DIGANTI"), AkmRules.watermark(true, "SESI DIGANTI", null))
    }

    @Test
    fun `a missing key keeps what is stored instead of resetting it`() {
        val known = AkmRules.AkmWatermark(true, "DIBATALKAN")
        assertEquals(known, AkmRules.watermark(null, "", known))
        assertEquals(AkmRules.AkmWatermark.None, AkmRules.watermark(null, "", null))
    }

    @Test
    fun `an enabled watermark without a text gets the default label`() {
        assertEquals(AkmRules.AkmWatermark(true, "DIBATALKAN"), AkmRules.watermark(true, "", null))
        assertEquals(AkmRules.AkmWatermark(true, "DIBATALKAN"), AkmRules.watermark(true, "   ", null))
        assertEquals(AkmRules.AkmWatermark(true, "BATAL"), AkmRules.watermark(true, "  BATAL ", null))
    }

    // ---- essay input ----

    @Test
    fun `text without line breaks is untouched`() {
        assertEquals("jawaban saya", AkmRules.sanitizeEssayInput("jawaban sa", "jawaban saya"))
        assertEquals("", AkmRules.sanitizeEssayInput("abc", ""))
    }

    @Test
    fun `a single typed enter is dropped wherever it lands`() {
        assertEquals("abc", AkmRules.sanitizeEssayInput("abc", "abc\n"))
        assertEquals("abc", AkmRules.sanitizeEssayInput("abc", "ab\nc"))
        assertEquals("abc", AkmRules.sanitizeEssayInput("abc", "\nabc"))
        assertEquals("abc", AkmRules.sanitizeEssayInput("abc", "abc\r"))
        assertEquals("", AkmRules.sanitizeEssayInput("", "\n"))
    }

    @Test
    fun `a pasted block becomes one line with spaces for the breaks`() {
        assertEquals("baris satu baris dua baris tiga", AkmRules.sanitizeEssayInput("", "baris satu\nbaris dua\nbaris tiga"))
        assertEquals("a b c", AkmRules.sanitizeEssayInput("", "a\r\nb\rc"))
        assertEquals("awal tempel satu tempel dua akhir", AkmRules.sanitizeEssayInput("awal  akhir", "awal tempel satu\ntempel dua akhir"))
    }

    @Test
    fun `a two character line break is a paste, so it becomes a space`() {
        assertEquals("a b", AkmRules.sanitizeEssayInput("ab", "a\r\nb"))
    }

    @Test
    fun `done key for essays and one-word answers, number keyboard for numeric answers`() {
        assertTrue(AkmRules.usesDoneAction("ESSAY"))
        assertTrue(AkmRules.usesDoneAction("SHORT_ESSAY_WORD"))
        assertFalse(AkmRules.usesDoneAction("SHORT_ESSAY_NUM"))
        assertEquals(8, AkmRules.ESSAY_MIN_LINES)
    }

    // ---- server switches ----

    @Test
    fun `a switch the app never received counts as on, a received zero as off`() {
        assertTrue(AkmRules.isStrict(AkmRules.SETTING_NEVER_FETCHED))
        assertTrue(AkmRules.isPenaltyApplied(AkmRules.SETTING_NEVER_FETCHED))
        assertTrue(AkmRules.isStrict(1))
        assertFalse(AkmRules.isStrict(0))
        assertTrue(AkmRules.isPenaltyApplied(1))
        assertFalse(AkmRules.isPenaltyApplied(0))
    }

    @Test
    fun `exam_lock_mode is stored as off when the key was missing, on for an explicit null`() {
        assertEquals(0, AkmRules.lockModeRaw(false)) // a missing key reaches here as the DTO default, false
        assertEquals(1, AkmRules.lockModeRaw(true))
        assertEquals(1, AkmRules.lockModeRaw(null))
    }

    @Test
    fun `penalty_applied is stored the same way`() {
        assertEquals(0, AkmRules.penaltyAppliedRaw(false))
        assertEquals(1, AkmRules.penaltyAppliedRaw(true))
        assertEquals(1, AkmRules.penaltyAppliedRaw(null))
    }

    @Test
    fun `the penalty lasts penalty_times seconds and five minutes when that is unusable`() {
        assertEquals(120, AkmRules.penaltySeconds(120))
        assertEquals(300, AkmRules.penaltySeconds(0))
        assertEquals(300, AkmRules.penaltySeconds(-1))
    }

    @Test
    fun `settings are stale after twelve hours, when never fetched, or when the clock went back`() {
        val now = 100_000_000_000L
        assertTrue(AkmRules.settingsStale(0L, now))
        assertFalse(AkmRules.settingsStale(now - AkmRules.SETTINGS_TTL_MS, now))
        assertTrue(AkmRules.settingsStale(now - AkmRules.SETTINGS_TTL_MS - 1, now))
        assertTrue(AkmRules.settingsStale(now + 1, now))
        assertFalse(AkmRules.settingsStale(now - 1_000, now))
    }
}
