package id.diskola.app.utils

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import id.diskola.app.apiservice.ApiException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import retrofit2.HttpException

/**
 * What the exam session records about the student and how it is sent (doc `06` §10): the violation types, when each is recorded, the
 * wording of the message, the payload and what to do with each server answer. Pure — no Android, no network, every rule has a JVM test.
 *
 * A violation is **always recorded and sent**, whether or not the penalty system is on: the penalty only decides if a dialog is shown
 * ([triggersPenalty] is asked only after the row is stored). That is how legacy behaves with `penalty_applied = false`: leaving the
 * app is still sent to the school.
 */
object AkmViolationRules {

    // ---- Types (`AkmViolationEntities.kt:8-15`) ----

    object Type {
        const val APP_BACKGROUND = "app_background"
        const val SPLIT_SCREEN = "split_screen"
        const val FLOATING_WINDOW = "floating_window"
        const val SCREENSHOT = "screenshot"
        const val CONNECTION_LOSS = "connection_loss"
        const val TIME_TAMPERING = "time_tampering"
    }

    /** `{"reason":"not_submitted"}` — the previous session of this exam was left without handing in (doc 06 §10.2). */
    const val NOT_SUBMITTED_REASON = "not_submitted"

    // ---- Thresholds carried over from legacy ----

    /** An outage shorter than this is not reported. */
    const val MIN_CONNECTION_LOSS_SECONDS = 5L

    /** The device clock may disagree with the monotonic clock by this much (sync, drift) before it counts as a change. */
    const val TIME_TAMPER_THRESHOLD_MS = 5_000L

    /** At most one `floating_window` row per this long (an overlay produces a burst of obscured touches). */
    const val TOUCH_OBSCURED_DEBOUNCE_MS = 5_000L

    const val CHUNK_SIZE = 50
    const val DEFAULT_429_PAUSE_SECONDS = 60L
    const val BACKOFF_SERVER_ERROR_SECONDS = 30L
    const val MAX_TYPE_LENGTH = 50

    /** The safety-net worker gives up after this many retries (a later session or the foreground flush still sends the rest). */
    const val MAX_WORKER_ATTEMPTS = 10

    // ---- Which violations raise the penalty dialog, and what it says ----

    /** `connection_loss` and `time_tampering` are information for the school: they never show a dialog or start a penalty. */
    fun triggersPenalty(type: String): Boolean = when (type) {
        Type.APP_BACKGROUND, Type.SPLIT_SCREEN, Type.FLOATING_WINDOW, Type.SCREENSHOT -> true
        else -> false
    }

    fun description(type: String): String = when (type) {
        Type.APP_BACKGROUND -> "Terdeteksi keluar dari halaman ujian atau berpindah aplikasi."
        Type.SPLIT_SCREEN -> "Terdeteksi membagi layar dengan aplikasi lain."
        Type.FLOATING_WINDOW -> "Terdeteksi jendela aplikasi lain di atas layar ujian."
        Type.SCREENSHOT -> "Terdeteksi mengambil tangkapan layar (screenshot)."
        else -> "Terdeteksi aktivitas yang tidak diperbolehkan selama ujian."
    }

    /** Shown for the `not_submitted` row, which is typed `app_background`. */
    const val NOT_SUBMITTED_DESCRIPTION = "Terdeteksi keluar dari ujian tanpa mengumpulkan jawaban."

    const val PENALTY_FALLBACK_CAUSE = "Terdeteksi keluar dari halaman ujian atau ada aplikasi lain di atas layar ujian."

    /** The text of the penalty dialog's cause card. */
    fun penaltyCause(description: String): String = description + "\n\nPelanggaranmu telah dicatat dan dikirim ke pihak sekolah."

    // ---- Recording rules ----

    /**
     * Leaving the app is not recorded while the system's own pin prompt is open, or right after a deliberate exit (opening the data
     * settings, ending the session) — the system takes the screen away, not the student.
     */
    fun shouldRecordBackground(intentionalExit: Boolean, promptPending: Boolean): Boolean = !intentionalExit && !promptPending

