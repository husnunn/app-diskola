package id.diskola.app.utils

import id.diskola.app.apiservice.ApiException
import id.diskola.app.dataclass.akm.AkmStatus
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import retrofit2.HttpException

/**
 * Pure rules of the school-exam module (doc `06-asesmen-akm-tryout-ujian.md`): status precedence, what blocks a logout, how a failed
 * upload is classified, the deadline and re-upload arithmetic. No Android, no network — every rule has a JVM test.
 */
object AkmRules {

    // ---- Numbers carried over from legacy (`AkmUploader`, `AkmScorePage`) ----

    /** A queued answer is retried up to this many times before it is left for a manual re-upload. */
    const val MAX_ATTEMPTS = 10

    /** Linear backoff: the n-th retry waits `n * BACKOFF_STEP_MS`. */
    const val BACKOFF_STEP_MS = 10_000L

    /** An auto-submit that wakes up this close to the deadline (or later) goes out now instead of waiting again. */
    const val FORCE_SUBMIT_GRACE_MS = 5_000L

    /** After a re-upload attempt the button stays locked this long, whatever the outcome. */
    const val REUPLOAD_COOLDOWN_MS = 60_000L

    // ---- Local submission state (kept in `akm_submission`) ----

    object SubmissionState {
        /** Asked to be sent (manually or at the deadline), not confirmed by the server yet. */
        const val QUEUED = "QUEUED"

        /** The server accepted the answers. */
        const val SENT = "SENT"

        /** The server refused them for good (a 4xx that retrying cannot fix). */
        const val REJECTED = "REJECTED"
    }

    /** What a download row and its submission row say about one exam on this device. */
    data class LocalExam(val downloadStatus: String, val studentExamId: Int, val submissionState: String?)

    /**
     * Whether answers of this exam may still be on their way — the one predicate behind every logout block (doc 06 §15, decision
     * 2026-09-30 "jangan logout"). A downloaded exam counts from the moment its session exists (`studentExamId > 0`), submitted
     * or not; [SubmissionState.REJECTED] is the server's final no, so it no longer blocks (it is shown to the student instead).
     */
    fun isPending(exam: LocalExam): Boolean = when (exam.submissionState) {
        SubmissionState.SENT, SubmissionState.REJECTED -> false
        SubmissionState.QUEUED -> true
        else -> exam.downloadStatus == DOWNLOADED && exam.studentExamId > 0
    }

    const val DOWNLOADING = "DOWNLOADING"
    const val DOWNLOADED = "DOWNLOADED"

    // ---- Status ----

    /**
     * The server's own `status` string wins when it is present (only the scored endpoint sends it). `isDone` marks the *session
     * window* as closed, not that the student submitted — 42 of 54 live rows had `isDone=true` while `status` said the student
     * never took the exam — so it only ranks as "waiting for a score" when nothing more specific is known.
     */
    fun serverStatus(
        status: String,
        isAssessed: Boolean,
        isQueued: Boolean,
        isDone: Boolean,
    ): AkmStatus = when {
        status.equals("COMPLETED", ignoreCase = true) -> AkmStatus.SudahDinilai
        status.equals("Sesi Terlewat", ignoreCase = true) -> AkmStatus.SesiTerlewat
        status.equals("Tidak Mengerjakan", ignoreCase = true) -> AkmStatus.TidakMengerjakan
        isAssessed -> AkmStatus.SudahDinilai
        isQueued -> AkmStatus.SudahDikumpulkan
        isDone -> AkmStatus.MenungguPenilaian
        else -> AkmStatus.BelumSinkron
    }

    /** What this device alone knows: downloading, ready, waiting to upload, uploaded. */
    fun localStatus(local: LocalExam?): AkmStatus = when {
        local == null -> AkmStatus.BelumSinkron
        local.submissionState == SubmissionState.SENT -> AkmStatus.SudahDikumpulkan
        local.submissionState == SubmissionState.QUEUED -> AkmStatus.ProsesUpload
        local.downloadStatus == DOWNLOADING -> AkmStatus.MengunduhSoal
        local.downloadStatus == DOWNLOADED -> AkmStatus.SiapDikerjakan
        else -> AkmStatus.BelumSinkron
    }

    /**
     * Legacy merge (`OnKlasDbUtil.kt:924-932`): the server never gets downgraded by an older local state, and a local state that is
     * further along (answers uploaded) is not erased by a list that has not caught up yet — the higher rank wins.
     */
    fun effectiveStatus(server: AkmStatus, local: LocalExam?): AkmStatus {
        // `isDone` alone ("window closed") says nothing about this device's answers: while they are still waiting to be sent the exam
        // must not read as already handed in.
        if (server == AkmStatus.MenungguPenilaian && local != null && isPending(local)) return AkmStatus.ProsesUpload
        val fromLocal = localStatus(local)
        return if (rank(fromLocal) > rank(server)) fromLocal else server
    }

