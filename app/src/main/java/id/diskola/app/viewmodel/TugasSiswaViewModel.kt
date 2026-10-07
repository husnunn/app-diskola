package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.repository.TugasRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

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

    val backlog: StateFlow<List<HomeworkTable>> =
        repository.observeBacklog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val done: StateFlow<List<HomeworkTable>> =
        repository.observeDone().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val scored: StateFlow<List<HomeworkTable>> =
        repository.observeScored().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backlogLoading = MutableStateFlow(false)
    val backlogLoading: StateFlow<Boolean> = _backlogLoading.asStateFlow()
    private var hasNextBacklog = true
    private var loadingMoreBacklog = false

    private val _doneLoading = MutableStateFlow(false)
    val doneLoading: StateFlow<Boolean> = _doneLoading.asStateFlow()
    private var hasNextDone = true
    private var loadingMoreDone = false

    private val _scoredLoading = MutableStateFlow(false)
    val scoredLoading: StateFlow<Boolean> = _scoredLoading.asStateFlow()
    private var hasNextScored = true
    private var loadingMoreScored = false

    fun ensureBacklog() = launchWithHandling(showLoading = false) {
        _backlogLoading.value = true
        try {
            repository.ensureBacklogFirstPage()
        } finally {
            _backlogLoading.value = false
        }
    }

    fun loadMoreBacklog() {
        if (loadingMoreBacklog || !hasNextBacklog) return
        loadingMoreBacklog = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextBacklog = repository.loadMoreBacklog(backlog.value.size)
        }.invokeOnCompletion { loadingMoreBacklog = false }
    }

    fun refreshBacklog() = launchWithHandling(showLoading = false) {
        _backlogLoading.value = true
        try {
            hasNextBacklog = repository.refreshBacklog()
        } finally {
            _backlogLoading.value = false
        }
    }

    fun ensureDone() = launchWithHandling(showLoading = false) {
        _doneLoading.value = true
        try {
            repository.ensureDoneFirstPage()
        } finally {
            _doneLoading.value = false
        }
    }

    fun loadMoreDone() {
        if (loadingMoreDone || !hasNextDone) return
        loadingMoreDone = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextDone = repository.loadMoreDone(done.value.size)
        }.invokeOnCompletion { loadingMoreDone = false }
    }

    fun refreshDone() = launchWithHandling(showLoading = false) {
        _doneLoading.value = true
        try {
            hasNextDone = repository.refreshDone()
        } finally {
            _doneLoading.value = false
        }
    }

    fun ensureScored() = launchWithHandling(showLoading = false) {
        _scoredLoading.value = true
        try {
            repository.ensureScoredFirstPage()
        } finally {
            _scoredLoading.value = false
        }
    }

    fun loadMoreScored() {
        if (loadingMoreScored || !hasNextScored) return
        loadingMoreScored = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextScored = repository.loadMoreScored(scored.value.size)
        }.invokeOnCompletion { loadingMoreScored = false }
    }

    fun refreshScored() = launchWithHandling(showLoading = false) {
        _scoredLoading.value = true
        try {
            hasNextScored = repository.refreshScored()
        } finally {
            _scoredLoading.value = false
        }
    }
}
