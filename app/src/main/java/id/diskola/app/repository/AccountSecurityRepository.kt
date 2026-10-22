package id.diskola.app.repository

import id.diskola.app.apiservice.AkunApiService
import id.diskola.app.apiservice.AuthApiService
import id.diskola.app.dataclass.ResponData.ChangePasswordRequest
import id.diskola.app.dataclass.ResponData.UpdateProfileRequest
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject

/**
 * The calls behind the e-mail and default-password dialogs that stand in front of wallet activation (doc `02-auth-login-sesi.md` §8.2).
 * Each one keeps the matching session flag in step, like legacy `ApiWrapper` / `SettingAkunViewmodel`.
 */
class AccountSecurityRepository @Inject constructor(
    private val akunApi: AkunApiService,
    private val authApi: AuthApiService,
    private val sessionStore: SessionStore,
) {
    /** `change-profile` with the new e-mail and the stored phone/username (the endpoint wants all three). */
    suspend fun changeEmail(email: String) {
        val user = sessionStore.user
        akunApi.updateProfile(sessionStore.userUuid, UpdateProfileRequest(phone = user.phone, email = email, username = user.username))
        sessionStore.writeUserContact(email = email, phone = user.phone, username = user.username)
    }

    /** Sends the verification mail; afterwards the account counts as "verifying". */
    suspend fun requestEmailVerification() {
        authApi.verifyEmail()
        sessionStore.markEmailVerifying()
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        akunApi.changePassword(sessionStore.userUuid, ChangePasswordRequest(old_password = oldPassword, new_password = newPassword))
        sessionStore.markPasswordChanged()
    }
}
