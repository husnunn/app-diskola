package id.diskola.app.utils.session

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import id.diskola.app.dataclass.ResponData.CheckAccountApiResponse
import id.diskola.app.dataclass.ResponData.LoginAccountApiResponse
import id.diskola.app.dataclass.ResponData.LoginSsoSchoolData
import id.diskola.app.dataclass.ResponData.LoginSsoUserData
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.utils.PreferenceClass
import javax.inject.Inject

/**
 * Single reader/writer of every auth/session SharedPreferences key ([SessionKeys]). Replaces the
 * old `user_session` file and the ad-hoc field mapping that used to live in `AuthViewModel` —
 * field mapping here follows `docs/migrasi/02-auth-login-sesi.md` §4.3/§5.2/§5.3 exactly, with the
 * corrections noted inline (from re-reading `android-portal`'s actual runtime behaviour).
 */
class SessionStore @Inject constructor(
    private val preference: PreferenceClass,
    moshi: Moshi,
) {
    private val userAdapter: JsonAdapter<SessionUser> = moshi.adapter(SessionUser::class.java)
    private val schoolAdapter: JsonAdapter<SessionSchool> = moshi.adapter(SessionSchool::class.java)
    private val studentAdapter: JsonAdapter<SessionStudent> = moshi.adapter(SessionStudent::class.java)
    private val teacherAdapter: JsonAdapter<SessionTeacher> = moshi.adapter(SessionTeacher::class.java)

    // ---- Reads used by the router and by other screens (Home/Akun/Akm) ----

    val isOnboarded: Boolean get() = preference.getBoolean(SessionKeys.ONBOARD)
    val isLoggedIn: Boolean get() = preference.getBoolean(SessionKeys.LOGGED_IN)
    val token: String get() = preference.getString(SessionKeys.USER_TOKEN)
    val isStudent: Boolean get() = preference.getBoolean(SessionKeys.IS_STUDENT)
    val isTeacher: Boolean get() = preference.getBoolean(SessionKeys.IS_TEACHER)
    val isHavingClass: Boolean get() = preference.getBoolean(SessionKeys.IS_HAVING_CLASS)
    val isActive: Boolean get() = preference.getBoolean(SessionKeys.IS_ACTIVE, true)
    val isEmailVerified: Boolean get() = preference.getBoolean(SessionKeys.IS_EMAIL_VERIFIED)
    val defaultPass: Boolean get() = preference.getBoolean(SessionKeys.DEFAULT_PASS)
    val klaspayActive: Boolean get() = preference.getBoolean(SessionKeys.KLASPAY_ACTIVE)
    val userId: Int get() = preference.getInt(SessionKeys.USER_ID)
    val userUuid: String get() = preference.getString(SessionKeys.USER_UUID)
    val fcmToken: String get() = preference.getString(SessionKeys.FCM_TOKEN)

    val user: SessionUser get() = readJson(SessionKeys.USER_JSON, userAdapter) ?: SessionUser()
    val school: SessionSchool get() = readJson(SessionKeys.SCHOOL_JSON, schoolAdapter) ?: SessionSchool()
    val student: SessionStudent? get() = readJson(SessionKeys.STUDENT_JSON, studentAdapter)
    val teacher: SessionTeacher? get() = readJson(SessionKeys.TEACHER_JSON, teacherAdapter)

    /** `role_label` (Student/Teacher/Guest, set by SSO — doc §5.2) falling back to `roles`
     * (the backend's own `rule_label`, set by login-account — doc §4.3). Used to detect Guests. */
    val roleLabel: String
        get() = preference.getString(SessionKeys.ROLE_LABEL).ifBlank { preference.getString(SessionKeys.ROLES) }

    val isGuest: Boolean get() = !isStudent && !isTeacher

    fun setOnboarded() {
        preference.putBoolean(SessionKeys.ONBOARD, true)
    }

    // ---- Writes ----

    /** `check-account` success side effects — doc §4.1 "Efek samping". */
    fun writeCheckAccount(response: CheckAccountApiResponse, selectedSchool: SchoolItem) {
        val usesDefaultPassword = response.using_default_password
            .ifBlank { response.data?.using_default_password.orEmpty() }
            .equals("Y", ignoreCase = true)
        preference.putBoolean(SessionKeys.DEFAULT_PASS, usesDefaultPassword)
        preference.putInt(SessionKeys.USER_ID, response.data?.id ?: 0)
        preference.putBoolean(SessionKeys.IS_ACTIVE, response.is_active ?: true)

        val schoolToStore = response.data?.school?.toSession() ?: selectedSchool.toSession()
        writeJson(SessionKeys.SCHOOL_JSON, schoolAdapter, schoolToStore)

        response.data?.let { data ->
            writeJson(
                SessionKeys.USER_JSON, userAdapter,
                SessionUser(
                    id = data.id,
                    uuid = data.uuid,
                    name = data.name.orEmpty(),
                    email = data.email.orEmpty(),
                    nisnNik = data.nis_nik.orEmpty(),
                    nisNik = data.nis_nik.orEmpty(),
                    avatar = data.user_avatar_image.orEmpty(),
                    schoolId = schoolToStore.id,
                ),
            )
        }
    }

    /**
     * `login-account` success — doc §4.3 table. Fixes gap `02a` P7: reads `is_student`/`is_teacher`
     * independently off `rule` instead of guessing from role-name substrings.
     *
     * @return whether the app must force a "Ganti Password" prompt (`credential.using_default_password == "Y"`).
     */
    fun writeLoginAccount(response: LoginAccountApiResponse, selectedSchool: SchoolItem): Boolean {
        val data = response.data
        val rule = data?.rule ?: response.rule
        val ruleLabel = (data?.rule_label ?: response.rule_label).orEmpty()
        val product = data?.product_school ?: response.product_school
        val token = data?.meta?.token ?: response.meta?.token
        val credential = data?.credential ?: response.credential

        val isStudentRole = rule?.is_student ?: false
        val isTeacherRole = (rule?.is_teacher ?: false) && !isStudentRole

        val studentClass = data?.student?.student_class
        val classRoomId = studentClass?.class_room?.id ?: 0
        val classRoomName = studentClass?.class_room?.name?.takeIf { it.isNotBlank() }
            ?: studentClass?.name?.takeIf { it.isNotBlank() }
        val isHavingClass = if (isStudentRole) (studentClass?.id ?: 0) > 0 else isTeacherRole

        preference.putBoolean(SessionKeys.IS_SSO, false)
        preference.putInt(SessionKeys.USER_ID, data?.id ?: 0)
        preference.putString(SessionKeys.USER_UUID, data?.uuid.orEmpty())
        preference.putBoolean(SessionKeys.IS_STUDENT, isStudentRole)
        preference.putBoolean(SessionKeys.IS_TEACHER, isTeacherRole)
        preference.putBoolean(SessionKeys.IS_HAVING_CLASS, isHavingClass)
        preference.putString(SessionKeys.ROLES, ruleLabel)
        preference.putString(SessionKeys.SCHOOL_UUID, selectedSchool.uuid)
        preference.putString(SessionKeys.USER_TOKEN, token.orEmpty())
        preference.putBoolean(SessionKeys.LOGGED_IN, true)

        (data?.is_verified ?: response.is_verified)?.let { preference.putBoolean(SessionKeys.IS_VERIFIED, it) }
        (data?.is_email_verified ?: response.is_email_verified)?.let { preference.putBoolean(SessionKeys.IS_EMAIL_VERIFIED, it) }
        (data?.is_klaspay_activated ?: response.is_klaspay_activated)?.let { preference.putBoolean(SessionKeys.KLASPAY_ACTIVE, it) }

        val productSchool = product ?: id.diskola.app.dataclass.ResponData.ProductSchoolData()
        preference.putBoolean(SessionKeys.ONKLAS_LITE, productSchool.diskola_lite)
        preference.putBoolean(SessionKeys.ONKLAS_PRO, productSchool.diskola_pro)
        preference.putBoolean(SessionKeys.KLASTIME, productSchool.klastime)

        if (isStudentRole) {
            preference.putInt(SessionKeys.CLASS_ID, classRoomId)
            writeJson(
                SessionKeys.STUDENT_JSON, studentAdapter,
                SessionStudent(
                    id = data?.student?.id ?: 0,
                    nisn = data?.student?.nisn.orEmpty(),
                    nis = data?.student?.nis.orEmpty(),
                    name = data?.student?.name.orEmpty(),
                    className = classRoomName ?: NO_CLASS_LABEL,
                    classRoomId = classRoomId,
                    studentClassId = studentClass?.id ?: 0,
                ),
            )
        } else if (isTeacherRole) {
            writeJson(
                SessionKeys.TEACHER_JSON, teacherAdapter,
                SessionTeacher(
                    id = data?.teacher?.id ?: 0,
                    nik = data?.teacher?.nik.orEmpty(),
                    name = data?.name.orEmpty(),
                ),
            )
        }

        val schoolToStore = data?.school?.toSession(school) ?: selectedSchool.toSession()
        writeJson(SessionKeys.SCHOOL_JSON, schoolAdapter, schoolToStore)

        writeJson(
            SessionKeys.USER_JSON, userAdapter,
            SessionUser(
                id = data?.id ?: 0,
                uuid = data?.uuid.orEmpty(),
                name = data?.name.orEmpty(),
                email = data?.email.orEmpty(),
                nisnNik = data?.nis_nik.orEmpty(),
                nisNik = data?.nis_nik.orEmpty(),
                avatar = data?.user_avatar_image.orEmpty(),
                username = data?.user_username.orEmpty(),
                schoolId = schoolToStore.id,
            ),
        )

        return credential?.using_default_password.orEmpty().equals("Y", ignoreCase = true)
    }

    /**
     * `login-sso` (checkEmail) success — doc §5.2. Only writes what the legacy `ApiWrapper`/
     * `LoginViewModel` actually wrote at this step; role/school confirmation happens one step
     * later in [writeSsoSchool].
     */
    fun writeSsoCheckEmail(ssoData: LoginSsoUserData?, token: String) {
        val roleLabel = when {
            ssoData?.is_student == true -> "Student"
            ssoData?.is_teacher == true -> "Teacher"
            else -> "Guest"
        }
        preference.putString(SessionKeys.ROLE_LABEL, roleLabel)
        preference.putInt(SessionKeys.USER_ID, ssoData?.id ?: 0)
        preference.putBoolean(SessionKeys.KLASPAY_ACTIVE, ssoData?.is_klaspay_activated ?: false)
        preference.putBoolean(SessionKeys.IS_EMAIL_VERIFIED, true)
        preference.putString(SessionKeys.USER_TOKEN, token)

        if (ssoData != null && ssoData.username.isNotBlank()) {
            writeJson(
                SessionKeys.USER_JSON, userAdapter,
                SessionUser(id = ssoData.id, name = ssoData.username, email = ssoData.email, avatar = ssoData.image),
            )
            ssoData.school?.let { writeJson(SessionKeys.SCHOOL_JSON, schoolAdapter, it.toSession()) }
        }
    }

    /**
     * `login-sso/school` success — doc §5.3. Deliberately does **not** write
     * `onklas_lite`/`onklas_pro`/`klastime`, and does **not** reset `default_pass` (decision Q5:
     * tiru — those simply aren't part of the SSO write in the legacy app), and does **not** write
     * the `user` pref (legacy `ApiWrapper.kt` has that call commented out).
     *
     * Role flags come straight from [checkEmailData] — for a true Guest, `checkEmail` already
     * built a synthetic `is_student=is_teacher=false` record (doc §5.2), so there's no separate
     * "force guest" branch needed here; a real student/teacher who simply has no school picks one
     * through this same path (the school-picker "daftar" branch, decision Q2) and keeps their role.
     *
     * @return the resulting `klaspayActive` value, so the caller can decide Main vs Klaspay activation.
     */
    fun writeSsoSchool(
        response: LoginSsoSchoolData,
        checkEmailData: LoginSsoUserData?,
        selectedSchoolFallback: SchoolItem?,
    ): Boolean {
        val isStudentRole = checkEmailData?.is_student == true
        val isTeacherRole = checkEmailData?.is_teacher == true && !isStudentRole
        val classRoomId = checkEmailData?.current_class?.class_room?.id ?: 0
        val isHavingClass = if (isStudentRole) classRoomId > 0 else isTeacherRole

        val klaspayActive = response.is_klaspay_activated == 1

        preference.putBoolean(SessionKeys.IS_SSO, true)
        preference.putInt(SessionKeys.USER_ID, response.id)
        preference.putString(SessionKeys.USER_UUID, response.uuid)
        preference.putBoolean(SessionKeys.IS_STUDENT, isStudentRole)
        preference.putBoolean(SessionKeys.IS_TEACHER, isTeacherRole)
        preference.putBoolean(SessionKeys.IS_HAVING_CLASS, isHavingClass)
        preference.putString(SessionKeys.ROLES, if (isStudentRole) "Student" else if (isTeacherRole) "Teacher" else "Guest")
        preference.putBoolean(SessionKeys.KLASPAY_ACTIVE, klaspayActive)
        preference.putBoolean(SessionKeys.IS_VERIFIED, false)
        preference.putBoolean(SessionKeys.IS_EMAIL_VERIFIED, true)
        // Klaspay not yet active → gate stays closed (logged_in/is_active=false) until activation
        // finishes (doc §5.3, §6) — that's what routes the user to KlaspayActivation instead of Main.
        preference.putBoolean(SessionKeys.LOGGED_IN, klaspayActive)
        preference.putBoolean(SessionKeys.IS_ACTIVE, klaspayActive)

        if (isStudentRole) preference.putInt(SessionKeys.CLASS_ID, classRoomId)

        val namedSchool = checkEmailData?.school?.takeIf { it.name.isNotBlank() }
        val schoolToStore = when {
            namedSchool != null -> namedSchool.toSession()
            else -> response.toSession(selectedSchoolFallback?.toSession() ?: school)
        }
        writeJson(SessionKeys.SCHOOL_JSON, schoolAdapter, schoolToStore)
        preference.putString(SessionKeys.SCHOOL_UUID, schoolToStore.uuid)

        return klaspayActive
    }

    /** Called once Klaspay activation finishes for an SSO user (doc §6) — opens the gate that
     * [writeSsoSchool] left closed while the wallet wasn't active yet. */
    fun markKlaspayActivated(fromSso: Boolean) {
        preference.putBoolean(SessionKeys.KLASPAY_ACTIVE, true)
        if (fromSso) {
            preference.putBoolean(SessionKeys.LOGGED_IN, true)
            preference.putBoolean(SessionKeys.IS_ACTIVE, true)
        }
    }

    private fun <T> readJson(key: String, adapter: JsonAdapter<T>): T? {
        val raw = preference.getString(key)
        if (raw.isBlank()) return null
        return try {
            adapter.fromJson(raw)
        } catch (e: Exception) {
            null
        }
    }

    private fun <T> writeJson(key: String, adapter: JsonAdapter<T>, value: T) {
        preference.putString(key, adapter.toJson(value))
    }
}
