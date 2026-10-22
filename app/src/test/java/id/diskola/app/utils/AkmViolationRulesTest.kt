package id.diskola.app.utils

import id.diskola.app.apiservice.ApiException
import id.diskola.app.di.module.MoshiModule
import id.diskola.app.utils.AkmViolationRules.ChunkDecision
import id.diskola.app.utils.AkmViolationRules.Type
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.ZoneId
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AkmViolationRulesTest {

    private val moshi = MoshiModule.instance()

    // ---- penalty vs recording ----

    @Test
    fun `leaving the app, a split screen, an overlay and a screenshot may raise the penalty dialog`() {
        listOf(Type.APP_BACKGROUND, Type.SPLIT_SCREEN, Type.FLOATING_WINDOW, Type.SCREENSHOT).forEach {
            assertTrue(it, AkmViolationRules.triggersPenalty(it))
        }
    }

    @Test
    fun `a lost connection and a changed clock are information only`() {
        assertFalse(AkmViolationRules.triggersPenalty(Type.CONNECTION_LOSS))
        assertFalse(AkmViolationRules.triggersPenalty(Type.TIME_TAMPERING))
        assertFalse(AkmViolationRules.triggersPenalty("something_new"))
    }

    @Test
    fun `descriptions and the cause card keep the legacy words`() {
        assertEquals("Terdeteksi keluar dari halaman ujian atau berpindah aplikasi.", AkmViolationRules.description(Type.APP_BACKGROUND))
        assertEquals("Terdeteksi membagi layar dengan aplikasi lain.", AkmViolationRules.description(Type.SPLIT_SCREEN))
        assertEquals("Terdeteksi jendela aplikasi lain di atas layar ujian.", AkmViolationRules.description(Type.FLOATING_WINDOW))
        assertEquals("Terdeteksi mengambil tangkapan layar (screenshot).", AkmViolationRules.description(Type.SCREENSHOT))
        assertEquals("Terdeteksi aktivitas yang tidak diperbolehkan selama ujian.", AkmViolationRules.description(Type.CONNECTION_LOSS))
        assertEquals(
            "Terdeteksi keluar dari ujian tanpa mengumpulkan jawaban.\n\nPelanggaranmu telah dicatat dan dikirim ke pihak sekolah.",
            AkmViolationRules.penaltyCause(AkmViolationRules.NOT_SUBMITTED_DESCRIPTION),
        )
    }

    // ---- recording rules ----

    @Test
    fun `leaving is not recorded while the pin prompt is open or right after a deliberate exit`() {
        assertTrue(AkmViolationRules.shouldRecordBackground(intentionalExit = false, promptPending = false))
        assertFalse(AkmViolationRules.shouldRecordBackground(intentionalExit = true, promptPending = false))
        assertFalse(AkmViolationRules.shouldRecordBackground(intentionalExit = false, promptPending = true))
    }

    @Test
    fun `time away is counted in whole seconds and a quick trip is a zero, not a skipped row`() {
        assertEquals(0L, AkmViolationRules.backgroundSeconds(1_000, 1_900))
        assertEquals(12L, AkmViolationRules.backgroundSeconds(1_000, 13_999))
        assertEquals(0L, AkmViolationRules.backgroundSeconds(5_000, 4_000))
    }

    @Test
    fun `an outage is reported from five seconds on`() {
        assertFalse(AkmViolationRules.shouldReportConnectionLoss(AkmViolationRules.connectionLossSeconds(0, 4_999)))
        assertTrue(AkmViolationRules.shouldReportConnectionLoss(AkmViolationRules.connectionLossSeconds(0, 5_000)))
        assertEquals(61L, AkmViolationRules.connectionLossSeconds(10_000, 71_500))
    }

    @Test
    fun `a clock that moved differently from the monotonic clock is tampering, a small drift is not`() {
        // 60 s of real time passed, but the wall clock jumped an hour ahead
        assertTrue(AkmViolationRules.isTimeTampered(wallBefore = 1_000_000, wallNow = 1_000_000 + 3_660_000, elapsedBefore = 0, elapsedNow = 60_000))
        // the wall clock went back
        assertTrue(AkmViolationRules.isTimeTampered(1_000_000, 900_000, 0, 60_000))
        // a sync of 2 s
        assertFalse(AkmViolationRules.isTimeTampered(1_000_000, 1_000_000 + 62_000, 0, 60_000))
        // exactly at the threshold is still fine
        assertFalse(AkmViolationRules.isTimeTampered(1_000_000, 1_000_000 + 65_000, 0, 60_000))
        assertTrue(AkmViolationRules.isTimeTampered(1_000_000, 1_000_000 + 65_001, 0, 60_000))
    }

    @Test
    fun `deviceTimeBefore is where the clock would be, not the last event's time`() {
        assertEquals(1_060_000L, AkmViolationRules.expectedWallNow(wallBefore = 1_000_000, elapsedBefore = 0, elapsedNow = 60_000))
    }

    @Test
    fun `obscured touches are reported once per five seconds`() {
        assertTrue(AkmViolationRules.shouldReportObscured(0, 100))
        assertFalse(AkmViolationRules.shouldReportObscured(1_000, 5_999))
        assertTrue(AkmViolationRules.shouldReportObscured(1_000, 6_000))
    }

    // ---- payload ----

    @Test
    fun `detectedAt is an ISO offset timestamp in the device zone`() {
        val zone = ZoneId.of("Asia/Jakarta")
        assertEquals("1970-01-01T07:00:00+07:00", AkmViolationRules.isoDateTime(0, zone))
        assertEquals("2026-10-16T09:30:12.345+07:00", AkmViolationRules.isoDateTime(1_792_117_812_345L, zone))
    }

    @Test
    fun `metadata round trips and integers stay integers`() {
        val json = AkmViolationRules.metadataJson(mapOf("durationSeconds" to 12L, "reason" to "not_submitted"))
        assertEquals("{\"durationSeconds\":12,\"reason\":\"not_submitted\"}", json)
        val back = AkmViolationRules.parseMetadata(json, moshi)!!
        assertEquals(12L, back["durationSeconds"])
        assertEquals("not_submitted", back["reason"])
    }

    @Test
    fun `metadata with quotes, newlines and unicode survives`() {
        val tricky = mapOf("note" to "a \"quoted\" line\nnext\\ \u0001")
        val back = AkmViolationRules.parseMetadata(AkmViolationRules.metadataJson(tricky), moshi)!!
        assertEquals(tricky["note"], back["note"])
    }

    @Test
    fun `no metadata means none is stored and none is sent`() {
        assertNull(AkmViolationRules.metadataJson(null))
        assertNull(AkmViolationRules.metadataJson(emptyMap()))
        assertNull(AkmViolationRules.parseMetadata(null, moshi))
        assertNull(AkmViolationRules.parseMetadata("not json", moshi))
        assertFalse(AkmViolationRules.payloadItem(Type.SCREENSHOT, 0, null, ZoneId.of("UTC")).containsKey("metadata"))
    }

    @Test
    fun `a payload item carries type, detectedAt and metadata`() {
        val item = AkmViolationRules.payloadItem(Type.APP_BACKGROUND, 0, mapOf("durationSeconds" to 3L), ZoneId.of("UTC"))
        assertEquals("app_background", item["type"])
        assertEquals("1970-01-01T00:00:00Z", item["detectedAt"])
        assertEquals(mapOf("durationSeconds" to 3L), item["metadata"])
    }

    @Test
    fun `a type longer than 50 characters is cut`() {
        assertEquals(50, AkmViolationRules.truncateType("x".repeat(80)).length)
    }

    @Test
    fun `rows are sent in chunks of fifty`() {
        assertEquals(listOf(50, 50, 20), AkmViolationRules.chunks((1..120).toList()).map { it.size })
        assertTrue(AkmViolationRules.chunks(emptyList<Int>()).isEmpty())
    }

    // ---- server answers ----

    private fun api(code: Int?, retryAfter: Long? = null) = ApiException("x", code, retryAfterSeconds = retryAfter)

    private fun http(code: Int, retryAfter: String? = null): HttpException {
        val builder = okhttp3.Response.Builder()
            .code(code)
            .message("x")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .request(okhttp3.Request.Builder().url("http://localhost/").build())
        if (retryAfter != null) builder.header("Retry-After", retryAfter)
        val raw = builder.build()
        return HttpException(Response.error<Any>("".toResponseBody("application/json".toMediaType()), raw))
    }

    @Test
    fun `a 400 drops the chunk`() = assertEquals(ChunkDecision.Drop, AkmViolationRules.classify(api(400)))

    @Test
    fun `a 429 pauses for Retry-After, a minute when it is missing`() {
        assertEquals(ChunkDecision.Pause(17), AkmViolationRules.classify(api(429, retryAfter = 17)))
        assertEquals(ChunkDecision.Pause(60), AkmViolationRules.classify(api(429)))
        assertEquals(ChunkDecision.Pause(9), AkmViolationRules.classify(http(429, "9")))
    }

    @Test
    fun `server trouble pauses thirty seconds whichever way the error arrives`() {
        assertEquals(ChunkDecision.Pause(30), AkmViolationRules.classify(api(500)))
        assertEquals(ChunkDecision.Pause(30), AkmViolationRules.classify(api(503)))
        assertEquals(ChunkDecision.Pause(30), AkmViolationRules.classify(api(403)))
        // legacy never paused here: the interceptor lets 403/500 through as an HttpException
        assertEquals(ChunkDecision.Pause(30), AkmViolationRules.classify(http(500)))
        assertEquals(ChunkDecision.Pause(30), AkmViolationRules.classify(http(403)))
    }

    @Test
    fun `offline, a timeout and anything unknown keep the rows for the next cycle`() {
        assertEquals(ChunkDecision.Retry, AkmViolationRules.classify(IOException("offline")))
        assertEquals(ChunkDecision.Retry, AkmViolationRules.classify(SocketTimeoutException()))
        assertEquals(ChunkDecision.Retry, AkmViolationRules.classify(api(null)))
        assertEquals(ChunkDecision.Retry, AkmViolationRules.classify(api(401)))
        assertEquals(ChunkDecision.Retry, AkmViolationRules.classify(IllegalStateException("?")))
    }

    // ---- multi-window ----

    @Test
    fun `an OEM floating window is read from the configuration text`() {
        assertTrue(AkmViolationRules.isOemFloatingConfig("{1.0 ... mMultiWindowId=3 ...}"))
        assertTrue(AkmViolationRules.isOemFloatingConfig("{... mMultiWindowMode=FLOATING ...}"))
        assertFalse(AkmViolationRules.isOemFloatingConfig("{... mMultiWindowId=0 mMultiWindowMode=normal ...}"))
        assertFalse(AkmViolationRules.isOemFloatingConfig("{... mMultiWindowMode=UNDEFINED ...}"))
        assertFalse(AkmViolationRules.isOemFloatingConfig("{... no such keys ...}"))
    }

    @Test
    fun `a window the size of the display is not shrunk, a half-height window is`() {
        assertFalse(AkmViolationRules.isWindowShrunk(1080, 2340, 1080, 2340, displayRotated = false))
        assertFalse(AkmViolationRules.isWindowShrunk(1080, 2280, 1080, 2340, displayRotated = false))
        assertTrue(AkmViolationRules.isWindowShrunk(1080, 1170, 1080, 2340, displayRotated = false))
    }

    @Test
    fun `the display mode is taken in the orientation the display is shown in`() {
        // physical mode 1080x2340, display rotated to landscape: a 2340x1080 window is full
        assertFalse(AkmViolationRules.isWindowShrunk(2340, 1080, 1080, 2340, displayRotated = true))
        assertTrue(AkmViolationRules.isWindowShrunk(1500, 1080, 1080, 2340, displayRotated = true))
    }

    @Test
    fun `a letterboxed window of the other orientation is not counted as shrunk, and unknown sizes are never shrunk`() {
        assertFalse(AkmViolationRules.isWindowShrunk(1000, 600, 1080, 2340, displayRotated = false))
        assertFalse(AkmViolationRules.isWindowShrunk(0, 0, 1080, 2340, displayRotated = false))
        assertNotNull(AkmViolationRules.MULTIWINDOW_TITLE)
    }

    @Test
    fun `the blocker words and the numbers are the legacy ones`() {
        assertEquals("Layar Penuh Diperlukan", AkmViolationRules.MULTIWINDOW_TITLE)
        assertTrue(AkmViolationRules.MULTIWINDOW_BODY.startsWith("Ujian hanya dapat dikerjakan dalam layar penuh."))
        assertEquals(5L, AkmViolationRules.MIN_CONNECTION_LOSS_SECONDS)
        assertEquals(5_000L, AkmViolationRules.TIME_TAMPER_THRESHOLD_MS)
        assertEquals(5_000L, AkmViolationRules.TOUCH_OBSCURED_DEBOUNCE_MS)
        assertEquals(50, AkmViolationRules.CHUNK_SIZE)
        assertEquals(0.04, AkmViolationRules.ASPECT_TOLERANCE, 0.0)
    }
}
