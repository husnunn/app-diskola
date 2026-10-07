package id.diskola.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.dataclass.ResponData.PoinFormMode
import id.diskola.app.dataclass.ResponData.PoinItemTable
import id.diskola.app.repository.PoinRepository
import id.diskola.app.utils.PhotoCapture
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * One ViewModel for the three teacher forms (pelanggaran / prestasi / pemanggilan). Unlike legacy,
 * success is reported only after the API call really succeeded (a failure surfaces as
 * [errorMessage] and the form stays open), for all three forms.
 */
@HiltViewModel
class PoinFormViewModel @Inject constructor(
    private val repository: PoinRepository,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private val mode = MutableStateFlow<PoinFormMode?>(null)

    fun setMode(value: PoinFormMode) {
        if (mode.value == null) mode.value = value
    }

    // ---- Form fields ----

    private val _selectedType = MutableStateFlow<PoinItemTable?>(null)
    val selectedType: StateFlow<PoinItemTable?> = _selectedType.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _photo = MutableStateFlow<File?>(null)
    val photo: StateFlow<File?> = _photo.asStateFlow()

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _time = MutableStateFlow<LocalTime?>(null)
    val time: StateFlow<LocalTime?> = _time.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun onMessageChange(value: String) { _message.value = value }
    fun onTypeSelected(item: PoinItemTable) { _selectedType.value = item }
    fun onDateSelected(value: LocalDate) { _date.value = value }
    fun onTimeSelected(value: LocalTime) { _time.value = value }
    fun removePhoto() { _photo.value = null }
    fun reportError(message: String) = emitError(message)

    /** Compresses the picked/captured image to ≤1 MB; one that cannot fit is rejected loudly. */
    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _loading.value = true
            val file = PhotoCapture.compress(context, uri)
            _loading.value = false
            if (file == null) emitError("Foto tidak dapat diproses atau ukurannya melebihi 1 MB.") else _photo.value = file
        }
    }

    // ---- "Jenis" picker (searchable, paged, Room-backed) ----

    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    private val _pickerLoading = MutableStateFlow(false)
    val pickerLoading: StateFlow<Boolean> = _pickerLoading.asStateFlow()

    private var pickerHasNext = true
    private var pickerLoadingMore = false
    private var pickerJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val pickerItems: StateFlow<List<PoinItemTable>> = combine(mode.filterNotNull(), _pickerQuery) { m, q -> m to q }
        .flatMapLatest { (m, q) -> repository.observeItems(m.poinType, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openPicker() {
        _pickerQuery.value = ""
        fetchPicker(refresh = false)
    }

    fun onPickerQuery(value: String) {
        _pickerQuery.value = value
        pickerJob?.cancel()
        pickerJob = viewModelScope.launch {
            delay(500)
            fetchPicker(refresh = false)
        }
    }

    fun loadMorePicker() {
        val type = mode.value?.poinType ?: return
        if (pickerLoadingMore || !pickerHasNext) return
        pickerLoadingMore = true
        viewModelScope.launch {
            try {
                pickerHasNext = repository.loadMore(type, _pickerQuery.value, pickerItems.value.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                pickerLoadingMore = false
            }
        }
    }

    private fun fetchPicker(refresh: Boolean) {
        val type = mode.value?.poinType ?: return
        val query = _pickerQuery.value
        viewModelScope.launch {
            _pickerLoading.value = true
            try {
                if (refresh) {
                    pickerHasNext = repository.refresh(type, query)
                } else {
                    repository.ensureFirstPage(type, query)
                    pickerHasNext = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(id.diskola.app.utils.AppErrorHandler.getMessage(e))
            } finally {
                _pickerLoading.value = false
            }
        }
    }

    // ---- Submit ----

    /** [target] is what the teacher picked in search: `userId` for pelanggaran/prestasi,
     * `student.id` (the score-student id) for pemanggilan. */
    fun submit(target: PoinSelected) {
        val m = mode.value ?: return
        val type = _selectedType.value
        val message = _message.value.trim()

        if (m == PoinFormMode.HANDLING) {
            val date = _date.value
            val time = _time.value
            if (type == null || message.isBlank() || date == null || time == null) {
                emitError("Mohon lengkapi data terlebih dahulu")
                return
            }
            launchWithHandling {
                repository.submitCalling(
                    scoreStudentId = target.student.id,
                    handlingId = type.id,
                    callingAt = "$date ${time.format(TIME_FORMATTER)}",
                    message = message,
                )
                _done.value = true
            }
            return
        }

        if (type == null || message.isBlank()) {
            emitError("Mohon isi keterangan terlebih dahulu")
            return
        }
        launchWithHandling {
            if (m == PoinFormMode.VIOLATION) {
                repository.submitViolation(target.userId, type.id, message, _photo.value)
            } else {
                repository.submitAchievement(target.userId, type.id, message, _photo.value)
            }
            _done.value = true
        }
    }
}
