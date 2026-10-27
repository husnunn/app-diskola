package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.AssignmentDay
import id.diskola.app.dataclass.ResponData.AssignmentSchedule
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.dataclass.ResponData.MapelItem
import id.diskola.app.repository.LinkPreviewData
import id.diskola.app.repository.LinkPreviewRepository
import id.diskola.app.repository.TugasRepository
import id.diskola.app.utils.AssignmentAccess
import id.diskola.app.utils.AssignmentDates
import id.diskola.app.utils.AssignmentError
import id.diskola.app.utils.AssignmentErrors
import id.diskola.app.utils.AssignmentInput
import id.diskola.app.utils.AssignmentPayload
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import timber.log.Timber

/** The dependent choices of the form: they reset each other, so the ViewModel owns them. 0 / "" = nothing chosen. */
data class TugasSelection(val classId: Int = 0, val day: String = "", val scheduleId: Int = 0, val subjectId: Int = 0)

/** What the edit form shows first: the cached task and its deadline read back from `end_at_label` (null when it could not be read). */
data class TugasPrefill(val table: HomeworkTable, val dueAtMillis: Long?)

sealed interface TugasFormDialog {
    /** 403: the student/teacher account is blocked; the trace id is what the school needs to look it up. */
    data class Blocked(val message: String, val traceId: String?) : TugasFormDialog

    /** "Periksa kembali isian": the messages of a 422 that have no field of their own (and the long text fields). */
    data class Invalid(val text: String) : TugasFormDialog

    /** "Gagal menyimpan tugas": any other failure. */
    data class Failed(val text: String) : TugasFormDialog
}

/** The form is done: [toast] (if any) is shown, the screen closes and the list refreshes. */
data class TugasFormClose(val toast: String? = null)

/**
 * "Buat/Edit Tugas" of the teacher (doc `05` §6.4, backend contract of 2026-10-22). Two forms, chosen by `teacher-data-scope`:
 *  - **assigned** (`restricted` is not `false`, also when the scope cannot be read): Kelas → Hari → Mata Pelajaran from the schedule;
 *  - **free** (`restricted == false`): Kelas + Mata Pelajaran (paged), no Hari.
 * The payload, the validation, the dates and the error texts are the pure rules in `AssignmentRules` / `AssignmentErrors`.
 */
