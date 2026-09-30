package id.diskola.app.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.R
import id.diskola.app.apiservice.CommonApiService
import id.diskola.app.dataclass.ResponData.CheckVersion
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import timber.log.Timber

enum class SplashDestination { UPDATE_REQUIRED, ONBOARDING, LOGIN, MAIN }

/**
 * Splash/router (doc `02-auth-login-sesi.md` §2). Fixes gap `02a` S1–S3: reads `onboard`/
 * `logged_in` from [SessionStore] (the same prefs file/keys the legacy app used, not the retired
 * `user_session` file), and actually uses the version-check result instead of discarding it behind
 * a timeout.
 *
 * Not ported yet (tracked, not silently dropped): the Firebase In-App Messaging banner gate (§2.3,
 * decision Q4) and the extra FCM `page`/`goto` handling (§2.1 step 1, S4) — both belong with the
 * notification-routing work in doc 03, which this phase doesn't cover.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commonApiService: CommonApiService,
    private val moshi: Moshi,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val appName = context.getString(R.string.app_name)
        manager.createNotificationChannel(NotificationChannel(appName, appName, NotificationManager.IMPORTANCE_DEFAULT))
        manager.createNotificationChannel(
            NotificationChannel("fcm_default_channel_id", appName, NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel("$appName.silent", "$appName (senyap)", NotificationManager.IMPORTANCE_MIN).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
        )
    }

    /** `null` = version check failed/unparseable → doc §2.2 says fall through to the normal
     * router (`goNext()`), not block startup on a network hiccup. */
    private suspend fun checkVersion(): CheckVersion? = try {
        val raw = commonApiService.checkVersion().data.setting_globals_value
        moshi.adapter(CheckVersion::class.java).fromJson(raw)
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    suspend fun resolveDestination(currentVersionName: String): SplashDestination {
        val serverVersion = checkVersion()
        if (serverVersion != null && needsUpdate(currentVersionName, serverVersion.version)) {
            return SplashDestination.UPDATE_REQUIRED
        }
        return when {
            !sessionStore.isOnboarded -> SplashDestination.ONBOARDING
            sessionStore.isLoggedIn -> SplashDestination.MAIN
            else -> SplashDestination.LOGIN
        }
    }

    /** `zip`s each dotted segment as an Int and compares the first that differs — doc §2.2. A
     * non-numeric segment on either side means "can't tell", so it's treated as no update, same as
     * the legacy app's unguarded `toInt()` would otherwise crash and abandon the check entirely. */
    private fun needsUpdate(current: String, server: String): Boolean {
        val currentSegments = current.split(".").mapNotNull { it.toIntOrNull() }
        val serverSegments = server.split(".").mapNotNull { it.toIntOrNull() }
        if (currentSegments.size != current.split(".").size || serverSegments.size != server.split(".").size) return false
        serverSegments.zip(currentSegments).forEach { (serverPart, currentPart) ->
            if (serverPart != currentPart) return serverPart > currentPart
        }
        return false
    }
}
