package id.diskola.app.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small per-exam flags that must survive a killed process and must be on disk *before* the next line of code runs — hence
 * `commit()` and a file of their own, which is excluded from auto-backup (`backup_rules.xml`, `data_extraction_rules.xml`): a marker
 * restored onto another phone would claim an exam session that never ran there.
 *
 * - **session marker** (`exam_session_active_{id}`, doc 06 §10.2): set when a session starts, cleared only when the exam is handed in
 *   ([AkmSessionRules.EndReason.completes]). Still set at the next start ⇒ the exam was left without handing in.
 * - **penalty** (`penalty_end_{id}` wall-clock ms, `penalty_total_{id}` ms): survives a killed process; cleared when the exam is handed in.
 * - **report cooldown** (`problem_report_last_submit_at`): one "Laporkan Masalah" a minute for the whole app.
 * - **password override** (`password_checked_{id}`): the student passed the password dialog on this device; cleared when a screen of
 *   the exam is found without a live session (process restored), so the password is asked again.
 */
@Singleton
class AkmExamState @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isSessionActive(examId: Int): Boolean = prefs.getBoolean(sessionKey(examId), false)

    fun setSessionActive(examId: Int, active: Boolean) {
        prefs.edit().putBoolean(sessionKey(examId), active).commit()
    }

    /** The exam was handed in (or the answers were queued): the session is closed properly and no penalty is left over. */
    fun markHandedIn(examId: Int) {
        prefs.edit().putBoolean(sessionKey(examId), false).remove(penaltyEndKey(examId)).remove(penaltyTotalKey(examId)).commit()
    }

    fun penaltyEnd(examId: Int): Long = prefs.getLong(penaltyEndKey(examId), 0L)

    fun penaltyTotalMillis(examId: Int): Long = prefs.getLong(penaltyTotalKey(examId), 0L)

    fun setPenalty(examId: Int, endAtWallMillis: Long, totalMillis: Long) {
        prefs.edit().putLong(penaltyEndKey(examId), endAtWallMillis).putLong(penaltyTotalKey(examId), totalMillis).commit()
    }

    fun clearPenalty(examId: Int) {
        prefs.edit().remove(penaltyEndKey(examId)).remove(penaltyTotalKey(examId)).commit()
    }

    fun problemReportLastSubmitAt(): Long = prefs.getLong(KEY_REPORT_LAST, 0L)

    fun setProblemReportLastSubmitAt(wallMillis: Long) {
        prefs.edit().putLong(KEY_REPORT_LAST, wallMillis).commit()
    }

    /** `null` when this device has no opinion yet (the server's `password_checked` then decides). */
    fun passwordChecked(examId: Int): Boolean? =
        if (prefs.contains(passwordKey(examId))) prefs.getBoolean(passwordKey(examId), false) else null

    fun setPasswordChecked(examId: Int, checked: Boolean) {
        prefs.edit().putBoolean(passwordKey(examId), checked).commit()
    }

    /** Logout: another account must not inherit anything of this one. */
    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun sessionKey(examId: Int) = "exam_session_active_$examId"

    private fun passwordKey(examId: Int) = "password_checked_$examId"

    private fun penaltyEndKey(examId: Int) = "penalty_end_$examId"

    private fun penaltyTotalKey(examId: Int) = "penalty_total_$examId"

    companion object {
        const val FILE_NAME = "akm_exam_state"
        private const val KEY_REPORT_LAST = "problem_report_last_submit_at"
    }
}
