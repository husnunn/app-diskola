package id.diskola.app.utils.session

/**
 * Every SharedPreferences key the auth/session flow reads or writes, gathered in one place per
 * `docs/migrasi/02-auth-login-sesi.md` §12 and §4.3/§5.3. All keys live in the main prefs file
 * (`context.packageName`, injected as [id.diskola.app.utils.PreferenceClass]) — this is the same
 * file and the same key names the legacy `android-portal` app used, which is what lets a user who
 * updates in place stay logged in (see `docs/migrasi/02a` R9/S3 and doc 02 §14 Q9).
 *
 * The separate `user_session` SharedPreferences file used by the previous (non-`android-portal`)
 * port is retired — [SessionStore] is the only reader/writer of session state from now on.
 */
object SessionKeys {
    const val ONBOARD = "onboard"
    const val LOGGED_IN = "logged_in"
    const val USER_TOKEN = "user_token"
    const val USER_ID = "user_id"
    const val USER_UUID = "user_uuid"
    const val IS_SSO = "isSso"
    const val IS_STUDENT = "is_student"
    const val IS_TEACHER = "is_teacher"
    const val IS_LIBRARIAN = "is_librarian"
    const val IS_HAVING_CLASS = "is_having_class"
    const val HAS_STUDENT_CLASS = "has_student_class"
    const val STUDENT_JSON = "student"
    const val TEACHER_JSON = "teacher"
    const val CLASS_ID = "class_id"
    const val ROLES = "roles"
    const val ROLE_LABEL = "role_label"
    const val SCHOOL_JSON = "school"
    const val SCHOOL_UUID = "school_uuid"
    const val USER_JSON = "user"
    const val IS_ACTIVE = "is_active"
    const val IS_VERIFIED = "is_verified"
    const val IS_EMAIL_VERIFIED = "is_email_verified"
    const val IS_EMAIL_VERIFYING = "is_email_verifying"
    const val KLASPAY_ACTIVE = "klaspayActive"
    const val DEFAULT_PASS = "default_pass"
    const val ONKLAS_LITE = "onklas_lite"
    const val ONKLAS_PRO = "onklas_pro"
    const val KLASTIME = "klastime"
    const val FCM_TOKEN = "token"
    const val FIREBASE_ID = "firebase_id"
    const val URL_API = "url_api"
    const val CHECK_VC = "check_vc"

    /** Kept for the legacy in-place-update migration story; not read anywhere in this app. */
    const val THEME_MODE = "theme_mode"
}
