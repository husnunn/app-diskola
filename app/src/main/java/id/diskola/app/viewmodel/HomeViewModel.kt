package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.CheckAccountApiResponse
import id.diskola.app.dataclass.ResponData.CurrentSubjectData
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.repository.AgendaRepository
import id.diskola.app.repository.AuthRepository
import id.diskola.app.repository.FeatureGateRepository
import id.diskola.app.repository.HubNotificationRepository
import id.diskola.app.repository.PresensiRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.DeviceIdProvider
import id.diskola.app.utils.HubRules
import id.diskola.app.utils.session.SessionManager
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Who is signed in and what the hub header shows — always read back from [SessionStore] after a check-account lands. */
data class HomeProfile(
    val schoolName: String,
    val schoolLogo: String,
    val userName: String,
    val userAvatar: String,
    /** Under the name: the e-mail for a teacher, the class for everyone else (legacy `title_class`). */
    val classLine: String,
    val isStudent: Boolean,
    val isTeacher: Boolean,
    val isGuest: Boolean,
    val isHavingClass: Boolean,
    val klaspayActive: Boolean,
)

data class HomeUiState(
    val profile: HomeProfile,
    val verify: HubRules.VerifyState,
    /** `check-account.current_subject` of the latest successful check-account (students' "Kelas Berlangsung"). */
    val currentSubject: CurrentSubjectData? = null,
    /** Teachers' attendance banner; null until `setting/me/today` + `check` answered. */
    val banner: HubRules.AttendanceBanner? = null,
    val bellCount: Int = 0,
    val agendaMissing: Int = 0,
    val loading: Boolean = false,
)

data class FeatureUnavailable(val name: String, val message: String)

