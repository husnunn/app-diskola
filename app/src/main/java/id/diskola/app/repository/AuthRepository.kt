package id.diskola.app.repository

import id.diskola.app.apiservice.AuthApiService
import id.diskola.app.dataclass.ResponData.CheckAccountApiResponse
import id.diskola.app.dataclass.ResponData.CheckAccountRequest
import id.diskola.app.dataclass.ResponData.LoginAccountApiResponse
import id.diskola.app.dataclass.ResponData.LoginAccountRequest
import id.diskola.app.dataclass.ResponData.LoginSsoApiResponse
import id.diskola.app.dataclass.ResponData.LoginSsoRequest
import id.diskola.app.dataclass.ResponData.LoginSsoSchoolApiResponse
import id.diskola.app.dataclass.ResponData.LoginSsoSchoolRequest
import id.diskola.app.dataclass.ResponData.ResetPasswordRequest
import id.diskola.app.dataclass.ResponData.SetupUserFcmRequest
import id.diskola.app.utils.DeviceIdProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Thin wrapper over [AuthApiService] — centralizes `device_id` injection (gap `02a` P1) and the
 * "errors ignored" contract for `setup-user-fcm` (doc `02-auth-login-sesi.md` §4.3 step 3) so every
 * ViewModel doesn't have to reimplement either.
 */
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun checkAccount(nisNik: String, schoolUuid: String): CheckAccountApiResponse =
        authApiService.checkAccount(CheckAccountRequest(school_id = schoolUuid, nisn_nik = nisNik))

    suspend fun loginAccount(uuid: String, password: String): LoginAccountApiResponse =
        authApiService.loginAccount(
            LoginAccountRequest(uuid = uuid, password = password, device_id = deviceIdProvider.get())
        )

    suspend fun loginSso(email: String, name: String, image: String): LoginSsoApiResponse =
        authApiService.loginSso(
            LoginSsoRequest(email = email, name = name, image = image, device_id = deviceIdProvider.get())
        )

    suspend fun loginSsoSchool(schoolUuid: String): LoginSsoSchoolApiResponse =
        authApiService.loginSsoSchool(LoginSsoSchoolRequest(school_id = schoolUuid))

    suspend fun resetPassword(userId: Int, email: String) =
        authApiService.resetPassword(ResetPasswordRequest(user_id = userId, email = email))

    /** Fire-and-forget — doc §4.3 step 3 says errors here are swallowed by the legacy app too. */
    suspend fun setupFcmToken(uuid: String, fcmToken: String) {
        if (fcmToken.isBlank() || uuid.isBlank()) return
        try {
            authApiService.updateFcmToken(SetupUserFcmRequest(uuid = uuid, user_fcm_token = fcmToken))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
