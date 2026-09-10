package id.app.education.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.app.education.apiservice.CommonApiService
import id.app.education.dataclass.ResponData.PolicyResponse
import id.app.education.utils.IntentUtil
import id.app.education.utils.PreferenceClass
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AkunViewModel @Inject constructor(
    private val commonApiService: CommonApiService,
    val preference: PreferenceClass,
    private val intentUtil: IntentUtil,
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

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                intentUtil.logOut()
            } catch (_: Exception) {
            }
            onComplete()
        }
    }
}