/**
 * Pembelajaran hub (legacy `PembelajaranPage`). One load, in the legacy order: `check-account` (profile, role, class, verify
 * progress, `current_subject`), then in parallel the session guard (`current-user`), the bell badge (`payment/wallet` →
 * `notification/summary`), the teacher's agenda badge and — for teachers whose content shows — the attendance banner
 * (`setting/me/today` + `staff/check`).
 *
 * It runs on every return to the screen and on the refresh icon, like legacy, but never overlaps itself (the flag is set before
 * `launch`) and a return within [MIN_RELOAD_MS] of the last successful load is skipped. The refresh icon always loads.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val agendaRepository: AgendaRepository,
    private val presensiRepository: PresensiRepository,
    private val notificationRepository: HubNotificationRepository,
    private val featureGate: FeatureGateRepository,
    private val sessionManager: SessionManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _accountInactive = MutableStateFlow(false)
    val accountInactive: StateFlow<Boolean> = _accountInactive.asStateFlow()

    private val _deviceConflict = MutableStateFlow(false)
    val deviceConflict: StateFlow<Boolean> = _deviceConflict.asStateFlow()

    private val _guestPopup = MutableStateFlow(false)
    val guestPopup: StateFlow<Boolean> = _guestPopup.asStateFlow()

    private val _logoutBlocked = MutableStateFlow(false)
    val logoutBlocked: StateFlow<Boolean> = _logoutBlocked.asStateFlow()

    private val _featureChecking = MutableStateFlow(false)
    val featureChecking: StateFlow<Boolean> = _featureChecking.asStateFlow()

    private val _featureUnavailable = MutableStateFlow<FeatureUnavailable?>(null)
    val featureUnavailable: StateFlow<FeatureUnavailable?> = _featureUnavailable.asStateFlow()

    private var inFlight = false
    private var lastLoadedAt = 0L
    private var guestPopupShown = false

    // ---- Triggers ----

    /** Every time the hub comes back to the foreground. */
    fun onResumed() = load(force = false)

    /** The refresh icon. */
    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        if (inFlight) return
        if (!force && lastLoadedAt != 0L && System.currentTimeMillis() - lastLoadedAt < MIN_RELOAD_MS) return
        inFlight = true
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                loadAll()
            } finally {
                inFlight = false
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        val response = refreshProfile()
        if (response != null) lastLoadedAt = System.currentTimeMillis()

        val profile = readProfile()
        _state.update { current ->
            if (response == null) {
                current.copy(profile = profile)
            } else {
                current.copy(
                    profile = profile,
                    verify = HubRules.verifyState(response.userProgress, profile.isStudent, profile.isHavingClass),
                    currentSubject = response.current_subject,
                )
            }
        }
        if (!sessionStore.isActive) _accountInactive.value = true
        if (!guestPopupShown && profile.isGuest) {
            guestPopupShown = true
            _guestPopup.value = true
        }

        launch { guardSession() }
        launch { loadBell(profile) }
        if (profile.isTeacher) launch { loadAgendaBadge() }
        // Legacy shows the banner wherever a non-student's content card would have been (`layoutCurrentClass` visible).
        if (!profile.isStudent && _state.value.verify == HubRules.VerifyState.ShowContent) {
            launch { loadBanner() }
        }
    }

    /** `check-account`; null when it could not run or failed (the stored profile keeps being used, like legacy). */
    private suspend fun refreshProfile(): CheckAccountApiResponse? {
        val school = sessionStore.school
        val nisn = sessionStore.student?.nis?.takeIf { it.isNotBlank() } ?: sessionStore.user.nisNik
        if (school.uuid.isBlank() || nisn.isBlank()) return null
        return try {
            val response = authRepository.checkAccount(nisn, school.uuid)
            sessionStore.writeCheckAccount(response, SchoolItem(id = school.id, uuid = school.uuid, name = school.name))
            response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e)
            emitError(AppErrorHandler.getMessage(e))
            null
        }
    }

    /** Single-device check; a 401 reaches the central "Sesi Berakhir" handler on its own. */
    private suspend fun guardSession() {
        try {
            if (HubRules.deviceMismatch(authRepository.currentUser(), deviceIdProvider.get())) _deviceConflict.value = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun loadBell(profile: HomeProfile) {
        if (!profile.klaspayActive) return
        notificationRepository.unreadCount()?.let { count -> _state.update { it.copy(bellCount = count) } }
    }

    /** Teacher badge: today's missing sessions (0 when agendas are off or the call fails). */
    private suspend fun loadAgendaBadge() {
        val missing = agendaRepository.missingToday()
        _state.update { it.copy(agendaMissing = missing) }
    }

    /**
     * `setting/me/today` and `check` in parallel. If today's setting cannot be read the banner stays as it was (legacy would
     * claim "Jadwal absensi belum diatur"); a missing `check` is the legacy "Di luar jam absensi".
     */
    private suspend fun loadBanner() = coroutineScope {
        val today = async { attempt { presensiRepository.settingToday() } }
        val check = async { attempt { presensiRepository.check() } }
        val todayResult = today.await()
        val checkResult = check.await()
        if (todayResult.isFailure) return@coroutineScope
        val banner = HubRules.attendanceBanner(todayResult.getOrNull(), checkResult.getOrNull())
        _state.update { it.copy(banner = banner) }
    }

    // ---- Feature gate (Presensi / Jurnal tiles, banner CTA) ----

    /**
     * Server-side feature switch (`check-feature-availability`) in front of Presensi/Jurnal. Checked once per tap;
     * an error counts as available.
     */
    fun openFeature(name: String, onAvailable: () -> Unit) {
        if (_featureChecking.value) return
        _featureChecking.value = true
        viewModelScope.launch {
            val result = featureGate.check(name)
            _featureChecking.value = false
            if (result.available) onAvailable() else _featureUnavailable.value = FeatureUnavailable(name, result.message.orEmpty())
        }
    }

    fun dismissFeatureUnavailable() {
        _featureUnavailable.value = null
    }

    // ---- Dialog results ----

    fun dismissGuestPopup() {
        _guestPopup.value = false
    }

    fun consumeLogoutBlocked() {
        _logoutBlocked.value = false
    }

    /** "Baik" on the inactive-account alert and "Mengerti" on the other-device alert: log out, unless an exam is pending. */
    fun confirmLogout(onComplete: () -> Unit) {
        viewModelScope.launch {
            when (sessionManager.logout()) {
                is SessionManager.LogoutResult.Success -> onComplete()
                is SessionManager.LogoutResult.BlockedByPendingExam -> _logoutBlocked.value = true
            }
        }
    }

    // ---- State from the session ----

    private fun initialState(): HomeUiState {
        val profile = readProfile()
        return HomeUiState(profile = profile, verify = HubRules.verifyState(null, profile.isStudent, profile.isHavingClass))
    }

    private fun readProfile(): HomeProfile {
        val school = sessionStore.school
        val user = sessionStore.user
        return HomeProfile(
            schoolName = school.name,
            schoolLogo = school.image,
            userName = user.name,
            userAvatar = user.avatar,
            classLine = HubRules.classLine(sessionStore.isTeacher, user.email, sessionStore.student?.className.orEmpty()),
            isStudent = sessionStore.isStudent,
            isTeacher = sessionStore.isTeacher,
            isGuest = sessionStore.isGuest,
            isHavingClass = sessionStore.isHavingClass,
            klaspayActive = sessionStore.klaspayActive,
        )
    }

    /** `runCatching` without swallowing cancellation. */
    private inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e)
        Result.failure(e)
    }

    private companion object {
        /** A return to the hub sooner than this after a successful load does not fetch again. */
        const val MIN_RELOAD_MS = 30_000L
    }
}
