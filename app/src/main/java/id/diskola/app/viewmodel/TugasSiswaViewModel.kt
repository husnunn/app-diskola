package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.repository.TugasRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * The 3-tab student shell (Belum Dikerjakan/Sudah Dikerjakan/Nilai, doc
 * `05-pembelajaran-materi-tugas.md` §5.3/§5.5) — each tab pages independently (10/page). Unlike
 * the legacy app, list-fetch failures surface a banner (`errorMessage`) instead of failing
 * silently — a deliberate deviation confirmed with the user for this feature.
 */
@HiltViewModel
class TugasSiswaViewModel @Inject constructor(
    private val repository: TugasRepository,
) : BaseViewModel() {

    private val backlogLoad = ListLoad(viewModelScope, repository.observeBacklog())
    val backlog: StateFlow<List<HomeworkTable>> = backlogLoad.items
    val backlogLoading: StateFlow<Boolean> = backlogLoad.loading
    private var hasNextBacklog = true
    private var loadingMoreBacklog = false

    private val doneLoad = ListLoad(viewModelScope, repository.observeDone())
    val done: StateFlow<List<HomeworkTable>> = doneLoad.items
    val doneLoading: StateFlow<Boolean> = doneLoad.loading
    private var hasNextDone = true
    private var loadingMoreDone = false

    private val scoredLoad = ListLoad(viewModelScope, repository.observeScored())
    val scored: StateFlow<List<HomeworkTable>> = scoredLoad.items
    val scoredLoading: StateFlow<Boolean> = scoredLoad.loading
    private var hasNextScored = true
    private var loadingMoreScored = false

    fun ensureBacklog() = launchWithHandling(showLoading = false) {
        backlogLoad.fetch { repository.ensureBacklogFirstPage() }
    }

    fun loadMoreBacklog() {
        if (loadingMoreBacklog || !hasNextBacklog) return
        loadingMoreBacklog = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextBacklog = repository.loadMoreBacklog(backlog.value.size)
        }.invokeOnCompletion { loadingMoreBacklog = false }
    }

    fun refreshBacklog() = launchWithHandling(showLoading = false) {
        backlogLoad.fetch { hasNextBacklog = repository.refreshBacklog() }
    }

    fun ensureDone() = launchWithHandling(showLoading = false) {
        doneLoad.fetch { repository.ensureDoneFirstPage() }
    }

    fun loadMoreDone() {
        if (loadingMoreDone || !hasNextDone) return
        loadingMoreDone = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextDone = repository.loadMoreDone(done.value.size)
        }.invokeOnCompletion { loadingMoreDone = false }
    }

    fun refreshDone() = launchWithHandling(showLoading = false) {
        doneLoad.fetch { hasNextDone = repository.refreshDone() }
    }

    fun ensureScored() = launchWithHandling(showLoading = false) {
        scoredLoad.fetch { repository.ensureScoredFirstPage() }
    }

    fun loadMoreScored() {
        if (loadingMoreScored || !hasNextScored) return
        loadingMoreScored = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextScored = repository.loadMoreScored(scored.value.size)
        }.invokeOnCompletion { loadingMoreScored = false }
    }

    fun refreshScored() = launchWithHandling(showLoading = false) {
        scoredLoad.fetch { hasNextScored = repository.refreshScored() }
    }
}
