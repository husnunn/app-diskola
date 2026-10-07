package id.diskola.app.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.dataclass.ResponData.JurnalClassItem
import id.diskola.app.dataclass.ResponData.JurnalSubjectItem
import id.diskola.app.dataclass.ResponData.JurnalTeacherItem
import id.diskola.app.dataclass.ResponData.PlotItem
import id.diskola.app.repository.JURNAL_PICKER_PAGE
import id.diskola.app.repository.JournalExistsException
import id.diskola.app.repository.JurnalRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.location.LocationProvider
import id.diskola.app.utils.location.MockLocationGuard
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** Server-paged, searchable list behind one picker sheet (20 per page, 500 ms search debounce). */
class PagedPicker<T>(
    private val scope: CoroutineScope,
    private val idOf: (T) -> Int,
    private val fetch: suspend (skip: Int, name: String) -> List<T>,
    private val onError: (Throwable) -> Unit,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var hasNext = true
    private var loadingMore = false
    private var job: Job? = null
    private var generation = 0

    fun open() {
        _query.value = ""
        restart(delayMs = 0)
    }

    fun onQuery(value: String) {
        _query.value = value
        restart(delayMs = 500)
    }

    private fun restart(delayMs: Long) {
        job?.cancel()
        generation++
        val mine = generation
        job = scope.launch {
            if (delayMs > 0) delay(delayMs)
            _loading.value = true
            try {
                val page = fetch(0, _query.value)
                if (mine != generation) return@launch
                _items.value = page
                hasNext = page.size >= JURNAL_PICKER_PAGE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                onError(e)
            } finally {
                if (mine == generation) _loading.value = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasNext || _loading.value) return
        loadingMore = true
        val mine = generation
        scope.launch {
            try {
                val page = fetch(_items.value.size, _query.value)
                if (mine != generation) return@launch
                _items.value = (_items.value + page).distinctBy(idOf)
                hasNext = page.size >= JURNAL_PICKER_PAGE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                loadingMore = false
            }
        }
    }
}

sealed interface JurnalFormResult {
    data class Created(val message: String) : JurnalFormResult

    /** 307: the journal exists already; [attendanceIds].first() is the one to open. */
    data class Exists(val message: String, val attendanceIds: List<Int>) : JurnalFormResult
}

/**
 * Journal form for both roles (doc `07` §8). Teacher: consecutive hour chips + kelas + mapel + tujuan; student:
 * the hour is fixed (from the row they came from) + mapel + guru + status. The scope and the photo requirement are
 * read on every open. A missing required photo or field is reported on submit, not by disabling the button.
 */
@HiltViewModel
class JurnalFormViewModel @Inject constructor(
    private val repository: JurnalRepository,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    val isTeacher: Boolean get() = repository.isTeacher && !repository.isStudent

    private var started = false
    private var fixedPlotId = 0

    private val _assigned = MutableStateFlow(false)
    val assigned: StateFlow<Boolean> = _assigned.asStateFlow()

    private val _captureRequired = MutableStateFlow(false)
    val captureRequired: StateFlow<Boolean> = _captureRequired.asStateFlow()

    private val _plots = MutableStateFlow<List<PlotItem>>(emptyList())
    val plots: StateFlow<List<PlotItem>> = _plots.asStateFlow()

    private val _selectedPlots = MutableStateFlow<List<Int>>(emptyList())
    val selectedPlots: StateFlow<List<Int>> = _selectedPlots.asStateFlow()

    private val _selectedClass = MutableStateFlow<JurnalClassItem?>(null)
    val selectedClass: StateFlow<JurnalClassItem?> = _selectedClass.asStateFlow()

    private val _selectedSubject = MutableStateFlow<JurnalSubjectItem?>(null)
    val selectedSubject: StateFlow<JurnalSubjectItem?> = _selectedSubject.asStateFlow()

    private val _selectedTeacher = MutableStateFlow<JurnalTeacherItem?>(null)
    val selectedTeacher: StateFlow<JurnalTeacherItem?> = _selectedTeacher.asStateFlow()

    private val _objective = MutableStateFlow("")
    val objective: StateFlow<String> = _objective.asStateFlow()

    private val _studentStatus = MutableStateFlow<String?>(null)
    val studentStatus: StateFlow<String?> = _studentStatus.asStateFlow()

    private val _photo = MutableStateFlow<File?>(null)
    val photo: StateFlow<File?> = _photo.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _result = MutableStateFlow<JurnalFormResult?>(null)
    val result: StateFlow<JurnalFormResult?> = _result.asStateFlow()

    val classPicker = PagedPicker(viewModelScope, { it.id }, { skip, name -> repository.classes(skip, name) }, ::reportPickerError)
    val subjectPicker = PagedPicker(viewModelScope, { it.id }, { skip, name -> repository.subjects(skip, name) }, ::reportPickerError)
    val teacherPicker = PagedPicker(viewModelScope, { it.id }, { skip, name -> repository.teachers(skip, name) }, ::reportPickerError)

    private fun reportPickerError(e: Throwable) = emitError(AppErrorHandler.getMessage(e))

    fun start(plotId: Int) {
        if (started) return
        started = true
        fixedPlotId = plotId
        if (!isTeacher) _selectedPlots.value = listOf(plotId)
        viewModelScope.launch {
            _assigned.value = repository.teacherScopeAssigned()
            _captureRequired.value = repository.captureRequired()
            if (isTeacher) {
                try {
                    val list = repository.plots()
                    _plots.value = list
                    val index = list.indexOfFirst { it.time_plot_id == plotId }
                    if (index >= 0) _selectedPlots.value = listOf(index)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // legacy crashed here; the form stays usable and says why the chips are missing
                    Timber.e(e)
                    emitError(AppErrorHandler.getMessage(e))
                }
            }
        }
    }

    // ---- Inputs ----

    fun onObjectiveChange(value: String) { _objective.value = value }
    fun onClassSelected(item: JurnalClassItem) { _selectedClass.value = item }
    fun onSubjectSelected(item: JurnalSubjectItem) { _selectedSubject.value = item }
    fun onTeacherSelected(item: JurnalTeacherItem) { _selectedTeacher.value = item }
    fun onStatusSelected(value: String) { _studentStatus.value = value }
    fun onPhotoCaptured(path: String) { _photo.value = File(path) }
    fun removePhoto() { _photo.value = null }

    /** Hour chips must be consecutive; a refused tap changes nothing and says so. */
    fun onChipTapped(index: Int) {
        val next = JurnalRules.toggleChip(_selectedPlots.value, index)
        if (next == null) emitError(JurnalRules.NOT_SEQUENTIAL) else _selectedPlots.value = next
    }

    fun dismissResult() { _result.value = null }

    /** Where the photo was taken, for the watermark. Every part is optional and a slow fix never blocks the camera. */
    data class CaptureSpot(val address: String?, val lat: Double?, val lng: Double?)

    fun prepareCapture(onReady: (CaptureSpot) -> Unit) {
        viewModelScope.launch {
            _preparing.value = true
            val spot = try {
                val fix = bestEffortLocation()
                val address = fix?.let { withTimeoutOrNull(4_000L) { locationProvider.addressOf(it.latitude, it.longitude) } }
                CaptureSpot(address, fix?.latitude, fix?.longitude)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                CaptureSpot(null, null, null)
            } finally {
                _preparing.value = false
            }
            onReady(spot)
        }
    }

    private val _preparing = MutableStateFlow(false)
    val preparing: StateFlow<Boolean> = _preparing.asStateFlow()

    // ---- Submit ----

    fun submit() {
        clearError()
        if (isTeacher) submitTeacher() else submitStudent()
    }

    private fun submitTeacher() {
        val plotIds = _selectedPlots.value.mapNotNull { _plots.value.getOrNull(it)?.time_plot_id }
        val error = JurnalRules.validateTeacherForm(
            objective = _objective.value,
            classId = _selectedClass.value?.id,
            subjectId = _selectedSubject.value?.id,
            plotIds = plotIds,
            captureRequired = _captureRequired.value,
            hasPhoto = _photo.value != null,
        )
        if (error != null) {
            emitError(error)
            return
        }
        send {
            repository.createJournal(plotIds, _selectedSubject.value!!.id, _selectedClass.value!!.id, _objective.value, _photo.value)
        }
    }

    private fun submitStudent() {
        val teacherUuid = _selectedTeacher.value?.user?.id
        val error = JurnalRules.validateStudentForm(
            teacherUuid = teacherUuid,
            subjectId = _selectedSubject.value?.id,
            status = _studentStatus.value,
            captureRequired = _captureRequired.value,
            hasPhoto = _photo.value != null,
        )
        if (error != null) {
            emitError(error)
            return
        }
        send {
            // Coordinates are best effort; a mock location is not sent at all.
            val fix = bestEffortLocation()?.takeUnless(MockLocationGuard::isMock)
            repository.createStudentJournal(
                plotIds = listOf(fixedPlotId),
                subjectId = _selectedSubject.value!!.id,
                teacherUuid = teacherUuid!!,
                status = _studentStatus.value!!,
                lat = fix?.latitude,
                lng = fix?.longitude,
                photo = _photo.value,
            )
        }
    }

    private fun send(call: suspend () -> Any) {
        viewModelScope.launch {
            _submitting.value = true
            try {
                call()
                _result.value = JurnalFormResult.Created("Jurnal kelas berhasil diproses")
            } catch (e: CancellationException) {
                throw e
            } catch (e: JournalExistsException) {
                _result.value = JurnalFormResult.Exists(e.message.orEmpty(), e.attendanceIds)
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _submitting.value = false
            }
        }
    }

    private suspend fun bestEffortLocation(): Location? {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return null
        return withTimeoutOrNull(4_000L) { locationProvider.freshLocation() ?: locationProvider.currentLocation() }
    }
}
