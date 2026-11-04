package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.MagangAttendanceItem
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.repository.MagangRepository
import id.diskola.app.utils.AppErrorHandler
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

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

    /** Reports and izin as `GET attendance` sent them (told apart by `record_type`); the Laporan tab and the izin screens read this. */
    private val _attendance = MutableStateFlow<List<MagangAttendanceItem>>(emptyList())
    val attendance: StateFlow<List<MagangAttendanceItem>> = _attendance.asStateFlow()

    private val _attendanceLoaded = MutableStateFlow(false)
    val attendanceLoaded: StateFlow<Boolean> = _attendanceLoaded.asStateFlow()

    /** Why the reports could not be fetched; kept apart from [errorMessage] so a failing list does not hide the other tab. */
    private val _attendanceError = MutableStateFlow("")
    val attendanceError: StateFlow<String> = _attendanceError.asStateFlow()

    /** The Laporan tab's "Pilih Tanggal" range (both ends inclusive); null is "Semua". */
    private val _reportRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    val reportRange: StateFlow<Pair<LocalDate, LocalDate>?> = _reportRange.asStateFlow()

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

    fun setReportRange(from: LocalDate, to: LocalDate) {
        _reportRange.value = if (to.isBefore(from)) to to from else from to to
    }

    fun clearReportRange() {
        _reportRange.value = null
    }

    fun clearAttendanceError() {
        _attendanceError.value = ""
    }

    /**
     * Both lists are fetched together and each keeps its own outcome: a failing schedule leaves the reports on screen and the other way round.
     * A newer refresh replaces one still in flight: after a check-in or a report the answer to ask for is the one sent after the write, and
     * whichever request finished last must not decide what the list shows or when the spinner stops.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _loading.value = true
            try {
                coroutineScope {
                    val schedules = async { attempt { repository.schedules() } }
                    val attendance = async { attempt { repository.attendance() } }
                    schedules.await()
                        .onSuccess {
                            _schedules.value = it
                            _loaded.value = true
                        }
                        .onFailure(::handleError)
                    attendance.await()
                        .onSuccess {
                            _attendance.value = it
                            _attendanceLoaded.value = true
                            _attendanceError.value = ""
                        }
                        .onFailure {
                            Timber.e(it)
                            _attendanceError.value = AppErrorHandler.getMessage(it)
                        }
                }
            } finally {
                if (isActive) _loading.value = false
            }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
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
