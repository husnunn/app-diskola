package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.AgendaDay
import id.diskola.app.dataclass.ResponData.AgendaUiStatus
import id.diskola.app.dataclass.ResponData.StaffAgendaItem
import id.diskola.app.repository.AgendaRepository
import id.diskola.app.utils.AppErrorHandler
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface AgendaTapAction {
    data class Detail(val item: StaffAgendaItem) : AgendaTapAction
    data class CheckIn(val item: StaffAgendaItem) : AgendaTapAction
    data class CheckOut(val item: StaffAgendaItem) : AgendaTapAction
    data object NeedPresensi : AgendaTapAction
}

/**
 * Agenda Mingguan list. The first load happens exactly once, in `init` (legacy fetched twice on
 * open: the date observer and `onResume` both ran). Afterwards only three things refetch: a cache
 * miss on a newly picked date, pull-to-refresh, and coming back to today's date from another screen.
 */
@HiltViewModel
class AgendaMingguanViewModel @Inject constructor(
    private val repository: AgendaRepository,
) : BaseViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _day = MutableStateFlow<AgendaDay?>(null)
    val day: StateFlow<AgendaDay?> = _day.asStateFlow()

    private var loadJob: Job? = null
    private var firstResumeSeen = false

    init {
        load(_selectedDate.value, force = false)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        load(date, force = false)
    }

    /** Month picker: the current month lands on today, any other month on its 1st. */
    fun selectMonth(month: YearMonth) {
        val today = LocalDate.now()
        selectDate(if (month == YearMonth.from(today)) today else month.atDay(1))
    }

    fun refresh() = load(_selectedDate.value, force = true)

    /** Called on every ON_RESUME of the screen; the very first one is the open itself, already loaded. */
    fun onScreenResumed() {
        if (!firstResumeSeen) {
            firstResumeSeen = true
            return
        }
        if (_selectedDate.value == LocalDate.now()) load(_selectedDate.value, force = true)
    }

    fun resolveTap(item: StaffAgendaItem, day: AgendaDay): AgendaTapAction {
        val usable = _selectedDate.value == LocalDate.now() && day.agendaEnabled
        return when (item.uiStatus) {
            AgendaUiStatus.PENDING -> when {
                !usable -> AgendaTapAction.Detail(item)
                day.gate.gateIn == null -> AgendaTapAction.NeedPresensi
                else -> AgendaTapAction.CheckIn(item)
            }
            AgendaUiStatus.NEED_CHECKOUT -> if (usable) AgendaTapAction.CheckOut(item) else AgendaTapAction.Detail(item)
            else -> AgendaTapAction.Detail(item)
        }
    }

    private fun load(date: LocalDate, force: Boolean) {
        loadJob?.cancel()
        clearError()
        loadJob = viewModelScope.launch {
            val key = date.toString()
            val cached = repository.readLocal(key)
            _day.value = cached
            if (cached != null && !force) return@launch
            _loading.value = true
            try {
                _day.value = repository.fetchDay(key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                // Legacy shows the failure only when there is nothing cached to fall back on.
                if (cached == null) emitError(AppErrorHandler.getMessage(e).ifBlank { "Gagal memuat agenda" })
            } finally {
                _loading.value = false
            }
        }
    }
}
