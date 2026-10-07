package id.diskola.app.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.HomeworkAnswerFileTable
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.repository.FileOpenRepository
import id.diskola.app.repository.LinkPreviewData
import id.diskola.app.repository.LinkPreviewRepository
import id.diskola.app.repository.TugasRepository
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class SelectedAnswerFile(val uri: Uri, val name: String, val sizeBytes: Long)

private val ALLOWED_ANSWER_EXTENSIONS =
    setOf("jpeg", "jpg", "png", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
private const val MAX_ANSWER_FILE_BYTES = 12 * 1024 * 1024L

/**
 * `HomeworkDetailPage` (doc `05-pembelajaran-materi-tugas.md` §5.5, teacher read-only mode per
 * §6.4 "Lihat Tugas"). Decisions confirmed with the user (not silent replication of legacy bugs):
 * - a task not yet cached locally (e.g. a future notification deep link) triggers an `ensure*`
 *   fetch of the relevant list before giving up, instead of showing a blank screen (§[load]).
 * - "baca"/"upload" requirements are reactive to the *current* file/link state, not a one-way
 *   latch — removing every answer file un-satisfies "upload" again (§[clearSelectedFiles]).
 * - "baca soal" is only satisfied by an actually-completed download, never by cancelling the
 *   permission dialog — this falls out naturally since [openFile] only runs after "Setuju".
 */
@HiltViewModel
class TugasDetailViewModel @Inject constructor(
    private val repository: TugasRepository,
    private val fileOpenRepository: FileOpenRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val _tugas = MutableStateFlow<HomeworkTable?>(null)
    val tugas: StateFlow<HomeworkTable?> = _tugas.asStateFlow()

    /** Doc decision: id not found even after trying to refresh the relevant list → explicit
     * message, not a silently blank screen. */
    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    private val _currentId = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val answerFiles: StateFlow<List<HomeworkAnswerFileTable>> = _currentId
        .flatMapLatest { id -> if (id > 0) repository.observeAnswerFiles(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _readDone = MutableStateFlow(false)
    val readDone: StateFlow<Boolean> = _readDone.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<SelectedAnswerFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedAnswerFile>> = _selectedFiles.asStateFlow()

    private val _studentLink = MutableStateFlow("")
    val studentLink: StateFlow<String> = _studentLink.asStateFlow()

    private val _linkPreview = MutableStateFlow<LinkPreviewData?>(null)
    val linkPreview: StateFlow<LinkPreviewData?> = _linkPreview.asStateFlow()
    private val _linkPreviewLoading = MutableStateFlow(false)
    val linkPreviewLoading: StateFlow<Boolean> = _linkPreviewLoading.asStateFlow()
    private var linkPreviewSucceeded = false
    private var linkDebounceJob: Job? = null

    private val _teacherLinkPreview = MutableStateFlow<LinkPreviewData?>(null)
    val teacherLinkPreview: StateFlow<LinkPreviewData?> = _teacherLinkPreview.asStateFlow()
    private val _teacherLinkPreviewLoading = MutableStateFlow(false)
    val teacherLinkPreviewLoading: StateFlow<Boolean> = _teacherLinkPreviewLoading.asStateFlow()

    private val _fileLoading = MutableStateFlow(false)
    val fileLoading: StateFlow<Boolean> = _fileLoading.asStateFlow()
    private val _pdfPath = MutableStateFlow<String?>(null)
    val pdfPath: StateFlow<String?> = _pdfPath.asStateFlow()
    private val _openFileIntent = MutableStateFlow<Intent?>(null)
    val openFileIntent: StateFlow<Intent?> = _openFileIntent.asStateFlow()
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    /** `uploadDone` is reactive: files picked OR link preview already succeeded — recomputed every
     * time either changes, not cached as a permanent flag. */
    val uploadDone: StateFlow<Boolean> = combine(_selectedFiles, _linkPreview) { files, preview ->
        files.isNotEmpty() || (preview != null && linkPreviewSucceeded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var loadedFor = 0

    fun load(tugasId: Int, type: Int, isTeacher: Boolean) {
        if (loadedFor == tugasId && _tugas.value != null) return
        loadedFor = tugasId
        _readDone.value = false
        if (tugasId <= 0) {
            _notFound.value = true
            return
        }
        _currentId.value = tugasId
        launchWithHandling(showError = false) {
            var cached = repository.getCachedTugas(tugasId)
            if (cached == null) {
                try {
                    if (isTeacher) {
                        repository.ensureTeacherOwnFirstPage(sessionStore.teacher?.id ?: 0, null, null)
                    } else {
                        when (type) {
                            HomeworkTable.TYPE_DONE -> repository.ensureDoneFirstPage()
                            HomeworkTable.TYPE_SCORED -> repository.ensureScoredFirstPage()
                            else -> repository.ensureBacklogFirstPage()
                        }
                    }
                } catch (e: Exception) {
                    // handled below by the still-null check
                }
                cached = repository.getCachedTugas(tugasId)
            }
            if (cached == null) {
                _notFound.value = true
            } else {
                _tugas.value = cached
                loadAnswerFiles(cached)
                if (cached.link.isNotBlank()) fetchTeacherLinkPreview(cached.link)
                _studentLink.value = cached.link_student
            }
        }
    }

    private fun loadAnswerFiles(item: HomeworkTable) {
        viewModelScope.launch {
            if (item.uploaded > 0) {
                try {
                    repository.ensureAnswerFiles(item.id.toInt(), item.id.toInt(), item.student_assignment_id)
                } catch (e: Exception) {
                    // answer-detail fetch failing is non-fatal — the cached rows (if any) still show
                }
            }
        }
    }

    private fun fetchTeacherLinkPreview(url: String) {
        viewModelScope.launch {
            _teacherLinkPreviewLoading.value = true
            _teacherLinkPreview.value = linkPreviewRepository.fetch(url)
            _teacherLinkPreviewLoading.value = false
        }
    }

    fun onStudentLinkChanged(url: String) {
        _studentLink.value = url
        linkDebounceJob?.cancel()
        _linkPreview.value = null
        linkPreviewSucceeded = false
        if (url.isBlank()) return
        linkDebounceJob = viewModelScope.launch {
            delay(1000)
            _linkPreviewLoading.value = true
            val result = linkPreviewRepository.fetch(url)
            _linkPreviewLoading.value = false
            if (result == null) {
                emitError("gagal menampilkan preview url")
            } else {
                linkPreviewSucceeded = true
                _linkPreview.value = result
            }
        }
    }

    /** @return an error message if the file is rejected, or null if it was added. */
    fun addSelectedFile(file: SelectedAnswerFile): String? {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        if (ext !in ALLOWED_ANSWER_EXTENSIONS) return "Format file tidak didukung: ${file.name}"
        if (file.sizeBytes > MAX_ANSWER_FILE_BYTES) return "Ukuran maksimal 12 MB per file: ${file.name}"
        _selectedFiles.value = _selectedFiles.value + file
        return null
    }

    fun removeSelectedFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value.filterNot { it.uri == uri }
        // Doc decision: removing every answer file un-satisfies "upload" again rather than keeping
        // a legacy one-way latch — clear the cached answer rows from a previous submission too.
        if (_selectedFiles.value.isEmpty() && _studentLink.value.isBlank()) {
            val id = _tugas.value?.id?.toInt() ?: return
            viewModelScope.launch { repository.clearAnswerFiles(id) }
        }
    }

    /** "baca tugas" / "baca pembahasan" — satisfied only by an actually-completed download. */
    fun openFile(url: String, fileName: String, markRead: Boolean) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _fileLoading.value = true
            try {
                val file = fileOpenRepository.downloadToAppFiles(url, fileName)
                if (markRead) _readDone.value = true
                if (fileOpenRepository.isPdf(file.name)) {
                    _pdfPath.value = file.absolutePath
                } else {
                    _openFileIntent.value = fileOpenRepository.openFileIntent(file)
                }
            } catch (e: Exception) {
                emitError("Gagal membuka file")
            } finally {
                _fileLoading.value = false
            }
        }
    }

    fun downloadFile(url: String, fileName: String) {
        if (url.isBlank()) {
            _infoMessage.value = "Berkas tidak tersedia, mohon ulangi beberapa saat lagi"
            return
        }
        if (fileOpenRepository.enqueueDownload(url, fileName)) {
            _infoMessage.value = "Proses download $fileName akan dimulai sesaat lagi"
        } else {
            _infoMessage.value = "Berkas tidak tersedia, mohon ulangi beberapa saat lagi"
        }
    }

    fun consumeOpenFileIntent() { _openFileIntent.value = null }
    fun consumePdfPath() { _pdfPath.value = null }
    fun consumeInfoMessage() { _infoMessage.value = null }

    /** "Kirim Tugas" — `fileParts` are built by the screen (needs `Context`/`ContentResolver`). */
    fun submit(fileParts: List<MultipartBody.Part>) {
        val item = _tugas.value ?: return
        launchWithHandling(customMessage = "Gagal mengirim tugas") {
            val plain = "text/plain".toMediaTypeOrNull()
            val data = mutableMapOf<String, RequestBody>()
            data["checked"] = "1".toRequestBody(plain)
            val hasAnswer = fileParts.isNotEmpty() || _studentLink.value.isNotBlank()
            data["uploaded"] = (if (hasAnswer) "1" else "0").toRequestBody(plain)
            if (_studentLink.value.isNotBlank()) data["link"] = _studentLink.value.toRequestBody(plain)
            repository.collectAssignment(item.id.toInt(), data, fileParts)
            _submitSuccess.value = true
        }
    }
}
