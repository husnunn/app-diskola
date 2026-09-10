package id.app.education.viewmodel

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.app.education.apiservice.AuthApiService
import id.app.education.apiservice.CommonApiService
import id.app.education.dataclass.ResponData.CheckAccountRequest
import id.app.education.dataclass.ResponData.CheckAccountUserData
import id.app.education.dataclass.ResponData.LoginAccountApiResponse
import id.app.education.dataclass.ResponData.LoginAccountRequest
import id.app.education.dataclass.ResponData.PolicyResponse
import id.app.education.dataclass.ResponData.SchoolItem
import id.app.education.utils.PreferenceClass
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val commonApiService: CommonApiService,
    private val preference: PreferenceClass,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private val _schools = MutableStateFlow<List<SchoolItem>>(emptyList())
    val schools: StateFlow<List<SchoolItem>> = _schools.asStateFlow()

    private val _selectedSchool = MutableStateFlow<SchoolItem?>(null)
    val selectedSchool: StateFlow<SchoolItem?> = _selectedSchool.asStateFlow()

    private val _checkAccountResult = MutableStateFlow<CheckAccountUserData?>(null)
    val checkAccountResult: StateFlow<CheckAccountUserData?> = _checkAccountResult.asStateFlow()

    private val _loginAccountResult = MutableStateFlow<LoginAccountApiResponse?>(null)
    val loginAccountResult: StateFlow<LoginAccountApiResponse?> = _loginAccountResult.asStateFlow()

    private val _successMessage = MutableStateFlow("")
    val successMessage: StateFlow<String> = _successMessage.asStateFlow()

    private val _policy = MutableStateFlow<PolicyResponse?>(null)
    val policy: StateFlow<PolicyResponse?> = _policy.asStateFlow()

    private var searchJob: Job? = null

    fun fetchPolicy() {
        launchWithHandling {
            _policy.value = commonApiService.policy()
        }
    }

    fun searchSchools(keyword: String) {
        searchJob?.cancel()
        searchJob = launchWithHandling(
            showLoading = false,
            showError = false
        ) {
            delay(300)

            if (keyword.isBlank()) {
                _schools.value = emptyList()
                return@launchWithHandling
            }

            val response = authApiService.getSchools(q = keyword, name = keyword)
            _schools.value = response.data
        }
    }

    fun setSelectedSchool(item: SchoolItem?) {
        _selectedSchool.value = item
    }

    fun checkAccount(nisNik: String) {
        val school = _selectedSchool.value
        if (school == null) {
            emitError("Silakan pilih sekolah terlebih dahulu")
            return
        }

        if (nisNik.isBlank()) {
            emitError("NIS/NIK/NISN wajib diisi")
            return
        }

        launchWithHandling(customMessage = "Gagal cek akun") {
            val response = authApiService.checkAccount(
                CheckAccountRequest(
                    school_id = school.uuid,
                    nisn_nik = nisNik
                )
            )
            _checkAccountResult.value = response.data
        }
    }

    fun loginAccount(userUuid: String, password: String) {
        if (userUuid.isBlank()) {
            emitError("UUID pengguna tidak valid")
            return
        }

        if (password.isBlank()) {
            emitError("Password wajib diisi")
            return
        }

        launchWithHandling(customMessage = "Cek Password atau Akun Anda") {
            val response = authApiService.loginAccount(
                LoginAccountRequest(
                    uuid = userUuid,
                    password = password
                )
            )
            _loginAccountResult.value = response
            _successMessage.value = "Login berhasil"
        }
    }

    /**
     * Ported verbatim from the legacy `LoginPasswordActivity.saveSession()` — same field mapping
     * and fallback precedence, just moved out of the Activity. [fallbackAccount] plays the role
     * the Intent extras used to: known-good values from the check-account step, used only when
     * the login response itself doesn't carry them.
     */
    fun saveSession(response: LoginAccountApiResponse, fallbackAccount: CheckAccountUserData?): Boolean {
        val user = response.data
        val token = user?.meta?.token ?: response.meta?.token
        val rule = user?.rule ?: response.rule
        val product = user?.product_school ?: response.product_school
        val credential = user?.credential ?: response.credential
        val ruleLabel = user?.rule_label ?: response.rule_label
        val mustChangeDefaultPassword = credential?.using_default_password == "Y"

        Timber.e("EXTRACTED TOKEN: $token")

        val fallbackIsTeacher = fallbackAccount?.roles?.mapNotNull { it.name?.lowercase() }
            ?.any { it.contains("teacher") || it.contains("guru") } ?: false
        val fallbackIsStudent = fallbackAccount?.roles?.mapNotNull { it.name?.lowercase() }
            ?.any { it.contains("student") || it.contains("siswa") } ?: false

        val loginRoles = user?.roles?.mapNotNull { it.name?.lowercase() } ?: emptyList()
        val rolesIsTeacher = loginRoles.any { it.contains("teacher") || it.contains("guru") }
        val rolesIsStudent = loginRoles.any { it.contains("student") || it.contains("siswa") }

        val isTeacherFromSource = (rule?.is_teacher ?: false) || fallbackIsTeacher || rolesIsTeacher
        val isStudentFromSource = (rule?.is_student ?: false) || fallbackIsStudent || rolesIsStudent

        // Prioritaskan Student jika keduanya true, agar tidak terjadi tumpukan menu/endpoint
        val resolvedIsStudent = isStudentFromSource
        val resolvedIsTeacher = isTeacherFromSource && !isStudentFromSource

        // save to plain sharedprefs for backward compatibility
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
            .putString("token", token ?: "")
            .putBoolean("is_logged_in", true)
            .putString("user_uuid", user?.uuid ?: fallbackAccount?.uuid ?: "")
            .putString("user_name", user?.name ?: fallbackAccount?.name ?: "")
            .putString("user_email", user?.email ?: fallbackAccount?.email ?: "")
            .putString("user_avatar_image", user?.user_avatar_image ?: fallbackAccount?.user_avatar_image ?: "")
            .putString("school_uuid", user?.school?.uuid ?: fallbackAccount?.school?.uuid ?: "")
            .putString("school_name", user?.school?.name ?: fallbackAccount?.school?.name ?: "")
            .putString("rule_label", ruleLabel ?: "")
            .putBoolean("is_student", resolvedIsStudent)
            .putBoolean("is_teacher", resolvedIsTeacher)
            .apply()

        try {
            Timber.e("SAVING TOKEN: $token")
            preference.putString("user_token", token ?: "")
            preference.putBoolean("logged_in", true)

            user?.let {
                preference.putInt("user_id", it.id)
                preference.putString("user_uuid", it.uuid)
            }

            preference.putBoolean("is_student", resolvedIsStudent)
            preference.putBoolean("is_teacher", resolvedIsTeacher)
            preference.putBoolean("is_librarian", rule?.is_librarian ?: false)

            preference.putString("roles", ruleLabel ?: "")

            val isVerified = user?.is_verified ?: response.is_verified
            val isEmailVerified = user?.is_email_verified ?: response.is_email_verified
            val isKlaspayActivated = user?.is_klaspay_activated ?: response.is_klaspay_activated

            isVerified?.let { preference.putBoolean("is_verified", it) }
            isEmailVerified?.let { preference.putBoolean("is_email_verified", it) }
            isKlaspayActivated?.let { preference.putBoolean("klaspayActive", it) }

            product?.let {
                preference.putBoolean("onklas_lite", it.diskola_lite ?: false)
                preference.putBoolean("onklas_pro", it.diskola_pro ?: false)
                preference.putBoolean("klastime", it.klastime ?: false)
            }

            credential?.let {
                preference.putBoolean("default_pass", it.using_default_password == "Y")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return mustChangeDefaultPassword
    }
}