    /** Whole seconds between leaving and coming back; under a second is 0 (a row is still recorded). */
    fun backgroundSeconds(pausedAtElapsed: Long, resumedAtElapsed: Long): Long = ((resumedAtElapsed - pausedAtElapsed) / 1000).coerceAtLeast(0)

    fun connectionLossSeconds(offlineSinceElapsed: Long, nowElapsed: Long): Long = ((nowElapsed - offlineSinceElapsed) / 1000).coerceAtLeast(0)

    fun shouldReportConnectionLoss(seconds: Long): Boolean = seconds >= MIN_CONNECTION_LOSS_SECONDS

    /**
     * The wall clock moved by a different amount than the monotonic clock did: someone changed the date or time. Both pairs are
     * `before → now`; an NTP sync of a second or two stays under [TIME_TAMPER_THRESHOLD_MS].
     */
    fun isTimeTampered(wallBefore: Long, wallNow: Long, elapsedBefore: Long, elapsedNow: Long): Boolean =
        abs((wallNow - wallBefore) - (elapsedNow - elapsedBefore)) > TIME_TAMPER_THRESHOLD_MS

    /** What the wall clock would read now had nobody touched it — the honest `deviceTimeBefore` (legacy sent the previous event's time). */
    fun expectedWallNow(wallBefore: Long, elapsedBefore: Long, elapsedNow: Long): Long = wallBefore + (elapsedNow - elapsedBefore)

    /** [lastReportedElapsed] is 0 when nothing was reported yet. */
    fun shouldReportObscured(lastReportedElapsed: Long, nowElapsed: Long): Boolean =
        lastReportedElapsed == 0L || nowElapsed - lastReportedElapsed >= TOUCH_OBSCURED_DEBOUNCE_MS

    // ---- Payload ----

    /** `2026-10-16T09:30:12.345+07:00` — what legacy sends in `detectedAt`. */
    fun isoDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun truncateType(type: String): String = type.take(MAX_TYPE_LENGTH)