@HiltViewModel
class UploadTugasViewModel @Inject constructor(
    private val repository: TugasRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    /** `null` until the scope is known; the form waits for it ([initialLoading]). */
    private val _restricted = MutableStateFlow<Boolean?>(null)
    val restricted: StateFlow<Boolean?> = _restricted.asStateFlow()

    private val _classes = MutableStateFlow<List<ClassRoomTable>>(emptyList())
    val classes: StateFlow<List<ClassRoomTable>> = _classes.asStateFlow()

    private val _days = MutableStateFlow<List<AssignmentDay>>(emptyList())
    val days: StateFlow<List<AssignmentDay>> = _days.asStateFlow()

    private val _schedules = MutableStateFlow<List<AssignmentSchedule>>(emptyList())
    val schedules: StateFlow<List<AssignmentSchedule>> = _schedules.asStateFlow()

    private val _subjects = MutableStateFlow<List<MapelItem>>(emptyList())
    val subjects: StateFlow<List<MapelItem>> = _subjects.asStateFlow()

    private val _selection = MutableStateFlow(TugasSelection())
    val selection: StateFlow<TugasSelection> = _selection.asStateFlow()

    /** The blocking "mohon tunggu" while the scope, the classes and the days/subjects load (and the edit form is prepared). */
    private val _initialLoading = MutableStateFlow(true)
    val initialLoading: StateFlow<Boolean> = _initialLoading.asStateFlow()

    /** The schedule list of the chosen Kelas + Hari is on its way; Posting waits for it, but it is not the "Posting" spinner. */
    private val _scheduleLoading = MutableStateFlow(false)
    val scheduleLoading: StateFlow<Boolean> = _scheduleLoading.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _prefill = MutableStateFlow<TugasPrefill?>(null)
    val prefill: StateFlow<TugasPrefill?> = _prefill.asStateFlow()

    /** Form key (`school_class_id`, `day`, `school_subject_id`, `end_at`, `title`, `description`, `link`) → message from a 422. */
    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    private val _dialog = MutableStateFlow<TugasFormDialog?>(null)
    val dialog: StateFlow<TugasFormDialog?> = _dialog.asStateFlow()

    private val _finished = MutableStateFlow<TugasFormClose?>(null)
    val finished: StateFlow<TugasFormClose?> = _finished.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    private val _linkPreview = MutableStateFlow<LinkPreviewData?>(null)
    val linkPreview: StateFlow<LinkPreviewData?> = _linkPreview.asStateFlow()
    private val _linkPreviewLoading = MutableStateFlow(false)
    val linkPreviewLoading: StateFlow<Boolean> = _linkPreviewLoading.asStateFlow()
    private var linkDebounceJob: Job? = null

    private var started = false
    private var scheduleJob: Job? = null
    private var scheduleToken = 0

    /** Whether the Hari → Mapel (schedule) form applies; unknown scope counts as restricted. */
    private val assignedMode: Boolean get() = _restricted.value != false

    /** Loads scope → classes → days (assigned) / subjects (free); for an edit then prepares the form from the cached task. Runs once. */
    fun start(editTugasId: Int) {
        if (started) return
        started = true
        // No teacher data: the endpoints would answer 400 after the whole form is filled in, so the form closes at once and says why.
        if (!sessionStore.canManageAssignments) {
            _finished.value = TugasFormClose(AssignmentAccess.NO_TEACHER_DATA_TOAST)
            return
        }
        viewModelScope.launch {
            _initialLoading.value = true
            try {
                loadScope()
                loadClasses()
                if (assignedMode) loadDays() else loadSubjects()
                if (editTugasId > 0) applyExisting(editTugasId)
            } finally {
                _initialLoading.value = false
            }
        }
    }

    // ---- Loading the reference data ----

    private suspend fun loadScope() {
        _restricted.value = try {
            repository.fetchTeacherScope()?.isRestrictedForForm ?: true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "teacher-data-scope gagal")
            emitError("Gagal memuat pengaturan akses guru, form memakai mode jadwal")
            true // fail closed: the narrower form
        }
    }

    private suspend fun loadClasses() {
        try {
            _classes.value = repository.fetchFormClasses()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportLoadError(e)
        }
    }

    private suspend fun loadDays() {
        try {
            _days.value = repository.fetchScheduleDays()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportLoadError(e)
        }
    }

    private suspend fun loadSubjects() {
        try {
            _subjects.value = repository.fetchFormSubjects()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportLoadError(e)
        }
    }

    /** A failed load is a banner, not a crash; a 401 is the session handler's business (it signs the user out). */
    private fun reportLoadError(e: Throwable) {
        Timber.w(e, "Gagal memuat data form tugas")
        when (val error = AssignmentErrors.classify(e, isUpdate = false)) {
            is AssignmentError.Message -> emitError(error.text)
            is AssignmentError.Blocked -> emitError(error.message)
            AssignmentError.Unauthorized -> Unit
            else -> emitError(AssignmentErrors.DEFAULT_MESSAGE)
        }
    }

    // ---- Edit: prepare the form from the cached task ----

    private suspend fun applyExisting(tugasId: Int) {
        val item = repository.getCachedTugas(tugasId)
        if (item == null) {
            // Legacy kept the form open with no task id, so "Posting" created a **new** task; here the form closes.
            _finished.value = TugasFormClose("Tugas tidak ditemukan, silakan muat ulang daftar tugas")
            return
        }
        val dueAt = AssignmentDates.parseEndAtLabel(item.end_at_label)
        if (dueAt == null) _toasts.tryEmit("Waktu berakhir tidak terbaca, silakan pilih ulang")

        _selection.value = TugasSelection(classId = item.class_id.takeIf { it > 0 } ?: 0)
        if (!assignedMode) {
            _selection.update { it.copy(subjectId = item.subject_id.takeIf { id -> id > 0 } ?: 0) }
        } else if (item.schedule_id > 0 && item.schedule_day.isNotBlank()) {
            // The day comes back in the case the schedule had it; the day list's key (`SENIN`) is what the query wants.
            val day = _days.value.firstOrNull { it.key.equals(item.schedule_day, ignoreCase = true) }?.key ?: item.schedule_day
            _selection.update { it.copy(day = day, scheduleId = item.schedule_id) }
            fetchSchedule(keepScheduleId = item.schedule_id)?.join()
        }
        _prefill.value = TugasPrefill(item, dueAt)
    }

    // ---- The dependent choices ----

    fun selectClass(id: Int) {
        clearFieldError("school_class_id")
        if (assignedMode) {
            _selection.update { it.copy(classId = id, day = "") }
            clearSchedule()
        } else {
            _selection.update { it.copy(classId = id) }
        }
    }

    fun selectDay(key: String) {
        if (!assignedMode) return
        clearFieldError("day")
        _selection.update { it.copy(day = key) }
        clearSchedule()
        fetchSchedule()
    }

    fun selectSchedule(scheduleId: Int) {
        clearFieldError("school_subject_id")
        val subjectId = _schedules.value.firstOrNull { it.id == scheduleId }?.subject?.id ?: 0
        _selection.update { it.copy(scheduleId = scheduleId, subjectId = subjectId) }
    }

    fun selectSubject(subjectId: Int) {
        clearFieldError("school_subject_id")
        _selection.update { it.copy(subjectId = subjectId) }
    }

    /** A new Kelas or Hari makes the old schedule list and the chosen Mapel meaningless; an answer still on its way is ignored. */
    private fun clearSchedule() {
        cancelSchedule()
        _selection.update { it.copy(scheduleId = 0, subjectId = 0) }
        _schedules.value = emptyList()
    }

    private fun cancelSchedule() {
        scheduleToken++
        scheduleJob?.cancel()
        scheduleJob = null
        _scheduleLoading.value = false
    }

    /** Loads the schedule of the chosen Kelas + Hari. [keepScheduleId] > 0 (edit): that schedule stays chosen only if the list has it. */
    private fun fetchSchedule(keepScheduleId: Int = 0): Job? {
        val chosen = _selection.value
        if (!assignedMode || chosen.classId <= 0 || chosen.day.isBlank()) return null
        val token = ++scheduleToken
        scheduleJob?.cancel()
        val job = viewModelScope.launch {
            _scheduleLoading.value = true
            try {
                val list = repository.fetchSchedule(chosen.classId, chosen.day)
                if (token != scheduleToken) return@launch // a newer Kelas/Hari was chosen meanwhile
                _schedules.value = list
                if (keepScheduleId > 0) {
                    val kept = list.firstOrNull { it.id == keepScheduleId }
                    _selection.update { it.copy(scheduleId = kept?.id ?: 0, subjectId = kept?.subject?.id ?: 0) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token == scheduleToken) reportLoadError(e)
            } finally {
                if (token == scheduleToken) _scheduleLoading.value = false
            }
        }
        scheduleJob = job
        return job
    }

    // ---- Link preview (informs; "Posting" never waits for it) ----

    fun onLinkChanged(url: String) {
        linkDebounceJob?.cancel()
        _linkPreview.value = null
        clearFieldError("link")
        if (url.isBlank()) return
        linkDebounceJob = viewModelScope.launch {
            delay(1000)
            _linkPreviewLoading.value = true
            val result = linkPreviewRepository.fetch(url)
            _linkPreviewLoading.value = false
            if (result == null) {
                emitError("gagal menampilkan preview url")
            } else {
                _linkPreview.value = result
            }
        }
    }

    // ---- Posting ----

    /**
     * Sends the task. [input] is validated again here (the button is already disabled when it is not valid). [buildFile] makes the file part
     * on the IO dispatcher, so a big PDF is not copied on the main thread.
     */
    fun submit(isEdit: Boolean, tugasId: Int, input: AssignmentInput, buildFile: (() -> MultipartBody.Part?)?) {
        if (_submitting.value) return
        val errors = AssignmentPayload.validate(input)
        if (errors.isNotEmpty()) {
            showFieldErrors(errors.mapValues { listOf(it.value) })
            return
        }
        _fieldErrors.value = emptyMap()
        _submitting.value = true
        viewModelScope.launch {
            try {
                val file = buildFile?.let { build -> withContext(Dispatchers.IO) { build() } }
                val fields = AssignmentPayload.fields(input)
                if (isEdit) repository.updateTugas(tugasId, fields, file) else repository.createTugas(fields, file)
                _finished.value = TugasFormClose()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Gagal menyimpan tugas")
                handleSubmitFailure(e, isEdit, tugasId)
            } finally {
                _submitting.value = false
            }
        }
    }

    private suspend fun handleSubmitFailure(e: Exception, isEdit: Boolean, tugasId: Int) {
        when (val error = AssignmentErrors.classify(e, isUpdate = isEdit)) {
            // 422: the draft stays exactly as it is; each message is put where it belongs.
            is AssignmentError.Validation -> showFieldErrors(error.fieldErrors)
            AssignmentError.NotFound -> {
                if (tugasId > 0) repository.forgetLocalTugas(tugasId)
                _finished.value = TugasFormClose("Tugas tidak ditemukan, mungkin sudah dihapus")
            }
            // The interceptor already raised the session event that signs the user out.
            AssignmentError.Unauthorized -> Unit
            is AssignmentError.Blocked -> _dialog.value = TugasFormDialog.Blocked(error.message, error.traceId)
            is AssignmentError.Message -> _dialog.value = TugasFormDialog.Failed(error.text)
        }
    }

    private fun showFieldErrors(fieldErrors: Map<String, List<String>>) {
        val placement = AssignmentErrors.placeFieldErrors(fieldErrors)
        _fieldErrors.value = placement.inline
        if (placement.dialogLines.isNotEmpty()) _dialog.value = TugasFormDialog.Invalid(placement.dialogLines.joinToString("\n"))
    }

    /** Editing a field takes its server message away. */
    fun clearFieldError(key: String) {
        if (key in _fieldErrors.value) _fieldErrors.update { it - key }
    }

    fun dismissDialog() {
        _dialog.value = null
    }
}
