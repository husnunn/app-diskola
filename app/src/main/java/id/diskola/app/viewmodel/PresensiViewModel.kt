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
import id.diskola.app.utils.ServerText
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

    /** True from the start: the tab asks for the first load from a LaunchedEffect, a frame after it shows, and an empty list in that frame is not an empty result. */
    private val _rekapLoading = MutableStateFlow(true)
    val rekapLoading: StateFlow<Boolean> = _rekapLoading.asStateFlow()

    /** False while a Rekap fetch runs and for 5 s after it (legacy cooldown against rapid year flips). */
    private val _rekapFilterEnabled = MutableStateFlow(true)
    val rekapFilterEnabled: StateFlow<Boolean> = _rekapFilterEnabled.asStateFlow()

    private var monthJob: Job? = null
    private var rekapJob: Job? = null
    private var firstResumeSeen = false
    private var rekapStarted = false

    /** Each load takes a number; only the latest one may lower its loading flag, so a cancelled earlier load cannot drop the flag of the one that replaced it. */
    private var monthGeneration = 0
    private var rekapGeneration = 0

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
        val generation = ++monthGeneration
        // Up before the cache is read: until then an empty month is unknown, not empty (the screen would flash its empty state).
        _loading.value = true
        monthJob = viewModelScope.launch {
            try {
                _days.value = repository.readMonth(month)
                refreshGate()
                try {
                    _days.value = repository.fetchMonth(month)
                    refreshGate()
                    if (_days.value.isEmpty()) emitError("Data kehadiran tidak tersedia")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e)
                    emitError(AppErrorHandler.getMessage(e))
                }
            } finally {
                if (generation == monthGeneration) _loading.value = false
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
        offsite.responseText.isNotBlank() -> ServerText.localize(offsite.responseText)
        offsite.message.isNotBlank() -> ServerText.localize(offsite.message)
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
        val generation = ++rekapGeneration
        // Nothing on screen yet: hold the skeleton while the cache is read. With rows showing, a flag flip here would flash the refresh spinner.
        if (_rekap.value.isEmpty()) _rekapLoading.value = true
        rekapJob = viewModelScope.launch {
            try {
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
                }
            } finally {
                if (generation == rekapGeneration) _rekapLoading.value = false
            }
            delay(5_000)
            _rekapFilterEnabled.value = true
        }
    }
}
