package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.GradeTable
import id.diskola.app.dataclass.ResponData.MajorItem
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.repository.LinkPreviewData
import id.diskola.app.repository.LinkPreviewRepository
import id.diskola.app.repository.MateriRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber

/**
 * `UploadMateriPage` create/edit form (doc `05-pembelajaran-materi-tugas.md` §3.4). Fixes vs the
 * legacy app (all confirmed, not silent): a target (Jenjang/Jurusan/Kelas) is required before
 * submitting instead of silently sending `grade="null"`; the grade/kelas dropdowns come from
 * their real endpoints instead of a hardcoded list / the wrong endpoint; the Url Link field gets a
 * real 1000ms-debounced preview.
 */
@HiltViewModel
class UploadMateriViewModel @Inject constructor(
    private val repository: MateriRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
) : BaseViewModel() {

    val subjects: StateFlow<List<MapelTable>> =
        repository.observeSubjects("").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes: StateFlow<List<ClassRoomTable>> =
        repository.observeClasses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val majors: StateFlow<List<MajorItem>> =
        repository.observeMajors().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val grades: StateFlow<List<GradeTable>> =
        repository.observeGrades().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _existing = MutableStateFlow<MateriTable?>(null)
    val existing: StateFlow<MateriTable?> = _existing.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

    private val _linkPreview = MutableStateFlow<LinkPreviewData?>(null)
    val linkPreview: StateFlow<LinkPreviewData?> = _linkPreview.asStateFlow()

    private val _linkPreviewLoading = MutableStateFlow(false)
    val linkPreviewLoading: StateFlow<Boolean> = _linkPreviewLoading.asStateFlow()

    private var linkDebounceJob: Job? = null

    /** Doc §3.4: `assignmentClass`-style filter list isn't what this form uses — it's the
     * dedicated `mobile/teacher/school-class-room`/`school-grade` endpoints. */
    fun loadReferenceData() {
        viewModelScope.launch { try { repository.ensureTeacherSubjectsFirstPage() } catch (e: Exception) { Timber.e(e) } }
        viewModelScope.launch { try { repository.refreshFormClasses() } catch (e: Exception) { Timber.e(e) } }
        viewModelScope.launch { try { repository.refreshMajors() } catch (e: Exception) { Timber.e(e) } }
        viewModelScope.launch { try { repository.refreshGrades() } catch (e: Exception) { Timber.e(e) } }
    }

    fun loadExisting(materiId: Int) {
        viewModelScope.launch {
            _existing.value = repository.getCachedMateri(materiId)
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

    fun submit(isEdit: Boolean, materiId: Int, data: Map<String, RequestBody>, filePart: MultipartBody.Part?) {
        launchWithHandling(customMessage = "Gagal membuat materi") {
            if (isEdit) repository.updateMateri(materiId, data, filePart) else repository.createMateri(data, filePart)
            _uploadSuccess.value = true
        }
    }
}
