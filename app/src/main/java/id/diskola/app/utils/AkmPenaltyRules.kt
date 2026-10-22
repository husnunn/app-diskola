package id.diskola.app.utils

import kotlin.math.max
import kotlin.math.min

/**
 * The penalty of an exam session (doc `06` §11): when a violation starts one, how long it lasts, what the student sees. Pure — no clock,
 * no Android — so every rule has a JVM test.
 *
 * The penalty is stored as the **wall-clock moment it ends** (legacy kept `HH:mm:ss`, which is wrong across midnight, and restarted the
 * whole timer whenever a screen was recreated — doc 06 §23.3 #10). What is left is never more than the length it was given, so a clock
 * moved back cannot stretch it; a clock moved forward only shortens it, and moving the clock is itself recorded as a violation.
 */
object AkmPenaltyRules {

    const val DIALOG_TITLE = "Penalti Ujian"
    const val DIALOG_MESSAGE = "Kamu tidak dapat melanjutkan ujian sampai waktu penalti habis."
    const val REPORT_LINK = "Laporkan Masalah"

    /** The lobby's title while a penalty runs (legacy then goes back to "Kerjakan Ujian"; here it stays "Kerjakan Asesmen"). */
    const val RESUME_TITLE = "Penalti Ujian"
    const val QUESTION_TITLE = "Penalti"

    /** `akm_penalty_app_info`: `v%1$s • %2$s`. */
    fun footer(versionName: String, androidRelease: String, sdkInt: Int): String = "v$versionName • Android $androidRelease (API $sdkInt)"

    /** What to do with a violation (legacy `PenaltyFromOverlay`, minus the "an in-app dialog is showing" excuse — decided 2026-10-16). */
    enum class Decision {
        /** Only recorded and sent: the penalty system is off, the violation does not carry a penalty, or the system caused it. */
        None,

        /** Start a penalty and show the dialog. */
        Start,

        /** A penalty is running: the one dialog just shows the newest cause. */
        UpdateCause,
    }

    fun decide(penaltyOn: Boolean, triggersPenalty: Boolean, alreadyActive: Boolean, systemOwned: Boolean): Decision = when {
        !penaltyOn || !triggersPenalty -> Decision.None
        alreadyActive -> Decision.UpdateCause
        systemOwned -> Decision.None
        else -> Decision.Start
    }

    fun endAt(nowWallMillis: Long, seconds: Int): Long = nowWallMillis + AkmRules.penaltySeconds(seconds) * 1000L

    /** 0 when nothing is stored or it is over; never above [totalMillis]. */
    fun remainingMillis(endAtWallMillis: Long, nowWallMillis: Long, totalMillis: Long): Long {
        if (endAtWallMillis <= 0L || totalMillis <= 0L) return 0L
        return min(max(endAtWallMillis - nowWallMillis, 0L), totalMillis)
    }

    /** `m:ss` for the pill in the header ("4:59"); a part of a second counts as the whole second. */
    fun pillLabel(remainingMillis: Long): String {
        val seconds = ((max(remainingMillis, 0L) + 999) / 1000).toInt()
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    /** The cause card is hidden when there is no cause (a penalty carried over from the last session). */
    fun showCause(cause: String?): Boolean = !cause.isNullOrBlank()
}
