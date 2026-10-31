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

/** How long a new location fix (and then the reverse geocode) is waited for before the form goes on without it. */
private const val FIX_WAIT_MILLIS = 4_000L

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

    /** Why the list could not be loaded, for the sheet itself — a banner on the form sits behind the open sheet. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
            _error.value = null
            try {
                val page = fetch(0, _query.value)
                if (mine != generation) return@launch
                _items.value = page
                hasNext = page.size >= JURNAL_PICKER_PAGE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                // Rows of the previous query would pass for the answer to this one.
                if (mine == generation) {
                    _items.value = emptyList()
                    _error.value = AppErrorHandler.getMessage(e)
                }
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

    // Seeded from the last open (like legacy) so the hint and the "*" are right from the first frame; start() replaces them with the server's.
    private val _restricted = MutableStateFlow(repository.cachedScopeRestricted())
    val restricted: StateFlow<Boolean> = _restricted.asStateFlow()

    private val _captureRequired = MutableStateFlow(repository.cachedCaptureRequired())
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
            _restricted.value = repository.teacherScopeRestricted()
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
    fun onPhotoCaptured(path: String) {
        // A retake replaces the file: the stamped copy it replaces would otherwise stay in the cache for good.
        _photo.value?.takeIf { it.path != path }?.delete()
        _photo.value = File(path)
    }

    fun removePhoto() {
        _photo.value?.delete()
        _photo.value = null
    }

    /** Hour chips must be consecutive; a refused tap changes nothing and says so. */
    fun onChipTapped(index: Int) {
        val next = JurnalRules.toggleChip(_selectedPlots.value, index)
        if (next == null) emitError(JurnalRules.NOT_SEQUENTIAL) else _selectedPlots.value = next
    }

    fun dismissResult() { _result.value = null }

    /** Where the photo was taken, for the watermark. Every part is optional and a slow fix never blocks the camera. */
    data class CaptureSpot(val address: String?, val lat: Double?, val lng: Double?)

    /** The last place that got an address, so a retake in the same room is stamped like the first photo even when the geocoder is slow. */
    private data class RememberedSpot(val lat: Double, val lng: Double, val address: String)

    private var rememberedSpot: RememberedSpot? = null

    fun prepareCapture(onReady: (CaptureSpot) -> Unit) {
        viewModelScope.launch {
            _preparing.value = true
            val spot = try {
                val fix = bestEffortLocation()
                val address = fix?.let { addressFor(it) }
                if (fix != null && address != null) rememberedSpot = RememberedSpot(fix.latitude, fix.longitude, address)
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
            val fix = bestEffortLocation()
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
                // Sent: the stamped copy has done its job.
                _photo.value?.delete()
                _result.value = JurnalFormResult.Created("Jurnal kelas berhasil diproses")
            } catch (e: CancellationException) {
                throw e
            } catch (e: JournalExistsException) {
                // Legacy: without an id to open there is nothing to open — the server's message, and the form stays.
                if (e.attendanceIds.isEmpty()) emitError(e.message.orEmpty()) else _result.value = JurnalFormResult.Exists(e.message.orEmpty(), e.attendanceIds)
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _submitting.value = false
            }
        }
    }

    private fun locationGranted(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * What a submit sends and what the photo is stamped with: a new fix if one comes within 4 s, else the position the device already knows
     * when it is recent ([JurnalRules.isUsableLastKnown]) — legacy asked `lastLocation` first. Indoors a new high-accuracy fix can take longer
     * than 4 s, and without the fallback the watermark kept only its time line and the submit carried no coordinates. A mock position is
     * never used (the photo is evidence, like the submit). null when location is not permitted or unknown.
     */
    private suspend fun bestEffortLocation(): Location? {
        if (!locationGranted()) return null
        return locationProvider.freshOrRecentLocation(FIX_WAIT_MILLIS, JurnalRules::isUsableLastKnown)?.takeUnless(MockLocationGuard::isMock)
    }

    /** The address of [fix]; when the geocoder gives none in time, the one remembered for this same spot. */
    private suspend fun addressFor(fix: Location): String? {
        withTimeoutOrNull(FIX_WAIT_MILLIS) { locationProvider.addressOf(fix.latitude, fix.longitude) }?.let { return it }
        val remembered = rememberedSpot ?: return null
        return remembered.address.takeIf { JurnalRules.sameSpot(remembered.lat, remembered.lng, fix.latitude, fix.longitude) }
    }
}
