package id.diskola.app.utils

import id.diskola.app.utils.AkmPenaltyRules.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmPenaltyRulesTest {

    // ---- decision ----

    @Test
    fun `with the penalty system off a violation is only recorded`() {
        assertEquals(Decision.None, AkmPenaltyRules.decide(penaltyOn = false, triggersPenalty = true, alreadyActive = false, systemOwned = false))
        assertEquals(Decision.None, AkmPenaltyRules.decide(penaltyOn = false, triggersPenalty = true, alreadyActive = true, systemOwned = false))
    }

    @Test
    fun `information-only violations never raise a penalty`() {
        assertEquals(Decision.None, AkmPenaltyRules.decide(penaltyOn = true, triggersPenalty = false, alreadyActive = false, systemOwned = false))
    }

    @Test
    fun `a violation with the penalty on starts one`() {
        assertEquals(Decision.Start, AkmPenaltyRules.decide(penaltyOn = true, triggersPenalty = true, alreadyActive = false, systemOwned = false))
    }

    @Test
    fun `a second violation during a penalty only updates the cause of the one dialog`() {
        assertEquals(Decision.UpdateCause, AkmPenaltyRules.decide(penaltyOn = true, triggersPenalty = true, alreadyActive = true, systemOwned = false))
        // even if the system caused it: the dialog is already there
        assertEquals(Decision.UpdateCause, AkmPenaltyRules.decide(penaltyOn = true, triggersPenalty = true, alreadyActive = true, systemOwned = true))
    }

    @Test
    fun `what the system caused starts no penalty`() {
        assertEquals(Decision.None, AkmPenaltyRules.decide(penaltyOn = true, triggersPenalty = true, alreadyActive = false, systemOwned = true))
    }

    // ---- time ----

    @Test
    fun `the penalty lasts penalty_times seconds, five minutes when that is unusable`() {
        assertEquals(1_000_000L + 120_000L, AkmPenaltyRules.endAt(1_000_000L, 120))
        assertEquals(1_000_000L + 300_000L, AkmPenaltyRules.endAt(1_000_000L, 0))
        assertEquals(1_000_000L + 300_000L, AkmPenaltyRules.endAt(1_000_000L, -5))
    }

    @Test
    fun `what is left counts down to zero and stays there`() {
        val end = 1_000_000L + 120_000L
        assertEquals(120_000L, AkmPenaltyRules.remainingMillis(end, 1_000_000L, 120_000L))
        assertEquals(30_000L, AkmPenaltyRules.remainingMillis(end, 1_090_000L, 120_000L))
        assertEquals(0L, AkmPenaltyRules.remainingMillis(end, end, 120_000L))
        assertEquals(0L, AkmPenaltyRules.remainingMillis(end, end + 5_000L, 120_000L))
    }

    @Test
    fun `a clock moved back cannot stretch the penalty beyond its length`() {
        val end = 1_000_000L + 120_000L
        assertEquals(120_000L, AkmPenaltyRules.remainingMillis(end, 1_000_000L - 3_600_000L, 120_000L))
    }

    @Test
    fun `nothing stored means no penalty`() {
        assertEquals(0L, AkmPenaltyRules.remainingMillis(0L, 5L, 120_000L))
        assertEquals(0L, AkmPenaltyRules.remainingMillis(100L, 5L, 0L))
    }

    @Test
    fun `a penalty survives midnight, which the legacy HH mm ss clock got wrong`() {
        val beforeMidnight = 1_792_195_140_000L // 2026-10-16 23:59:00 UTC, one minute before midnight
        val end = AkmPenaltyRules.endAt(beforeMidnight, 300)
        assertEquals(240_000L, AkmPenaltyRules.remainingMillis(end, beforeMidnight + 60_000L, 300_000L))
        assertEquals(60_000L, AkmPenaltyRules.remainingMillis(end, beforeMidnight + 240_000L, 300_000L))
    }

    // ---- words ----

    @Test
    fun `the pill reads minutes and seconds`() {
        assertEquals("5:00", AkmPenaltyRules.pillLabel(300_000))
        assertEquals("4:59", AkmPenaltyRules.pillLabel(299_000))
        assertEquals("0:01", AkmPenaltyRules.pillLabel(1))
        assertEquals("0:00", AkmPenaltyRules.pillLabel(0))
        assertEquals("0:00", AkmPenaltyRules.pillLabel(-50))
        assertEquals("10:05", AkmPenaltyRules.pillLabel(605_000))
    }

    @Test
    fun `the dialog and the footer keep the legacy words`() {
        assertEquals("Penalti Ujian", AkmPenaltyRules.DIALOG_TITLE)
        assertEquals("Kamu tidak dapat melanjutkan ujian sampai waktu penalti habis.", AkmPenaltyRules.DIALOG_MESSAGE)
        assertEquals("Laporkan Masalah", AkmPenaltyRules.REPORT_LINK)
        assertEquals("v1.0.0 • Android 11 (API 30)", AkmPenaltyRules.footer("1.0.0", "11", 30))
    }

    @Test
    fun `a penalty carried over from the last session has no cause card`() {
        assertFalse(AkmPenaltyRules.showCause(null))
        assertFalse(AkmPenaltyRules.showCause("  "))
        assertTrue(AkmPenaltyRules.showCause("Terdeteksi keluar"))
    }
}
