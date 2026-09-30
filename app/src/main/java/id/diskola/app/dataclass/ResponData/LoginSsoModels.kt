package id.diskola.app.dataclass.ResponData

import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString

/** `POST mobile/app/authentication/login-sso` — doc `02-auth-login-sesi.md` §5.2, §11. */
@JsonClass(generateAdapter = true)
data class LoginSsoRequest(
    val email: String,
    val name: String,
    val image: String,
    val device_id: String,
)

@JsonClass(generateAdapter = true)
data class LoginSsoApiResponse(
    val data: LoginSsoUserData? = null,
    @NullToEmptyString val token: String = "",
    @NullToEmptyString val message: String = "",
)

@JsonClass(generateAdapter = true)
data class LoginSsoUserData(
    val id: Int = 0,
    @NullToEmptyString val username: String = "",
    @NullToEmptyString val email: String = "",
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val image: String = "",
    val is_student: Boolean = false,
    val is_teacher: Boolean = false,
    val is_klaspay_activated: Boolean = false,
    val school: SekolahItem? = null,
    val current_class: SsoStudentClass? = null,
)

@JsonClass(generateAdapter = true)
data class SsoStudentClass(
    val id: Int = 0,
    val class_room: SsoClassRoom = SsoClassRoom(),
    @NullToEmptyString val name: String = "",
    val grade: Int = 0,
)

@JsonClass(generateAdapter = true)
data class SsoClassRoom(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    val grade: Int = 0,
)

/** `POST mobile/app/authentication/login-sso/school` — doc §5.3. */
@JsonClass(generateAdapter = true)
data class LoginSsoSchoolRequest(val school_id: String)

@JsonClass(generateAdapter = true)
data class LoginSsoSchoolApiResponse(val data: LoginSsoSchoolData? = null)

@JsonClass(generateAdapter = true)
data class LoginSsoSchoolData(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val email: String = "",
    @NullToEmptyString val avatar: String = "",
    @NullToEmptyString val uuid: String = "",
    val school_id: Int = 0,
    // Legacy sends this one as an Int (0/1), unlike every other `is_klaspay_activated` in this app.
    val is_klaspay_activated: Int = 0,
)

/** `POST mobile/app/authentication/reset-password` — doc §7. */
@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(val user_id: Int, val email: String)

/** `POST sosmed/setting/setup-user-fcm` — doc §4.3 step 3, called after login/SSO; errors ignored. */
@JsonClass(generateAdapter = true)
data class SetupUserFcmRequest(val uuid: String, val user_fcm_token: String)
