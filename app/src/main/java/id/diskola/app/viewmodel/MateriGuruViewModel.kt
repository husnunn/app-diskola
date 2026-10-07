package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.repository.MateriRepository
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * "Materi Saya" (`MateriTeacherpage`) — doc `05-pembelajaran-materi-tugas.md` §3.4. Kelas/Mapel
 * filters are combined with AND (decision: fix the legacy exclusive-filter/no-reset behaviour),
 * and the list refreshes for real after create/edit/delete (decision: fix the legacy guard bug
 * that left the list stale until a manual pull-to-refresh, §3.8).
 */
@HiltViewModel
class MateriGuruViewModel @Inject constructor(
    private val repository: MateriRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val teacherId: Int get() = sessionStore.teacher?.id ?: 0

    private val _subjectFilter = MutableStateFlow<Int?>(null)
    val subjectFilter: StateFlow<Int?> = _subjectFilter.asStateFlow()

    private val _classFilter = MutableStateFlow<Int?>(null)
    val classFilter: StateFlow<Int?> = _classFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val materiList: StateFlow<List<MateriTable>> = combine(_subjectFilter, _classFilter) { s, c -> s to c }
        .flatMapLatest { (subjectId, classId) -> repository.observeTeacherOwnMateri(teacherId, subjectId, classId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<ClassRoomTable>> =
        repository.observeClasses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<MapelTable>> =
        repository.observeSubjects("").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _listLoading = MutableStateFlow(false)
    val listLoading: StateFlow<Boolean> = _listLoading.asStateFlow()

    private var hasNextPage = true
    private var loadingMore = false

    /** Doc §3.4: the Kelas filter dropdown's own class list is fetched fresh every time this
     * screen opens; the subject dropdown reuses the same subject cache as the picker screens.
     * Runs once per backstack entry (this ViewModel's own lifetime), not once per composition —
     * confirmed via device testing that calling this again from the screen's own
     * `LaunchedEffect(Unit)` double-fetches the list when returning from the upload form, since
     * `onMateriChanged()` independently triggers a refresh on the very same recomposition. */
    init {
        viewModelScope.launch {
            try {
                repository.refreshClasses()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        viewModelScope.launch {
            try {
                repository.ensureTeacherSubjectsFirstPage()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        ensureFirstPage()
    }

    fun setSubjectFilter(id: Int?) {
        _subjectFilter.value = id
        ensureFirstPage()
    }

    fun setClassFilter(id: Int?) {
        _classFilter.value = id
        ensureFirstPage()
    }

    private fun ensureFirstPage() {
        val subjectId = _subjectFilter.value
        val classId = _classFilter.value
        viewModelScope.launch {
            _listLoading.value = true
            try {
                repository.ensureTeacherOwnFirstPage(teacherId, subjectId, classId)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _listLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasNextPage) return
        loadingMore = true
        viewModelScope.launch {
            try {
                hasNextPage = repository.loadMoreTeacherOwn(_subjectFilter.value, _classFilter.value, materiList.value.size)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                loadingMore = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _listLoading.value = true
            try {
                hasNextPage = repository.refreshTeacherOwn(teacherId, _subjectFilter.value, _classFilter.value)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _listLoading.value = false
            }
        }
    }

    /** Called when returning from the upload form after a successful create/edit. */
    fun onMateriChanged() = refresh()

    fun deleteMateri(id: Int) {
        launchWithHandling(customMessage = "Gagal menghapus materi. Silakan coba lagi.") {
            repository.deleteMateri(id)
        }
    }
}
