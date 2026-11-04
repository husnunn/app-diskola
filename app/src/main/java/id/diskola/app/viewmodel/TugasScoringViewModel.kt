package id.diskola.app.viewmodel

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.dataclass.ResponData.AssignmentAnswerFile
import id.diskola.app.repository.FileOpenRepository
import id.diskola.app.repository.TugasRepository
import id.diskola.app.utils.AnswerFileRules
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** `HomeworkScoringPage` (doc §6.4) — the [Assignment] being scored is handed in by the screen
 * (already loaded a moment ago by `TugasTerkumpulViewModel`, not re-fetched) rather than passed
 * through navigation args, since it isn't a flat/serializable shape. */
@HiltViewModel
class TugasScoringViewModel @Inject constructor(
    private val repository: TugasRepository,
    private val fileOpenRepository: FileOpenRepository,
) : BaseViewModel() {

    // ---- The student's answer files: "Baca" opens one (a PDF in the app's own viewer), "Download" saves it (legacy `HomeworkScoringPage`) ----

    private val _fileLoading = MutableStateFlow(false)
    val fileLoading: StateFlow<Boolean> = _fileLoading.asStateFlow()
    private val _pdfPath = MutableStateFlow<String?>(null)
    val pdfPath: StateFlow<String?> = _pdfPath.asStateFlow()
    private val _openFileIntent = MutableStateFlow<Intent?>(null)
    val openFileIntent: StateFlow<Intent?> = _openFileIntent.asStateFlow()
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun openAnswerFile(file: AssignmentAnswerFile) {
        val url = AnswerFileRules.url(file)
        if (url.isBlank()) {
            _infoMessage.value = "File tidak tersedia"
            return
        }
        viewModelScope.launch {
            _fileLoading.value = true
            try {
                val local = fileOpenRepository.downloadToAppFiles(url, AnswerFileRules.name(file).ifBlank { "jawaban" })
                if (fileOpenRepository.isPdf(local.name)) {
                    _pdfPath.value = local.absolutePath
                } else {
                    _openFileIntent.value = fileOpenRepository.openFileIntent(local)
                }
            } catch (e: Exception) {
                // The reason, as legacy toasted `e.message`; the app's own wording when there is none.
                emitError(e.message?.takeIf { it.isNotBlank() } ?: "Gagal membuka file")
            } finally {
                _fileLoading.value = false
            }
        }
    }

    fun downloadAnswerFile(file: AssignmentAnswerFile) {
        val url = AnswerFileRules.url(file)
        val name = AnswerFileRules.name(file).ifBlank { "jawaban" }
        if (url.isBlank()) {
            _infoMessage.value = "File tidak tersedia"
            return
        }
        _infoMessage.value = if (fileOpenRepository.enqueueDownload(url, name)) {
            "Proses download $name akan dimulai sesaat lagi"
        } else {
            "File tidak tersedia"
        }
    }

    fun consumePdfPath() { _pdfPath.value = null }
    fun consumeOpenFileIntent() { _openFileIntent.value = null }
    fun consumeInfoMessage() { _infoMessage.value = null }

    private val _assignment = MutableStateFlow<Assignment?>(null)
    val assignment: StateFlow<Assignment?> = _assignment.asStateFlow()

    private val _score = MutableStateFlow("")
    val score: StateFlow<String> = _score.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun load(assignment: Assignment) {
        if (_assignment.value?.id == assignment.id) return
        _assignment.value = assignment
        _score.value = if (assignment.scored == 1) assignment.score.toString() else ""
    }

    fun onScoreChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(3)
        val clamped = digitsOnly.toIntOrNull()?.coerceIn(0, 100)?.toString() ?: digitsOnly
        _score.value = clamped
    }

    fun submit(collectedId: Int) {
        val assignmentId = _assignment.value?.id ?: return
        val scoreValue = _score.value.toIntOrNull() ?: return
        // A submission whose ids the server could not resolve arrives with `id: null`, read as 0: there is nothing to score, so no request goes out.
        if (collectedId <= 0 || assignmentId <= 0) {
            emitError("Data pengumpulan siswa tidak tersedia")
            return
        }
        // The server's own words (legacy showed `e.localizedMessage`), not a fixed "Gagal menyimpan nilai" that hid why the score was refused.
        launchWithHandling {
            repository.saveScore(collectedId, assignmentId, scoreValue)
            _saveSuccess.value = true
        }
    }
}
