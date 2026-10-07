package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.HomeworkCollected
import id.diskola.app.repository.TugasRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/** "Penilaian" tab (doc §6.4's `HomeworkTeacherNilaiPage`) — no filters, just a paged list of
 * tugas groups that have at least one student submission. */
@HiltViewModel
class TugasPenilaianViewModel @Inject constructor(
    private val repository: TugasRepository,
) : BaseViewModel() {

    val scoredGroups: StateFlow<List<HomeworkCollected>> =
        repository.observeScoredGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _listLoading = MutableStateFlow(false)
    val listLoading: StateFlow<Boolean> = _listLoading.asStateFlow()

    private var hasNextPage = true
    private var loadingMore = false

    /** Runs once per backstack entry, not once per composition — see `TugasGuruViewModel`'s
     * `init` doc comment for why this must not also be called from a `LaunchedEffect(Unit)`. */
    init {
        launchWithHandling(showLoading = false) {
            _listLoading.value = true
            try {
                repository.ensureScoredGroupsFirstPage()
            } finally {
                _listLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasNextPage) return
        loadingMore = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextPage = repository.loadMoreScoredGroups(scoredGroups.value.size)
        }.invokeOnCompletion { loadingMore = false }
    }

    fun refresh() = launchWithHandling(showLoading = false) {
        _listLoading.value = true
        try {
            hasNextPage = repository.refreshScoredGroups()
        } finally {
            _listLoading.value = false
        }
    }
}
