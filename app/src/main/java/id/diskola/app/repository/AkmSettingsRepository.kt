package id.diskola.app.repository

import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.PreferenceClass
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * The server's exam switches (`GET mobile/setting-akm`, doc 06 §11.1/§17.2): strict mode, penalty on/off, penalty length. Kept as
 * the legacy integers — `-1` means "never received" and counts as **on** (safe side) — with a 12 h time-to-live; [refresh] with
 * `force = true` runs right before an exam starts and keeps the old values when the call fails. Presensi keeps its own copy of
 * `absence_setting` (`PresensiRepository`), unchanged.
 */
@Singleton
class AkmSettingsRepository @Inject constructor(
    private val api: AsesmenApiService,
    private val preference: PreferenceClass,
) {

    /** Fetches the switches when they are stale (or [force]d). Never throws; returns whether new values were stored. */
    suspend fun refresh(force: Boolean = false, timeoutMillis: Long = REFRESH_TIMEOUT_MS): Boolean {
        val now = System.currentTimeMillis()
        if (!force && !AkmRules.settingsStale(preference.getLong(KEY_LAST_FETCH), now)) return false
        return try {
            val data = withTimeoutOrNull(timeoutMillis) { api.getAkmSettings().data } ?: return false
            preference.putInt(KEY_PENALTY_TIMES, data.penalty_times)
            preference.putInt(KEY_PENALTY_APPLIED, AkmRules.penaltyAppliedRaw(data.penalty_applied))
            preference.putInt(KEY_LOCK_MODE, AkmRules.lockModeRaw(data.exam_lock_mode))
            preference.putLong(KEY_LAST_FETCH, now)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Setelan asesmen tidak dapat dimuat; nilai lama dipakai")
            false
        }
    }

    fun isStrict(): Boolean = AkmRules.isStrict(preference.getInt(KEY_LOCK_MODE, AkmRules.SETTING_NEVER_FETCHED))

    fun isPenaltyApplied(): Boolean = AkmRules.isPenaltyApplied(preference.getInt(KEY_PENALTY_APPLIED, AkmRules.SETTING_NEVER_FETCHED))

    fun penaltySeconds(): Int = AkmRules.penaltySeconds(preference.getInt(KEY_PENALTY_TIMES, AkmRules.SETTING_NEVER_FETCHED))

    private companion object {
        const val KEY_LOCK_MODE = "akm_setting_exam_lock_mode"
        const val KEY_PENALTY_APPLIED = "akm_setting_penalty_applied"
        const val KEY_PENALTY_TIMES = "akm_setting_penalty_times"
        const val KEY_LAST_FETCH = "akm_setting_last_fetch"

        /** A phone with a bad connection must not stall the "Mulai" button for a whole network timeout. */
        const val REFRESH_TIMEOUT_MS = 4_000L
    }
}
