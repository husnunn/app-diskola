package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.repository.AgendaRepository
import id.diskola.app.repository.AuthRepository
import id.diskola.app.repository.FeatureGateRepository
import id.diskola.app.repository.JurnalRepository
import id.diskola.app.utils.JurnalRow
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.session.SessionManager
import id.diskola.app.utils.session.SessionStore
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * `PembelajaranPage` hub (doc `05-pembelajaran-materi-tugas.md` §1). Scope for this pass (agreed
 * with user): menu grid correctness, guest popup, the real "Kelas Berlangsung" card, and the
 * inactive-account check, and the teacher "Agenda Mingguan" missing-count badge. The notification
 * badge's `payment/wallet`→`notification/summary` chain needs endpoints that don't exist anywhere
 * in this app yet (confirmed by a full-repo search) and is deferred — see doc `05a`.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val jurnalRepository: JurnalRepository,
    private val agendaRepository: AgendaRepository,
    private val featureGate: FeatureGateRepository,
    private val sessionManager: SessionManager,
    val sessionStore: SessionStore,
) : BaseViewModel() {

    /** The class session whose hour is running right now (Hub "Kelas Berlangsung"), student-aware via [JurnalRules]. */
    private val _ongoingClass = MutableStateFlow<JurnalRow?>(null)
    val ongoingClass: StateFlow<JurnalRow?> = _ongoingClass.asStateFlow()

    private val _scheduleLoaded = MutableStateFlow(false)
    val scheduleLoaded: StateFlow<Boolean> = _scheduleLoaded.asStateFlow()

    private val _accountInactive = MutableStateFlow(false)
    val accountInactive: StateFlow<Boolean> = _accountInactive.asStateFlow()

    private val _featureChecking = MutableStateFlow(false)
    val featureChecking: StateFlow<Boolean> = _featureChecking.asStateFlow()

    private val _featureUnavailable = MutableStateFlow<FeatureUnavailable?>(null)
    val featureUnavailable: StateFlow<FeatureUnavailable?> = _featureUnavailable.asStateFlow()

    /** Teacher hub badge: today's `summary.missing` sessions (0 when agendas are off or the call fails). */
    private val _agendaMissing = MutableStateFlow(0)
    val agendaMissing: StateFlow<Int> = _agendaMissing.asStateFlow()

    /** `sessionStore` is a plain SharedPreferences reader, not an observable — bumping this after
     * `writeCheckAccount` lands is what tells the Composable to recompose and re-read
     * `isHavingClass`/`isStudent`/etc. Reading it (even unused) in the UI is what wires that up. */
    private val _sessionVersion = MutableStateFlow(0)
    val sessionVersion: StateFlow<Int> = _sessionVersion.asStateFlow()

    /** Re-run on every landing on Home and every foreground resume (doc §1.4) — `check-account`
     * refreshes `is_active`/class/school display data, the schedule call finds "Kelas Berlangsung". */
    fun refresh() {
        refreshProfile()
        loadSchedule()
        if (sessionStore.isTeacher) loadAgendaBadge()
    }

    private fun loadAgendaBadge() {
        launchWithHandling(showLoading = false, showError = false) {
            _agendaMissing.value = agendaRepository.missingToday()
        }
    }

    private fun refreshProfile() {
        val school = sessionStore.school
        val nisn = sessionStore.student?.nis?.takeIf { it.isNotBlank() } ?: sessionStore.user.nisNik
        if (school.uuid.isBlank() || nisn.isBlank()) return
        launchWithHandling(showLoading = false, showError = false) {
            val response = authRepository.checkAccount(nisn, school.uuid)
            sessionStore.writeCheckAccount(response, SchoolItem(id = school.id, uuid = school.uuid, name = school.name))
            if (!sessionStore.isActive) _accountInactive.value = true
            _sessionVersion.value += 1
        }
    }

    private fun loadSchedule() {
        launchWithHandling(showLoading = false, showError = false) {
            val now = LocalTime.now()
            val rows = JurnalRules.buildRows(jurnalRepository.schedule(), sessionStore.isStudent, now)
            _ongoingClass.value = JurnalRules.currentRow(rows, now)
            _scheduleLoaded.value = true
        }
    }

    /**
     * Server-side feature switch (`check-feature-availability`) in front of Presensi/Jurnal. Checked
     * once per tap (legacy checked again in the Activity's onCreate); an error counts as available.
     */
    fun openFeature(name: String, onAvailable: () -> Unit) {
        if (_featureChecking.value) return
        viewModelScope.launch {
            _featureChecking.value = true
            val result = featureGate.check(name)
            _featureChecking.value = false
            if (result.available) onAvailable() else _featureUnavailable.value = FeatureUnavailable(name, result.message.orEmpty())
        }
    }

    fun dismissFeatureUnavailable() {
        _featureUnavailable.value = null
    }

    /** "Baik" on the inactive-account alert (doc §1.4) — logs out then hands control back to the
     * caller to navigate to Login, mirroring `AkunViewModel.logout`'s split. */
    fun confirmInactiveLogout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionManager.logout()
            onComplete()
        }
    }
}

data class FeatureUnavailable(val name: String, val message: String)

/** "07:30:00" / "07:30" → "07.30", matching the legacy hub's clock display convention. */
fun formatScheduleClock(raw: String?): String = raw.orEmpty().split(":").take(2).joinToString(".")
