package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.dataclass.ResponData.AssignmentData
import id.diskola.app.repository.TugasRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ScoringFilter { SEMUA, BELUM_DINILAI, SUDAH_DINILAI }

/** "Tugas Terkumpul" (doc §6.4's `HomeworkSubmittedPage`) — always-network detail by id (no Room
 * cache; the per-student score list is mutable every time a score is saved), plus a local-only
 * Semua/Belum/Sudah filter over the already-loaded `assignments` list. */
@HiltViewModel
class TugasTerkumpulViewModel @Inject constructor(
    private val repository: TugasRepository,
) : BaseViewModel() {

    private val _detail = MutableStateFlow<AssignmentData?>(null)
    val detail: StateFlow<AssignmentData?> = _detail.asStateFlow()

    private val _filter = MutableStateFlow(ScoringFilter.SEMUA)
    val filter: StateFlow<ScoringFilter> = _filter.asStateFlow()

    val filteredAssignments: StateFlow<List<Assignment>> = combine(_detail, _filter) { detail, filter ->
        val all = detail?.assignments.orEmpty()
        when (filter) {
            ScoringFilter.SEMUA -> all
            ScoringFilter.BELUM_DINILAI -> all.filter { it.scored == 0 }
            ScoringFilter.SUDAH_DINILAI -> all.filter { it.scored == 1 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loadedFor = 0

    fun load(collectedId: Int) {
        if (loadedFor == collectedId && _detail.value != null) return
        loadedFor = collectedId
        launchWithHandling {
            _detail.value = repository.getScoredDetail(collectedId)
        }
    }

    fun refresh(collectedId: Int) = launchWithHandling(showLoading = false) {
        _detail.value = repository.getScoredDetail(collectedId)
    }

    fun setFilter(filter: ScoringFilter) {
        _filter.value = filter
    }

    /** Find an already-loaded assignment by id — the scoring screen reuses this instead of
     * fetching it again (same data, loaded a moment ago by this same screen). */
    fun findAssignment(assignmentId: Int): Assignment? = _detail.value?.assignments?.find { it.id == assignmentId }
}
