package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val sessionStore: SessionStore) : BaseViewModel() {
    /** Doc `02-auth-login-sesi.md` §3: onboarding only shows once — `onboard=true` survives logout. */
    fun markSeen() {
        sessionStore.setOnboarded()
    }
}
