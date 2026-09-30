package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.repository.AuthRepository
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject

/**
 * The read-only slice of Home's "refresh account on every landing/resume" behaviour (doc
 * `02-auth-login-sesi.md` §9's `checkUser()`) that's needed just to keep Home/Akun's display data
 * current now that `AuthViewModel` is gone. The full periodic-session-validation flow (401 via
 * `current-user`, `is_active==false` handling, etc.) is doc §9's own feature — out of scope here,
 * tracked for the Home-shell phase (doc 03).
 */
@HiltViewModel
class SessionRefreshViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val sessionStore: SessionStore,
) : BaseViewModel() {

    fun refreshProfile(onComplete: () -> Unit = {}) {
        val school = sessionStore.school
        val nisn = sessionStore.student?.nis?.takeIf { it.isNotBlank() } ?: sessionStore.user.nisNik
        if (school.uuid.isBlank() || nisn.isBlank()) {
            onComplete()
            return
        }
        launchWithHandling(showLoading = false, showError = false) {
            try {
                val response = authRepository.checkAccount(nisn, school.uuid)
                sessionStore.writeCheckAccount(response, SchoolItem(id = school.id, uuid = school.uuid, name = school.name))
            } finally {
                onComplete()
            }
        }
    }
}
