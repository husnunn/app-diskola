package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.SearchPoinStudentItem
import id.diskola.app.repository.PoinRepository
import id.diskola.app.repository.PoinTarget
import id.diskola.app.utils.AppErrorHandler
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface PoinAccess {
    data object Checking : PoinAccess
    data object Allowed : PoinAccess
    data object Denied : PoinAccess
}

sealed interface PoinSearchState {
    data object Idle : PoinSearchState
    data object Loading : PoinSearchState
    data class Result(val items: List<SearchPoinStudentItem>) : PoinSearchState
    data class Error(val message: String) : PoinSearchState
}

/** The student the teacher is currently acting on. [userId] ≠ [student].id — see [PoinTarget.Ready]. */
data class PoinSelected(val student: SearchPoinStudentItem, val userId: Int)

/**
 * Teacher flow root (search → hasil → form), scoped to the `PoinGuru` back-stack entry so the
 * selected student survives the child screens. [selected] is overwritten on every pick, so a new
 * student never inherits the previous one's state (legacy kept it in the activity-wide ViewModel).
 */
@HiltViewModel
class PoinGuruViewModel @Inject constructor(
    private val repository: PoinRepository,
) : BaseViewModel() {

    private val _access = MutableStateFlow<PoinAccess>(PoinAccess.Checking)
    val access: StateFlow<PoinAccess> = _access.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selected = MutableStateFlow<PoinSelected?>(null)
    val selected: StateFlow<PoinSelected?> = _selected.asStateFlow()

    private val _blockedMessage = MutableStateFlow("")
    val blockedMessage: StateFlow<String> = _blockedMessage.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val search: StateFlow<PoinSearchState> = _query
        .map { it.trim() }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow {
                if (q.length < MIN_QUERY) {
                    emit(PoinSearchState.Idle)
                    return@flow
                }
                emit(PoinSearchState.Loading)
                try {
                    emit(PoinSearchState.Result(repository.searchStudents(q)))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e)
                    emit(PoinSearchState.Error(AppErrorHandler.getMessage(e)))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PoinSearchState.Idle)

    init {
        viewModelScope.launch {
            _access.value = if (repository.isCooperativeOnly()) PoinAccess.Denied else PoinAccess.Allowed
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Resolves [student] to a `user_id`; [onReady] runs only when it is safe to continue. */
    fun pick(student: SearchPoinStudentItem, onReady: () -> Unit) {
        launchWithHandling {
            when (val target = repository.checkTarget(student)) {
                is PoinTarget.Ready -> {
                    _selected.value = PoinSelected(student, target.userId)
                    onReady()
                }
                is PoinTarget.Blocked -> _blockedMessage.value = target.message
            }
        }
    }

    fun dismissBlocked() {
        _blockedMessage.value = ""
    }

    /** After a successful submit the totals on the result card are stale; re-read this student's row. */
    fun refreshSelectedScores() {
        val current = _selected.value ?: return
        viewModelScope.launch {
            try {
                val fresh = repository.searchStudents(current.student.nisn.ifBlank { current.student.name })
                    .firstOrNull { it.id == current.student.id }
                if (fresh != null) _selected.value = current.copy(student = fresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private companion object {
        const val MIN_QUERY = 3
    }
}
