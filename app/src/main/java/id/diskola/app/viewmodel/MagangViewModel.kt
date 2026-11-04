package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.repository.MagangRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Magang (internship) state shared by the shell and the screens it opens. It is scoped to the `Route.Magang` back stack entry, so a check-in,
 * report or izin screen reads and refreshes the same lists the shell shows (legacy did this with an activity-scoped ViewModel). Nothing is
 * cached: a failed fetch before the first answer is an error state with a retry, a failed refresh keeps what is on screen and shows a banner.
 */
@HiltViewModel
class MagangViewModel @Inject constructor(
    private val repository: MagangRepository,
) : BaseViewModel() {

    private val _schedules = MutableStateFlow<List<MagangScheduleItem>>(emptyList())
    val schedules: StateFlow<List<MagangScheduleItem>> = _schedules.asStateFlow()

    /** The schedule has been answered once; before that an empty list only means "not asked yet". */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** The day whose cards the Jadwal tab shows; the strip shows its month. Legacy opened on today too. */
    private val _selected = MutableStateFlow(LocalDate.now())
    val selected: StateFlow<LocalDate> = _selected.asStateFlow()

    private var refreshJob: Job? = null
    private var firstResumeSeen = false

    init {
        refresh()
    }

    fun selectDate(date: LocalDate) {
        _selected.value = date
    }

    /** Legacy jumped to the 1st of whichever month the picker returned, even the one already shown. */
    fun selectMonth(month: YearMonth) {
        _selected.value = month.atDay(1)
    }

    /**
     * A newer refresh replaces one still in flight: after a check-in or a report the answer to ask for is the one sent after the write, and
     * whichever request finished last must not decide what the list shows or when the spinner stops.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _loading.value = true
            try {
                _schedules.value = repository.schedules()
                _loaded.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            } finally {
                if (isActive) _loading.value = false
            }
        }
    }

    /** The first resume is the screen opening, which `init` already answered; coming back from a child screen asks again. */
    fun onScreenResumed() {
        if (!firstResumeSeen) {
            firstResumeSeen = true
            return
        }
        refresh()
    }
}
