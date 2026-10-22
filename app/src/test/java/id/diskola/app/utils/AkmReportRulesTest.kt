package id.diskola.app.utils

import id.diskola.app.utils.AkmReportRules.ViolationLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmReportRulesTest {

    // ---- the form ----

    @Test
    fun `send needs words, the checkbox and no cooldown`() {
        assertTrue(AkmReportRules.canSubmit("aplikasi macet", confirmed = true, cooldownSecondsLeft = 0))
        assertFalse(AkmReportRules.canSubmit("", confirmed = true, cooldownSecondsLeft = 0))
        assertFalse(AkmReportRules.canSubmit("   \n ", confirmed = true, cooldownSecondsLeft = 0))
        assertFalse(AkmReportRules.canSubmit("aplikasi macet", confirmed = false, cooldownSecondsLeft = 0))
        assertFalse(AkmReportRules.canSubmit("aplikasi macet", confirmed = true, cooldownSecondsLeft = 12))
    }

    @Test
    fun `the button counts the cooldown down in its label`() {
        assertEquals("Kirim Laporan", AkmReportRules.sendLabel(0))
        assertEquals("Tunggu 42 detik lagi", AkmReportRules.sendLabel(42))
    }

    @Test
    fun `the text is cut at 500 characters and the counter says so`() {
        assertEquals(500, AkmReportRules.clampMessage("x".repeat(900)).length)
        assertEquals("abc", AkmReportRules.clampMessage("abc"))
        assertEquals("3/500", AkmReportRules.counter("abc"))
    }

    @Test
    fun `one report a minute for the whole app`() {
        val sent = 1_000_000L
        assertEquals(0, AkmReportRules.cooldownSecondsLeft(0L, sent))
        assertEquals(60, AkmReportRules.cooldownSecondsLeft(sent, sent))
        assertEquals(31, AkmReportRules.cooldownSecondsLeft(sent, sent + 29_001L))
        assertEquals(1, AkmReportRules.cooldownSecondsLeft(sent, sent + 59_999L))
        assertEquals(0, AkmReportRules.cooldownSecondsLeft(sent, sent + 60_000L))
    }

    @Test
    fun `a clock moved back does not lock the button for more than a minute`() {
        assertEquals(60, AkmReportRules.cooldownSecondsLeft(1_000_000L, 1_000_000L - 3_600_000L))
    }

    @Test
    fun `the form words are the legacy ones`() {
        assertEquals("Ceritakan kendalamu", AkmReportRules.LABEL)
        assertEquals("Tulis kendala yang kamu alami…", AkmReportRules.HINT)
        assertTrue(AkmReportRules.CONFIRM_TEXT.startsWith("Saya menyatakan bahwa masalah ini benar-benar terjadi karena kesalahan sistem/aplikasi"))
        assertEquals("Laporan tersimpan di perangkatmu dan akan dikirim otomatis saat tersedia.", AkmReportRules.SAVED_TOAST)
        assertEquals(3, AkmReportRules.MIN_LINES)
    }

    // ---- the log text ----

    @Test
    fun `the log names the schedule, the session, the device and the violations`() {
        val log = AkmReportRules.buildLog(
            examId = 312873,
            studentExamId = 9981,
            nowIso = "2026-10-16T09:30:12+07:00",
            deviceName = "Samsung SM-A505F",
            androidVersion = "Android 11 (API 30)",
            appVersion = "1.0.0",
            violations = listOf(
                ViolationLine("2026-10-16T09:29:00+07:00", "app_background", "Terdeteksi keluar dari halaman ujian atau berpindah aplikasi.", "{\"durationSeconds\":12}"),
                ViolationLine("2026-10-16T09:29:30+07:00", "screenshot", "Terdeteksi mengambil tangkapan layar (screenshot).", null),
            ),
        )
        assertEquals(
            "Penyebab penalti (AKM)\n" +
                "Jadwal: 312873 | Sesi: 9981 | Waktu: 2026-10-16T09:30:12+07:00\n" +
                "Perangkat: Samsung SM-A505F | OS: Android 11 (API 30) | App: 1.0.0\n\n" +
                "Pelanggaran tercatat sesi ini:\n" +
                "- [2026-10-16T09:29:00+07:00] app_background — Terdeteksi keluar dari halaman ujian atau berpindah aplikasi. {\"durationSeconds\":12}\n" +
                "- [2026-10-16T09:29:30+07:00] screenshot — Terdeteksi mengambil tangkapan layar (screenshot).",
            log,
        )
    }

    @Test
    fun `with no violations the log says the penalty is active without a specific cause`() {
        val log = AkmReportRules.buildLog(1, 2, "T", "d", "a", "v", emptyList())
        assertTrue(log.endsWith("Tidak ada pelanggaran spesifik tercatat; penalti aktif."))
    }

    @Test
    fun `an enormous log is cut at 200000 characters`() {
        val many = (1..5_000).map { ViolationLine("2026-10-16T09:29:00+07:00", "app_background", "x".repeat(100), null) }
        assertEquals(AkmReportRules.MAX_LOGCAT, AkmReportRules.buildLog(1, 2, "T", "d", "a", "v", many).length)
    }

    // ---- the payload ----

    @Test
    fun `only the four legacy fields are sent, each within its limit`() {
        val payload = AkmReportRules.payload("v".repeat(100), "a".repeat(50), "d".repeat(300), "l".repeat(300_000))
        assertEquals(setOf("appVersion", "androidVersion", "deviceName", "logcat"), payload.keys)
        assertEquals(64, (payload["appVersion"] as String).length)
        assertEquals(32, (payload["androidVersion"] as String).length)
        assertEquals(128, (payload["deviceName"] as String).length)
        assertEquals(200_000, (payload["logcat"] as String).length)
    }

    @Test
    fun `credentials in the log text never leave the phone`() {
        val payload = AkmReportRules.payload("1.0", "Android 11", "d", "x {\"password\":\"rahasia\"} y token=abc123")
        val log = payload["logcat"] as String
        assertFalse(log.contains("rahasia"))
        assertFalse(log.contains("abc123"))
    }
}
