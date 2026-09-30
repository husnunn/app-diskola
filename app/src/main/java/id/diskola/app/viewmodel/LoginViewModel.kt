package id.diskola.app.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.R
import id.diskola.app.apiservice.ApiException
import id.diskola.app.apiservice.CommonApiService
import id.diskola.app.dataclass.ResponData.CheckAccountUserData
import id.diskola.app.dataclass.ResponData.PolicyResponse
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.repository.AuthRepository
import id.diskola.app.repository.SchoolRepository
import id.diskola.app.utils.session.AuthError
import id.diskola.app.utils.session.AuthErrorMapper
import id.diskola.app.utils.session.FcmTopicManager
import id.diskola.app.utils.session.SessionKeys
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** What happens after `login-account` succeeds — doc `02-auth-login-sesi.md` §4.3 step 4. */
sealed interface LoginOutcome {
    data object MustChangePassword : LoginOutcome
    data object Success : LoginOutcome
}

/**
 * Backs the whole NISN+password auth graph (`LoginScreen` → `SchoolPickerSheet` → `PasswordScreen`)
 * — one instance shared across that back-stack, the same role the legacy Activity-scoped
 * `LoginViewModel` played across `LoginForm`/`LoginProcess`.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commonApiService: CommonApiService,
    private val authRepository: AuthRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionStore: SessionStore,
    private val fcmTopicManager: FcmTopicManager,
) : BaseViewModel() {

    private val _policy = MutableStateFlow<PolicyResponse?>(null)
    val policy: StateFlow<PolicyResponse?> = _policy.asStateFlow()

    private val _schoolQuery = MutableStateFlow("")
    val schoolQuery: StateFlow<String> = _schoolQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val schools: StateFlow<List<SchoolItem>> = _schoolQuery
        .flatMapLatest { query -> schoolRepository.observe(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _schoolListLoading = MutableStateFlow(false)
    val schoolListLoading: StateFlow<Boolean> = _schoolListLoading.asStateFlow()

    private var hasNextSchoolPage = true
    private var loadingMoreSchools = false

    private val _selectedSchool = MutableStateFlow<SchoolItem?>(null)
    val selectedSchool: StateFlow<SchoolItem?> = _selectedSchool.asStateFlow()

    private val _checkAccountResult = MutableStateFlow<CheckAccountUserData?>(null)
    val checkAccountResult: StateFlow<CheckAccountUserData?> = _checkAccountResult.asStateFlow()

    private val _authError = MutableStateFlow<AuthError?>(null)
    val authError: StateFlow<AuthError?> = _authError.asStateFlow()

    private val _loginOutcome = MutableStateFlow<LoginOutcome?>(null)
    val loginOutcome: StateFlow<LoginOutcome?> = _loginOutcome.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    fun consumeLoginOutcome() {
        _loginOutcome.value = null
    }

    /** Fetched once when the T&K dialog opens — the legacy app's own anti-pattern was fetching
     * `policy` twice (splash + dialog); here it's only ever the dialog. */
    fun fetchPolicy() {
        launchWithHandling {
            _policy.value = commonApiService.policy()
        }
    }

    // ---- School picker (doc §4.2) ----

    fun openSchoolPicker() {
        if (schools.value.isNotEmpty()) return
        viewModelScope.launch {
            _schoolListLoading.value = true
            try {
                schoolRepository.ensureFirstPage()
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _schoolListLoading.value = false
            }
        }
    }

    fun onSchoolQueryChange(query: String) {
        _schoolQuery.value = query
    }

    /** Only while the search box is empty and a previous page came back full — doc's
     * `onItemAtEndLoaded`/`hasNextSchool` boundary-callback behaviour. */
    fun loadMoreSchools() {
        if (_schoolQuery.value.isNotBlank() || loadingMoreSchools || !hasNextSchoolPage) return
        loadingMoreSchools = true
        viewModelScope.launch {
            try {
                hasNextSchoolPage = schoolRepository.loadNextPage(schools.value.size)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                loadingMoreSchools = false
            }
        }
    }

    fun refreshSchools() {
        viewModelScope.launch {
            _schoolListLoading.value = true
            try {
                hasNextSchoolPage = schoolRepository.refresh()
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _schoolListLoading.value = false
            }
        }
    }

    fun setSelectedSchool(item: SchoolItem?) {
        _selectedSchool.value = item
    }

    // ---- check-account / login-account (doc §4.1/§4.3) ----

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

        _loading.value = true
        viewModelScope.launch {
            try {
                val response = authRepository.checkAccount(nisNik, school.uuid)
                sessionStore.writeCheckAccount(response, school)
                _checkAccountResult.value = response.data
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _errorMessage.value = backendMessage(e, "Terjadi kesalahan saat login. Silakan coba lagi.")
            } finally {
                _loading.value = false
            }
        }
    }

    fun loginAccount(password: String) {
        val account = _checkAccountResult.value
        if (account == null) {
            emitError("Data akun tidak ditemukan — kembali ke form login")
            return
        }
        if (password.isBlank()) {
            emitError("Password wajib diisi")
            return
        }
        val school = _selectedSchool.value

        _loading.value = true
        _authError.value = null
        viewModelScope.launch {
            try {
                val response = authRepository.loginAccount(account.uuid, password)
                val mustChangePassword = sessionStore.writeLoginAccount(response, school ?: SchoolItem(id = 0, uuid = account.school?.uuid.orEmpty(), name = account.school?.name.orEmpty()))

                try {
                    FirebaseAnalytics.getInstance(context).setUserId(sessionStore.userId.toString())
                } catch (e: Exception) {
                    Timber.e(e)
                }
                authRepository.setupFcmToken(sessionStore.userUuid, sessionStore.fcmToken)
                fcmTopicManager.subscribeForSession(
                    sessionStore.userUuid,
                    sessionStore.userId,
                    sessionStore.student?.classRoomId ?: 0,
                    sessionStore.school.uuid,
                )

                if (mustChangePassword) {
                    postChangePasswordNotification()
                    _loginOutcome.value = LoginOutcome.MustChangePassword
                } else {
                    _loginOutcome.value = LoginOutcome.Success
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _authError.value = AuthErrorMapper.map(e)
            } finally {
                _loading.value = false
            }
        }
    }

    /** Local "Ganti Password" notification — doc §4.3 step 4. Tapping it is wired at the
     * `AndroidManifest`/notification-channel level to just open the app; deep-linking straight
     * into `AkunSub.Pass` is part of the notification-routing work in doc 03, out of scope here. */
    private fun postChangePasswordNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val subject = if (sessionStore.isStudent) "kamu" else "Anda"
        val channelId = context.getString(R.string.app_name)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ganti Password")
            .setContentText("Perhatian, $subject masih menggunakan password default, silahkan ubah password akun $subject untuk meningkatkan keamanan data")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Perhatian, $subject masih menggunakan password default, silahkan ubah password akun $subject untuk meningkatkan keamanan data"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(CHANGE_PASSWORD_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Timber.e(e)
        }
    }

    private fun backendMessage(throwable: Throwable, fallback: String): String {
        val apiException = throwable as? ApiException
        return apiException?.message?.takeIf { it.isNotBlank() && it != "null" } ?: fallback
    }

    companion object {
        private const val CHANGE_PASSWORD_NOTIFICATION_ID = 9001
    }
}
