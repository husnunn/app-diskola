package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.LoginSsoApiResponse
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.repository.AuthRepository
import id.diskola.app.ui.screens.auth.GoogleAccountInfo
import id.diskola.app.utils.session.AuthError
import id.diskola.app.utils.session.AuthErrorMapper
import id.diskola.app.utils.session.FcmTopicManager
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Backs `SsoScreen` + the guest-confirmation gate (doc `02-auth-login-sesi.md` §5.2–5.3).
 * `checkEmail` result decides, per decision Q1 (replicated exactly, race condition included —
 * verified by re-reading the legacy coroutine dispatch, not guessed): the guest-confirmation
 * dialog shows whenever `data == null || data.school == null`, i.e. exactly [isGuestGate].
 */
@HiltViewModel
class SsoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val fcmTopicManager: FcmTopicManager,
) : BaseViewModel() {

    private val _checkEmailResult = MutableStateFlow<LoginSsoApiResponse?>(null)
    val checkEmailResult: StateFlow<LoginSsoApiResponse?> = _checkEmailResult.asStateFlow()

    private val _authError = MutableStateFlow<AuthError?>(null)
    val authError: StateFlow<AuthError?> = _authError.asStateFlow()

    private val _selectedSchool = MutableStateFlow<SchoolItem?>(null)
    val selectedSchool: StateFlow<SchoolItem?> = _selectedSchool.asStateFlow()

    /** `null` while `checkEmail` hasn't returned yet; after that, true = show
     * [id.diskola.app.ui.screens.auth.GuestConfirmDialog] before `SsoScreen`. */
    private val _isGuestGate = MutableStateFlow<Boolean?>(null)
    val isGuestGate: StateFlow<Boolean?> = _isGuestGate.asStateFlow()

    /** `null` = not finished yet; true/false = resulting `klaspayActive`, so the caller can route
     * to Main vs Klaspay activation. */
    private val _ssoSchoolResult = MutableStateFlow<Boolean?>(null)
    val ssoSchoolResult: StateFlow<Boolean?> = _ssoSchoolResult.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    fun setSelectedSchool(item: SchoolItem?) {
        _selectedSchool.value = item
    }

    fun checkEmail(account: GoogleAccountInfo) {
        _loading.value = true
        _authError.value = null
        viewModelScope.launch {
            try {
                val response = authRepository.loginSso(account.email, account.displayName, account.profilePictureUri)
                sessionStore.writeSsoCheckEmail(response.data, response.token)
                _checkEmailResult.value = response
                _isGuestGate.value = response.data == null || response.data.school == null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _authError.value = AuthErrorMapper.map(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun confirmSchool() {
        val checkEmailData = _checkEmailResult.value?.data
        val schoolUuid = checkEmailData?.school?.uuid?.takeIf { it.isNotBlank() }
            ?: _selectedSchool.value?.uuid
        if (schoolUuid.isNullOrBlank()) {
            emitError("Silakan pilih sekolah terlebih dahulu")
            return
        }
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = authRepository.loginSsoSchool(schoolUuid)
                val data = response.data
                if (data == null) {
                    emitError("Data sekolah tidak ditemukan")
                    return@launch
                }
                val klaspayActive = sessionStore.writeSsoSchool(data, checkEmailData, _selectedSchool.value)
                authRepository.setupFcmToken(sessionStore.userUuid, sessionStore.fcmToken)
                fcmTopicManager.subscribeForSession(
                    sessionStore.userUuid,
                    sessionStore.userId,
                    sessionStore.student?.classRoomId ?: 0,
                    sessionStore.school.uuid,
                )
                _ssoSchoolResult.value = klaspayActive
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _authError.value = AuthErrorMapper.map(e)
            } finally {
                _loading.value = false
            }
        }
    }
}
