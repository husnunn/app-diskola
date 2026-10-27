package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.repository.TugasRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** `HomeworkScoringPage` (doc §6.4) — the [Assignment] being scored is handed in by the screen
 * (already loaded a moment ago by `TugasTerkumpulViewModel`, not re-fetched) rather than passed
 * through navigation args, since it isn't a flat/serializable shape. */
@HiltViewModel
class TugasScoringViewModel @Inject constructor(
    private val repository: TugasRepository,
) : BaseViewModel() {

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
