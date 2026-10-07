package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.PresensiDayTable
import id.diskola.app.dataclass.ResponData.PresensiRekapTable
import id.diskola.app.repository.IzinRepository
import id.diskola.app.repository.OffsiteAvailability
import id.diskola.app.repository.PresensiRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PresensiRules
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/** What "Lakukan Presensi" does for this user right now. */
sealed interface PresensiAction {
    data class Blocked(val message: String) : PresensiAction
    data object Onsite : PresensiAction

    /** Teacher with Dinas Luar available: pick "Presensi di Sekolah" or "Presensi Dinas Luar". */
    data object ChooseType : PresensiAction
    data class OffsiteBlocked(val message: String) : PresensiAction
}

/**
 * Presensi shell: Data Absensi (month list, Room first then one fetch) and Rekap (cache-first per
 * year). Loads once in `init`; coming back to the screen refreshes the open month once — legacy did
 * that on the very first resume too, fetching twice on open.
 */
@HiltViewModel
class PresensiViewModel @Inject constructor(
    private val repository: PresensiRepository,
    private val izinRepository: IzinRepository,
) : BaseViewModel() {

    val isStudent: Boolean get() = repository.isStudent

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _days = MutableStateFlow<List<PresensiDayTable>>(emptyList())
    val days: StateFlow<List<PresensiDayTable>> = _days.asStateFlow()

    private val _canPerform = MutableStateFlow(true)
    val canPerform: StateFlow<Boolean> = _canPerform.asStateFlow()

    private val _offsite = MutableStateFlow(OffsiteAvailability.Disabled)
    val offsite: StateFlow<OffsiteAvailability> = _offsite.asStateFlow()

    private val _year = MutableStateFlow(LocalDate.now().year)
    val year: StateFlow<Int> = _year.asStateFlow()

    private val _rekap = MutableStateFlow<List<PresensiRekapTable>>(emptyList())
    val rekap: StateFlow<List<PresensiRekapTable>> = _rekap.asStateFlow()

    private val _rekapLoading = MutableStateFlow(false)
    val rekapLoading: StateFlow<Boolean> = _rekapLoading.asStateFlow()

    /** False while a Rekap fetch runs and for 5 s after it (legacy cooldown against rapid year flips). */
    private val _rekapFilterEnabled = MutableStateFlow(true)
    val rekapFilterEnabled: StateFlow<Boolean> = _rekapFilterEnabled.asStateFlow()

    private var monthJob: Job? = null
    private var rekapJob: Job? = null
    private var firstResumeSeen = false
    private var rekapStarted = false

    init {
        loadMonth(_month.value)
    }

    // ---- Data Absensi ----

    fun selectMonth(month: YearMonth) {
        _month.value = month
        loadMonth(month)
    }

    fun shiftMonth(delta: Long) = selectMonth(_month.value.plusMonths(delta))

    fun refresh() = loadMonth(_month.value)

    /** ON_RESUME: the first one is the open itself (already loading); later ones follow a child screen. */
    fun onScreenResumed() {
        if (!firstResumeSeen) {
            firstResumeSeen = true
            return
        }
        loadMonth(_month.value)
    }

    private fun loadMonth(month: YearMonth) {
        monthJob?.cancel()
        clearError()
        monthJob = viewModelScope.launch {
            _days.value = repository.readMonth(month)
            refreshGate()
            _loading.value = true
            try {
                _days.value = repository.fetchMonth(month)
                refreshGate()
                if (_days.value.isEmpty()) emitError("Data kehadiran tidak tersedia")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _loading.value = false
            }
            if (!repository.isStudent) _offsite.value = repository.offsiteAvailability()
        }
    }

    /** Today's izin (pending/approved) closes the button; the cached izin wins over the month row. */
    private suspend fun refreshGate() {
        val today = LocalDate.now().toString()
        val status = izinRepository.todayStatus()?.takeIf { it.isNotBlank() }
            ?: repository.day(today)?.leaveRequestStatus
        _canPerform.value = PresensiRules.canPerformAttendance(status)
    }

    fun onPresensiClick(): PresensiAction {
        if (!_canPerform.value) return PresensiAction.Blocked("Pengajuan izin/sakit hari ini masih diproses atau sudah disetujui.")
        if (repository.isStudent) return PresensiAction.Onsite
        val offsite = _offsite.value
        return when {
            offsite.enabled && offsite.allowAttendance -> PresensiAction.ChooseType
            offsite.enabled -> PresensiAction.OffsiteBlocked(offsiteBlockMessage(offsite))
            else -> PresensiAction.Onsite
        }
    }

    private fun offsiteBlockMessage(offsite: OffsiteAvailability): String = when {
        offsite.message.contains("Workgroup", ignoreCase = true) -> "Maaf anda belum memiliki pengaturan jam"
        offsite.responseText.isNotBlank() -> offsite.responseText
        offsite.message.isNotBlank() -> offsite.message
        else -> "Presensi dinas luar tidak dapat dilakukan saat ini"
    }

    // ---- Rekap ----

    fun shiftYear(delta: Int) {
        if (!_rekapFilterEnabled.value) return
        _year.value += delta
        loadRekap(force = false)
    }

    fun refreshRekap() = loadRekap(force = true)

    /** Called when the Rekap tab is first shown. */
    fun ensureRekap() {
        if (rekapStarted) return
        rekapStarted = true
        loadRekap(force = false)
    }

    private fun loadRekap(force: Boolean) {
        rekapJob?.cancel()
        val year = _year.value
        rekapJob = viewModelScope.launch {
            val cached = repository.readRekap(year)
            // Always replace what is shown — never leave the previous year's rows under the new label.
            _rekap.value = cached
            if (cached.isNotEmpty() && !force) {
                _rekapFilterEnabled.value = true
                return@launch
            }
            _rekapLoading.value = true
            _rekapFilterEnabled.value = false
            try {
                _rekap.value = repository.fetchRekap(year)
                if (_rekap.value.isEmpty()) emitError("Data rekap tidak tersedia")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _rekapLoading.value = false
            }
            delay(5_000)
            _rekapFilterEnabled.value = true
        }
    }
}