    /** Metadata as the JSON text kept in Room; null when there is none (the row is sent without a `metadata` key). */
    fun metadataJson(metadata: Map<String, Any>?): String? {
        if (metadata.isNullOrEmpty()) return null
        return metadata.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
            "${quote(key)}:${if (value is Number || value is Boolean) value.toString() else quote(value.toString())}"
        }
    }

    private fun quote(text: String): String {
        val out = StringBuilder("\"")
        for (c in text) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append(String.format("\\u%04x", c.code))
                else -> out.append(c)
            }
        }
        return out.append('"').toString()
    }

    /**
     * Reads the stored metadata back for the request body. A number that was written as an integer comes back from the JSON parser as
     * a `Double` (`5.0`); it goes out as `5` again, like legacy's `JSONObject`. Unreadable metadata is left out, never fatal.
     */
    fun parseMetadata(json: String?, moshi: Moshi): Map<String, Any>? {
        if (json.isNullOrBlank()) return null
        return try {
            val adapter = moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
            val parsed = adapter.fromJson(json) ?: return null
            parsed.mapNotNull { (key, value) ->
                when (value) {
                    null -> null
                    is Double -> key to (if (value % 1.0 == 0.0 && abs(value) < 9.0E15) value.toLong() else value)
                    else -> key to value
                }
            }.toMap().ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /** One element of the JSON array the server expects: `{"type", "detectedAt", "metadata"?}`. */
    fun payloadItem(type: String, detectedAtMillis: Long, metadata: Map<String, Any>?, zone: ZoneId = ZoneId.systemDefault()): Map<String, Any> =
        buildMap {
            put("type", truncateType(type))
            put("detectedAt", isoDateTime(detectedAtMillis, zone))
            if (!metadata.isNullOrEmpty()) put("metadata", metadata)
        }

    fun <T> chunks(rows: List<T>): List<List<T>> = rows.chunked(CHUNK_SIZE)

    // ---- What a server answer means for the queue ----

    sealed interface ChunkDecision {
        /** The server will never accept this chunk (400): drop it so it cannot block the queue. */
        data object Drop : ChunkDecision

        /** Stop for [seconds]; the rows stay queued (429 with `Retry-After`, server trouble). */
        data class Pause(val seconds: Long) : ChunkDecision

        /** Keep the rows and try again next cycle (offline, timeout, anything unknown). */
        data object Retry : ChunkDecision
    }

    /**
     * 400 drops the chunk; 429 pauses for `Retry-After` (60 s when absent); 403 and 5xx pause for 30 s — legacy's 500 back-off never ran
     * because only [ApiException] was looked at while the interceptor let 500 through as an [HttpException] (doc 06 §23.3 #8), so both
     * shapes are read here. Anything else, offline included, keeps the rows.
     */
    fun classify(error: Throwable): ChunkDecision {
        val code: Int?
        val retryAfter: Long?
        when (error) {
            is ApiException -> {
                code = error.responseCode
                retryAfter = error.retryAfterSeconds
            }
            is HttpException -> {
                code = error.code()
                retryAfter = error.response()?.headers()?.get("Retry-After")?.toLongOrNull()
            }
            is IOException -> return ChunkDecision.Retry
            else -> return ChunkDecision.Retry
        }
        return when {
            code == 400 -> ChunkDecision.Drop
            code == 429 -> ChunkDecision.Pause((retryAfter ?: DEFAULT_429_PAUSE_SECONDS).coerceAtLeast(1))
            code == 403 || (code != null && code >= 500) -> ChunkDecision.Pause(BACKOFF_SERVER_ERROR_SECONDS)
            else -> ChunkDecision.Retry
        }
    }

    // ---- Multi-window ----

    /**
     * Some phones show a floating window as a window with its own id/mode in `Configuration.toString()` (Infinix XOS "Thunderback"),
     * which `isInMultiWindowMode` does not report. `mMultiWindowId` > 0, or a `mMultiWindowMode` other than normal/undefined, means one.
     */
    fun isOemFloatingConfig(configurationText: String): Boolean {
        val id = Regex("mMultiWindowId=(\\d+)").find(configurationText)?.groupValues?.get(1)?.toIntOrNull()
        if (id != null && id > 0) return true
        val mode = Regex("mMultiWindowMode=([A-Za-z_]+)").find(configurationText)?.groupValues?.get(1)?.lowercase()
        return mode != null && mode != "normal" && mode != "undefined"
    }

    /** The window's aspect ratio may differ from the display's by this fraction before the window counts as shrunk (floating/resized). */
    const val ASPECT_TOLERANCE = 0.04

    /**
     * A window that is not the size of the display (API 30+): compares aspect ratios, with the display's rotation taken into account.
     * A window whose orientation differs from the display's is a letterboxed app, not a shrunk one.
     */
    fun isWindowShrunk(windowWidth: Int, windowHeight: Int, displayWidth: Int, displayHeight: Int, displayRotated: Boolean): Boolean {
        if (windowWidth <= 0 || windowHeight <= 0 || displayWidth <= 0 || displayHeight <= 0) return false
        val shownWidth = if (displayRotated) displayHeight else displayWidth
        val shownHeight = if (displayRotated) displayWidth else displayHeight
        val windowLandscape = windowWidth >= windowHeight
        val displayLandscape = shownWidth >= shownHeight
        if (windowLandscape != displayLandscape) return false
        val windowRatio = windowWidth.toDouble() / windowHeight
        val displayRatio = shownWidth.toDouble() / shownHeight
        return abs(windowRatio - displayRatio) / displayRatio > ASPECT_TOLERANCE
    }

    const val MULTIWINDOW_TITLE = "Layar Penuh Diperlukan"
    const val MULTIWINDOW_BODY = "Ujian hanya dapat dikerjakan dalam layar penuh. Tutup layar terbagi atau jendela mengambang untuk melanjutkan."
}