    private fun rank(status: AkmStatus): Int = when (status) {
        AkmStatus.BelumSinkron -> 0
        AkmStatus.MengunduhSoal -> 1
        AkmStatus.SiapDikerjakan -> 2
        AkmStatus.ProsesUpload -> 3
        AkmStatus.SudahDikumpulkan -> 4
        AkmStatus.MenungguPenilaian -> 5
        AkmStatus.SudahDinilai, AkmStatus.TidakMengerjakan, AkmStatus.SesiTerlewat -> 6
    }

    /**
     * The server holds this student's answers: it queued them (`isQueued`) or graded them. `isDone` alone is **not** proof (it only
     * means the window closed), and neither are the "never took it" verdicts — closing a local queue on those would drop answers.
     */
    fun serverAlreadyHasAnswers(status: AkmStatus): Boolean =
        status == AkmStatus.SudahDikumpulkan || status == AkmStatus.SudahDinilai

    // ---- Failure classification (upload worker) ----

    sealed interface Failure {
        /** Worth another try (offline, timeout, 5xx, 429). */
        data object Retry : Failure

        /** 401: nothing can be sent until the user signs in again — keep trying, but do not use up the attempts. */
        data object AuthExpired : Failure

        /** The server said no and will say no again. */
        data class Permanent(val message: String) : Failure
    }

    fun classifyFailure(error: Throwable): Failure = when (error) {
        is ApiException -> classifyCode(error.responseCode, error.message)
        is HttpException -> classifyCode(error.code(), error.message())
        is IOException -> Failure.Retry
        // An exception this module does not know is more likely a transient glitch than a verdict: try again, the cap ends it.
        else -> Failure.Retry
    }

    private fun classifyCode(code: Int?, message: String?): Failure = when {
        code == null || code == 0 -> Failure.Retry // offline: the request interceptor throws before sending
        code == 401 -> Failure.AuthExpired
        code == 408 || code == 429 || code >= 500 -> Failure.Retry
        code in 400..499 -> Failure.Permanent(message.orEmpty().ifBlank { "Jawaban ditolak server (kode $code)" })
        else -> Failure.Retry
    }

    /** Whether another attempt is allowed after [attempts] have already been made. */
    fun mayRetry(attempts: Int): Boolean = attempts < MAX_ATTEMPTS

    fun backoffMillis(attempt: Int): Long = BACKOFF_STEP_MS * attempt.coerceAtLeast(1)

    // ---- Deadline ----

    sealed interface DeadlineAction {
        data object Submit : DeadlineAction

        /** Too early (the worker was woken ahead of time): look again after [delayMillis]. */
        data class Reschedule(val delayMillis: Long) : DeadlineAction

        /** The end time is unknown: never auto-submit on a guess. */
        data object Skip : DeadlineAction
    }

    /**
     * Legacy `AkmUploader.doWork`: a manual submit always goes; a deadline run goes only once within the grace of the end time,
     * otherwise it reschedules. Unlike legacy, an end time that could not be read does **not** fall back to "now" (that forced an
     * immediate submit of an exam that might not have ended).
     */
    fun deadlineAction(nowMillis: Long, endAtMillis: Long?, manual: Boolean): DeadlineAction {
        if (manual) return DeadlineAction.Submit
        if (endAtMillis == null) return DeadlineAction.Skip
        val remaining = endAtMillis - nowMillis
        return if (remaining > FORCE_SUBMIT_GRACE_MS) DeadlineAction.Reschedule(remaining) else DeadlineAction.Submit
    }

