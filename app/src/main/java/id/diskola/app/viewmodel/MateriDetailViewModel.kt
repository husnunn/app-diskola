package id.diskola.app.viewmodel

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.repository.FileOpenRepository
import id.diskola.app.repository.LinkPreviewData
import id.diskola.app.repository.LinkPreviewRepository
import id.diskola.app.repository.MateriRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * `MateriDetailPage` (doc `05-pembelajaran-materi-tugas.md` §2.4): Room-first lookup with an API
 * fallback for an id it hasn't seen yet (e.g. a future FCM `menu=theory` deep link), an
 * unrecoverable-id dialog instead of the legacy NPE crash, and the "baca"/"Unduh"/link actions the
 * screen used to have no-op callbacks for.
 */
@HiltViewModel
class MateriDetailViewModel @Inject constructor(
    private val repository: MateriRepository,
    private val fileOpenRepository: FileOpenRepository,
    private val linkPreviewRepository: LinkPreviewRepository,
) : BaseViewModel() {

    private val _materi = MutableStateFlow<MateriTable?>(null)
    val materi: StateFlow<MateriTable?> = _materi.asStateFlow()

    /** Doc §2.4: invalid/missing id → non-cancelable "Materi Tidak tersedia" dialog, not a crash. */
    private val _invalid = MutableStateFlow(false)
    val invalid: StateFlow<Boolean> = _invalid.asStateFlow()

    private val _linkPreview = MutableStateFlow<LinkPreviewData?>(null)
    val linkPreview: StateFlow<LinkPreviewData?> = _linkPreview.asStateFlow()

    private val _linkPreviewLoading = MutableStateFlow(false)
    val linkPreviewLoading: StateFlow<Boolean> = _linkPreviewLoading.asStateFlow()

    private val _fileLoading = MutableStateFlow(false)
    val fileLoading: StateFlow<Boolean> = _fileLoading.asStateFlow()

    private val _pdfPath = MutableStateFlow<String?>(null)
    val pdfPath: StateFlow<String?> = _pdfPath.asStateFlow()

    private val _openFileIntent = MutableStateFlow<Intent?>(null)
    val openFileIntent: StateFlow<Intent?> = _openFileIntent.asStateFlow()

    /** Plain informational toast text (doc §4.3's "proses download … akan dimulai" message) —
     * kept separate from [errorMessage] since it isn't an error. */
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun consumeInfoMessage() {
        _infoMessage.value = null
    }

    private var loadedFor = 0

    fun load(materiId: Int, subjectId: Int, isTeacher: Boolean) {
        if (loadedFor == materiId && _materi.value != null) return
        loadedFor = materiId
        if (materiId <= 0 || subjectId <= 0) {
            _invalid.value = true
            return
        }
        launchWithHandling(showError = false) {
            val result = repository.getDetail(materiId, subjectId, isTeacher)
            if (result == null) {
                _invalid.value = true
            } else {
                _materi.value = result
                if (result.link.isNotBlank()) fetchLinkPreview(result.link)
            }
        }
    }

    private fun fetchLinkPreview(url: String) {
        viewModelScope.launch {
            _linkPreviewLoading.value = true
            _linkPreview.value = linkPreviewRepository.fetch(url)
            _linkPreviewLoading.value = false
        }
    }

    /** "baca materi"/"baca pembahasan" — downloads then either opens the in-app PDF viewer or
     * hands the file to an external app (doc §2.4/§4.1/§4.3). */
    fun openFile(url: String, fileName: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _fileLoading.value = true
            try {
                val file = fileOpenRepository.downloadToAppFiles(url, fileName)
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

    /** Doc §4.3: toast-and-enqueue, not a suspend round trip — `DownloadManager` does the rest. */
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

    fun consumeOpenFileIntent() {
        _openFileIntent.value = null
    }

    fun consumePdfPath() {
        _pdfPath.value = null
    }
}
