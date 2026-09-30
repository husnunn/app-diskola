package id.diskola.app.utils.session

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.BuildConfig
import id.diskola.app.apiservice.AuthApiService
import id.diskola.app.database.LocalDatabase
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.ui.MainActivity
import id.diskola.app.utils.PreferenceClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * The single logout procedure for the whole app (doc `02-auth-login-sesi.md` §10.2, decision Q6/Q8
 * and `docs/FLOW_QUESTIONS.md` bagian E) — every trigger (Akun button, the centralized 401 handler
 * via [SessionEvents], FCM `"logout"`) calls [logout] instead of duplicating this sequence.
 *
 * Runs on its own application-scoped [CoroutineScope] rather than `GlobalScope` (gap `02a` anti-
 * pattern §3) so it survives whichever screen triggered it being torn down mid-logout.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localDatabase: LocalDatabase,
    private val preference: PreferenceClass,
    private val akmSyncDao: AkmSyncDao,
    private val fcmTopicManager: FcmTopicManager,
    private val authApiService: Provider<AuthApiService>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed interface LogoutResult {
        data object Success : LogoutResult
        data object BlockedByPendingExam : LogoutResult
    }

    /** DOWNLOADED-but-not-yet-`SUBMITTED` rows in `akm_synced_exam` — doc §10.1's "ujian belum
     * dikumpulkan", extended per `docs/FLOW_QUESTIONS.md` bagian E#2 to also cover exams whose
     * answers finished but aren't confirmed uploaded yet. */
    suspend fun hasPendingExam(): Boolean = akmSyncDao.countPendingSubmission() > 0

    suspend fun logout(navigateToLogin: Boolean = true): LogoutResult {
        if (hasPendingExam()) return LogoutResult.BlockedByPendingExam

        val wasLoggedIn = preference.getBoolean(SessionKeys.LOGGED_IN)
        val userUuid = preference.getString(SessionKeys.USER_UUID)
        val userId = preference.getInt(SessionKeys.USER_ID)
        val classId = preference.getInt(SessionKeys.CLASS_ID)
        val schoolUuid = preference.getString(SessionKeys.SCHOOL_UUID)

        try {
            withContext(Dispatchers.IO) { localDatabase.clearAllTables() }
        } catch (e: Exception) {
            Timber.e(e)
        }

        try {
            fcmTopicManager.unsubscribeAll(userUuid, userId, classId, schoolUuid)
        } catch (e: Exception) {
            Timber.e(e)
        }

        if (wasLoggedIn) {
            try {
                authApiService.get().logout()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        // commit(), not apply() — the caller is about to restart the whole Activity stack, so this
        // must be durable on disk before that happens (matches legacy `edit(true)`; fixes gap 02a's
        // "second edit{} block wipes onboard/url_api" bug, which used two overlapping edits).
        preference.edit().clear().commit()
        preference.edit()
            .putString(SessionKeys.URL_API, BuildConfig.API_URL)
            .putBoolean(SessionKeys.ONBOARD, true)
            .commit()

        NotificationManagerCompat.from(context).cancelAll()
        WorkManager.getInstance(context).cancelAllWork()

        if (navigateToLogin) navigateToLogin()
        return LogoutResult.Success
    }

    /** Fire-and-forget variant for callers that aren't in a coroutine (e.g. `NotifService`). */
    fun logoutInBackground(navigateToLogin: Boolean = true) {
        scope.launch { logout(navigateToLogin) }
    }

    /**
     * Restarts [MainActivity] with a clean task. `Splash` reads the (now logged-out) session state
     * itself and routes to Login — no extra needed, unlike the legacy app's `logout_message` extra.
     */
    fun navigateToLogin() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
