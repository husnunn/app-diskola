package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.apiservice.CommonApiService
import id.diskola.app.dataclass.ResponData.PolicyResponse
import id.diskola.app.utils.session.SessionManager
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AkunViewModel @Inject constructor(
    private val commonApiService: CommonApiService,
    val sessionStore: SessionStore,
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val _policy = MutableStateFlow<PolicyResponse?>(null)
    val policy: StateFlow<PolicyResponse?> = _policy.asStateFlow()

    private val _about = MutableStateFlow<PolicyResponse?>(null)
    val about: StateFlow<PolicyResponse?> = _about.asStateFlow()

    // UI-only mock flags — there is no email/phone verification endpoint yet (see `ref/fase2`).
    private val _emailVerified = MutableStateFlow(false)
    val emailVerified: StateFlow<Boolean> = _emailVerified.asStateFlow()

    fun fetchPolicy() {
        launchWithHandling {
            val response = commonApiService.policy()
            _policy.value = response
        }
    }

    fun fetchAbout(userId: Int) {
        launchWithHandling {
            val response = commonApiService.about(userId)
            _about.value = response
        }
    }

    fun markEmailVerificationSent() {
        // Mock only — a real implementation would call the verification endpoint.
    }

    private val _logoutBlocked = MutableStateFlow(false)
    val logoutBlocked: StateFlow<Boolean> = _logoutBlocked.asStateFlow()

    /** Doc `02-auth-login-sesi.md` §10.1: blocked (with [logoutBlocked] flipping true instead of
     * calling [onComplete]) while there's a pending AKM exam — see [SessionManager.logout]. */
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            when (sessionManager.logout()) {
                is SessionManager.LogoutResult.Success -> onComplete()
                is SessionManager.LogoutResult.BlockedByPendingExam -> _logoutBlocked.value = true
            }
        }
    }

    fun consumeLogoutBlocked() {
        _logoutBlocked.value = false
    }
}
