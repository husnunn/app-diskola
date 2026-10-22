package id.diskola.app.utils

/**
 * "Laporkan Masalah" (doc `06` §13): who may press send, what is sent, what is written into the log text. Pure — the device facts and
 * the clock are passed in.
 *
 * Like legacy only four fields leave the phone: `appVersion`, `androidVersion`, `deviceName` and `logcat`. The student's own words, the
 * checkbox and the violations are stored with the report but **not sent** — there is no backend field for them yet (doc 06 §23.3 #7,
 * `FLOW_QUESTIONS.md` 2026-10-16). When there is, only [payload] changes.
 */
object AkmReportRules {

    const val SUBTITLE = "Ceritakan kendala yang kamu alami. Riwayat pelanggaran sesi ujian ini akan disertakan otomatis."
    const val LABEL = "Ceritakan kendalamu"
    const val HINT = "Tulis kendala yang kamu alami…"
    const val CONFIRM_TEXT =
        "Saya menyatakan bahwa masalah ini benar-benar terjadi karena kesalahan sistem/aplikasi, bukan karena saya sengaja melakukan kecurangan selama ujian."
    const val SEND = "Kirim Laporan"
    const val CANCEL = "Batal"
    const val SAVED_TOAST = "Laporan tersimpan di perangkatmu dan akan dikirim otomatis saat tersedia."

    const val MAX_MESSAGE = 500
    const val MIN_LINES = 3

    /** One report a minute for the whole app, whichever exam. */
    const val COOLDOWN_MS = 60_000L

    const val MAX_APP_VERSION = 64
    const val MAX_ANDROID_VERSION = 32
    const val MAX_DEVICE_NAME = 128
    const val MAX_LOGCAT = 200_000

    const val NO_VIOLATIONS_TEXT = "Tidak ada pelanggaran spesifik tercatat; penalti aktif."

    fun clampMessage(text: String): String = text.take(MAX_MESSAGE)

    fun counter(text: String): String = "${text.length}/$MAX_MESSAGE"

    fun cooldownSecondsLeft(lastSubmitAtWall: Long, nowWall: Long): Int {
        if (lastSubmitAtWall <= 0L) return 0
        val left = lastSubmitAtWall + COOLDOWN_MS - nowWall
        // A clock moved back must not lock the button for longer than the minute itself.
        return if (left <= 0L) 0 else (minOf(left, COOLDOWN_MS) + 999).div(1000).toInt()
    }

    fun canSubmit(message: String, confirmed: Boolean, cooldownSecondsLeft: Int): Boolean =
        message.isNotBlank() && confirmed && cooldownSecondsLeft <= 0

    fun sendLabel(cooldownSecondsLeft: Int): String = if (cooldownSecondsLeft > 0) "Tunggu $cooldownSecondsLeft detik lagi" else SEND

    /** One violation row as the log text shows it. */
    data class ViolationLine(val detectedAtIso: String, val type: String, val description: String, val metadataJson: String?)

    /** The `logcat` text of the report (doc 06 §13 step 1), cut at [MAX_LOGCAT]. */
    fun buildLog(
        examId: Int,
        studentExamId: Int,
        nowIso: String,
        deviceName: String,
        androidVersion: String,
        appVersion: String,
        violations: List<ViolationLine>,
    ): String {
        val head = "Penyebab penalti (AKM)\n" +
            "Jadwal: $examId | Sesi: $studentExamId | Waktu: $nowIso\n" +
            "Perangkat: $deviceName | OS: $androidVersion | App: $appVersion\n\n"
        val body = if (violations.isEmpty()) {
            NO_VIOLATIONS_TEXT
        } else {
            "Pelanggaran tercatat sesi ini:\n" + violations.joinToString("\n") { line ->
                "- [${line.detectedAtIso}] ${line.type} — ${line.description}" + (line.metadataJson?.let { " $it" } ?: "")
            }
        }
        return (head + body).take(MAX_LOGCAT)
    }

    /** The body of `POST …/error-reports/{student_exam_id}`: exactly the four legacy fields, each cut to its limit, the log redacted. */
    fun payload(appVersion: String, androidVersion: String, deviceName: String, logcat: String): Map<String, Any> = mapOf(
        "appVersion" to appVersion.take(MAX_APP_VERSION),
        "androidVersion" to androidVersion.take(MAX_ANDROID_VERSION),
        "deviceName" to deviceName.take(MAX_DEVICE_NAME),
        "logcat" to LogRedactor.redact(logcat).take(MAX_LOGCAT),
    )
}
