package id.diskola.app.viewmodel

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.repository.FileOpenRepository
import id.diskola.app.repository.TugasRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Score-detail sheet reached from the "Nilai" tab (doc §5.5's `HomeworkNilaiDetailPage`) — a
 * narrow read-only load-by-id plus the "baca materi"/"Download" pembahasan actions, kept separate
 * from [TugasDetailViewModel] since it has no requirement/submit logic at all. */
@HiltViewModel
class TugasNilaiDetailViewModel @Inject constructor(
    private val repository: TugasRepository,
    private val fileOpenRepository: FileOpenRepository,
) : BaseViewModel() {

    private val _tugas = MutableStateFlow<HomeworkTable?>(null)
    val tugas: StateFlow<HomeworkTable?> = _tugas.asStateFlow()

    private val _fileLoading = MutableStateFlow(false)
    val fileLoading: StateFlow<Boolean> = _fileLoading.asStateFlow()
    private val _pdfPath = MutableStateFlow<String?>(null)
    val pdfPath: StateFlow<String?> = _pdfPath.asStateFlow()
    private val _openFileIntent = MutableStateFlow<Intent?>(null)
    val openFileIntent: StateFlow<Intent?> = _openFileIntent.asStateFlow()
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private var loadedFor = 0

    fun load(tugasId: Int) {
        if (loadedFor == tugasId && _tugas.value != null) return
        loadedFor = tugasId
        launchWithHandling(showError = false) {
            _tugas.value = repository.getCachedTugas(tugasId)
        }
    }

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
}