    /** `date` + `endAt` (`yyyy-MM-dd` + `HH:mm`) in [timeZone]; null when either is missing or malformed. */
    fun parseDateTime(date: String, time: String, timeZone: TimeZone = TimeZone.getDefault()): Long? {
        if (date.isBlank() || time.isBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                this.timeZone = timeZone
                isLenient = false
            }.parse("$date $time")?.time
        } catch (e: Exception) {
            null
        }
    }

    /** Full-timestamp comparison (legacy compared only HH:mm on the detail page, which is wrong across days). */
    fun isTimeUp(nowMillis: Long, endAtMillis: Long?): Boolean = endAtMillis != null && nowMillis >= endAtMillis

    // ---- Re-upload (score screen) ----

    fun cooldownSecondsLeft(nowMillis: Long, untilMillis: Long): Int =
        if (untilMillis <= nowMillis) 0 else ((untilMillis - nowMillis + 999) / 1000).toInt()

    /** The button shows for an upload that is waiting or done, except for exams the student never took. */
    fun canReupload(status: AkmStatus, scoreStatus: String): Boolean {
        if (status != AkmStatus.ProsesUpload && status != AkmStatus.SudahDikumpulkan && status != AkmStatus.MenungguPenilaian) return false
        val text = scoreStatus.lowercase()
        return !text.contains("tidak mengerjakan") && !text.contains("sesi terlewat")
    }

    // ---- Answers ----

    /** `SHORT_ESSAY_WORD` keeps no whitespace, `SHORT_ESSAY_NUM` only digits, every other type is left as typed (`QuestionEssayVh`). */
    fun cleanEssay(answerType: String, text: String): String = when (answerType) {
        "SHORT_ESSAY_WORD" -> text.filterNot { it.isWhitespace() }
        "SHORT_ESSAY_NUM" -> text.filter { it.isDigit() }
        else -> text
    }

    /** "Kumpulkan" is enabled when every question has an answer (SCHOOL scope, doc 06 §5.5). */
    fun allAnswered(questionIds: List<Int>, answeredIds: Set<Int>): Boolean = questionIds.isNotEmpty() && questionIds.all { it in answeredIds }

    // ---- "Dibatalkan" label/stamp (doc 06 §4.4, android-portal 2.1.41) ----

    /** What the server says about a cancelled schedule: [enabled] shows the label/stamp/watermark, [label] is its text. Purely visual. */
    data class AkmWatermark(val enabled: Boolean, val label: String) {
        companion object {
            val None = AkmWatermark(enabled = false, label = "")
        }
    }

    /** `akm_watermark_default_label`: used when the server turns the watermark on without a text of its own. */
    const val DEFAULT_WATERMARK_LABEL = "DIBATALKAN"

    /**
     * Legacy merge (`OnKlasDbUtil.kt:952-960`): a response that carries `watermark_bool` wins — a schedule can be cancelled and later
     * restored; a response that has no such key (e.g. `exam-schedules-scored`) leaves the value already stored on this device alone
     * instead of resetting it to "off". [known] is that stored value.
     */
    fun watermark(serverBool: Boolean?, serverLabel: String, known: AkmWatermark?): AkmWatermark = when (serverBool) {
        true -> AkmWatermark(enabled = true, label = serverLabel.trim().ifBlank { DEFAULT_WATERMARK_LABEL })
        false -> AkmWatermark.None
        null -> known ?: AkmWatermark.None
    }

    // ---- Server switches (`GET mobile/setting-akm`, doc 06 §11.1/§17.2) ----

    /** What the app stores for a switch it has never received (legacy `-1`): both exam switches are then **on**, the safe side. */
    const val SETTING_NEVER_FETCHED = -1

    /** `penalty_times` is in **seconds**; absent or ≤ 0 means 300. */
    const val DEFAULT_PENALTY_SECONDS = 300

    /** The settings are fetched at most this often, except right before an exam starts. */
    const val SETTINGS_TTL_MS = 12L * 60 * 60 * 1000

    /**
     * Value kept for `exam_lock_mode` (legacy `SettingAkmCache.kt:69`): the DTO defaults a **missing** key to `false` (strict mode off,
     * decision 2026-09-30), while an explicit `null` reads as "on".
     */
    fun lockModeRaw(value: Boolean?): Int = if (value ?: true) 1 else 0

    /** Same shape for `penalty_applied`: missing key → `false` (off, as legacy), explicit `null` → on. */
    fun penaltyAppliedRaw(value: Boolean?): Int = if (value ?: true) 1 else 0

    /** Strict mode (screen pinning) is on unless the server said 0; never fetched (−1) is on. */
    fun isStrict(raw: Int): Boolean = raw != 0

    /** The penalty system is on unless the server said 0; never fetched (−1) is on. */
    fun isPenaltyApplied(raw: Int): Boolean = raw != 0

    fun penaltySeconds(raw: Int): Int = if (raw > 0) raw else DEFAULT_PENALTY_SECONDS

    /** Older than 12 h, never fetched, or fetched "in the future" (the clock was moved back) ⇒ fetch again. */
    fun settingsStale(lastFetchMillis: Long, nowMillis: Long): Boolean {
        if (lastFetchMillis <= 0L) return true
        val age = nowMillis - lastFetchMillis
        return age < 0 || age > SETTINGS_TTL_MS
    }

    // ---- Essay input (doc 06 §5.7, 12-10-2026) ----

    /** Every essay box shows at least this many lines (`ESSAY_MIN_LINES`, layout `minLines=8`). */
    const val ESSAY_MIN_LINES = 8

    /** Essay and one-word answers get a "Selesai" keyboard key; numeric answers keep the plain number keyboard (`QuestionEssayVh.kt:145-165`). */
    fun usesDoneAction(answerType: String): Boolean = answerType != "SHORT_ESSAY_NUM"

    private val newline = Regex("\r\n|\n|\r")

    /**
     * The legacy `noNewLineFilter`: an answer never contains a line break. A single typed Enter is dropped; a pasted block has each
     * line break replaced by one space, so it becomes one long line. [previous] is the text before the change, [next] the text after.
     */
    fun sanitizeEssayInput(previous: String, next: String): String {
        if (!next.contains('\n') && !next.contains('\r')) return next
        var prefix = 0
        val maxPrefix = minOf(previous.length, next.length)
        while (prefix < maxPrefix && previous[prefix] == next[prefix]) prefix++
        var suffix = 0
        val maxSuffix = minOf(previous.length, next.length) - prefix
        while (suffix < maxSuffix && previous[previous.length - 1 - suffix] == next[next.length - 1 - suffix]) suffix++
        val inserted = next.substring(prefix, next.length - suffix)
        val cleaned = if (inserted.length == 1 && (inserted[0] == '\n' || inserted[0] == '\r')) "" else inserted.replace(newline, " ")
        return next.substring(0, prefix) + cleaned + next.substring(next.length - suffix)
    }
}
