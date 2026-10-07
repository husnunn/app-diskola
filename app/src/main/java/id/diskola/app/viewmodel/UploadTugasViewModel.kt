package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.AssignmentDay
import id.diskola.app.dataclass.ResponData.AssignmentSchedule
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.repository.LinkPreviewData
import id.diskola.app.repository.LinkPreviewRepository
import id.diskola.app.repository.MateriRepository
import id.diskola.app.repository.TugasRepository
import id.diskola.app.utils.session.SessionStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val END_AT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale("id"))

/**
 * `CreateHomeworkPage` (doc §6.4) — 3 modes (create/edit/detail) via `editable`. Fixes vs legacy
 * (confirmed, not silent): the Kelas→Hari→Mapel cascade's schedule fetch is wrapped so a network
 * failure shows an error instead of crashing; edit-mode's deadline always defaults to "now" rather
 * than feeding the server's display-label string back as if it were a parseable date.
 */
@HiltViewModel
class UploadTugasViewModel @Inject constructor(
    private val repository: TugasRepository,
    private val materiRepository: MateriRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    val classes: StateFlow<List<ClassRoomTable>> =
        materiRepository.observeClasses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _days = MutableStateFlow<List<AssignmentDay>>(emptyList())
    val days: StateFlow<List<AssignmentDay>> = _days.asStateFlow()

    private val _schedules = MutableStateFlow<List<AssignmentSchedule>>(emptyList())
    val schedules: StateFlow<List<AssignmentSchedule>> = _schedules.asStateFlow()

    private val _existing = MutableStateFlow<HomeworkTable?>(null)
    val existing: StateFlow<HomeworkTable?> = _existing.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

    private val _linkPreview = MutableStateFlow<LinkPreviewData?>(null)
    val linkPreview: StateFlow<LinkPreviewData?> = _linkPreview.asStateFlow()
    private val _linkPreviewLoading = MutableStateFlow(false)
    val linkPreviewLoading: StateFlow<Boolean> = _linkPreviewLoading.asStateFlow()
    private var linkDebounceJob: Job? = null

    private val _scheduleDayLoadFailed = MutableStateFlow(false)
    val scheduleDayLoadFailed: StateFlow<Boolean> = _scheduleDayLoadFailed.asStateFlow()

    fun loadReferenceData() {
        viewModelScope.launch { try { materiRepository.refreshFormClasses() } catch (e: Exception) { /* dropdown stays empty, user can retry by reopening */ } }
        viewModelScope.launch {
            try {
                _days.value = repository.fetchScheduleDays()
            } catch (e: Exception) {
                _scheduleDayLoadFailed.value = true
            }
        }
    }

    fun loadExisting(tugasId: Int) {
        viewModelScope.launch { _existing.value = repository.getCachedTugas(tugasId) }
    }

    /** Doc §6.4: picking a Kelas resets Hari/Mapel/schedule — the caller clears its own selection
     * state; this just means the previously-fetched `schedules` list is now stale. */
    fun onClassSelected() {
        _schedules.value = emptyList()
    }

    fun onDaySelected(classId: Int, day: String) {
        _schedules.value = emptyList()
        if (classId <= 0 || day.isBlank()) return
        launchWithHandling(customMessage = "Gagal memuat jadwal mata pelajaran") {
            _schedules.value = repository.fetchSchedule(classId, day)
        }
    }

    fun onLinkChanged(url: String) {
        linkDebounceJob?.cancel()
        _linkPreview.value = null
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

    fun submit(
        isEdit: Boolean,
        tugasId: Int,
        title: String,
        description: String,
        classId: Int,
        subjectId: Int,
        scheduleId: Int,
        grade: Int,
        endAt: LocalDateTime,
        requireRead: Boolean,
        requireUpload: Boolean,
        link: String,
        filePart: MultipartBody.Part?,
    ) {
        launchWithHandling(customMessage = "Gagal membuat tugas") {
            val plain = "text/plain".toMediaTypeOrNull()
            val data = mutableMapOf(
                "title" to title.toRequestBody(plain),
                "description" to description.toRequestBody(plain),
                "teacher_id" to (sessionStore.teacher?.id ?: 0).toString().toRequestBody(plain),
                "school_id" to sessionStore.school.id.toString().toRequestBody(plain),
                "school_subject_id" to subjectId.toString().toRequestBody(plain),
                "school_classes_id" to classId.toString().toRequestBody(plain),
                "school_subject_schedules_id" to scheduleId.toString().toRequestBody(plain),
                "grade" to grade.toString().toRequestBody(plain),
                "end_at" to endAt.format(END_AT_FORMATTER).toRequestBody(plain),
                "checked" to "1".toRequestBody(plain),
                "downloded" to (if (requireRead) "1" else "0").toRequestBody(plain),
                "uploaded" to (if (requireUpload) "1" else "0").toRequestBody(plain),
            )
            if (link.isNotBlank()) data["link"] = link.toRequestBody(plain)
            if (isEdit) repository.updateTugas(tugasId, data, filePart) else repository.createTugas(data, filePart)
            _uploadSuccess.value = true
        }
    }
}
