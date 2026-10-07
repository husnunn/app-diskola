package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.repository.MateriRepository
import id.diskola.app.repository.TugasRepository
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

/**
 * "List Tugas" (`HomeworkTeacherListPage`, doc `05-pembelajaran-materi-tugas.md` §6.4) — Kelas/
 * Mapel filters combined with AND, same pattern as `MateriGuruViewModel`. Two confirmed deviations
 * from legacy: a single fetch per filter change (not the legacy triple-fetch-on-open bug), and
 * `onTugasChanged()` forces a real refresh after create/edit/delete (legacy never refreshed without
 * a manual pull).
 */
@HiltViewModel
class TugasGuruViewModel @Inject constructor(
    private val repository: TugasRepository,
    private val materiRepository: MateriRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val teacherId: Int get() = sessionStore.teacher?.id ?: 0

    private val _subjectFilter = MutableStateFlow<Int?>(null)
    val subjectFilter: StateFlow<Int?> = _subjectFilter.asStateFlow()

    private val _classFilter = MutableStateFlow<Int?>(null)
    val classFilter: StateFlow<Int?> = _classFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tugasList: StateFlow<List<HomeworkTable>> = combine(_subjectFilter, _classFilter) { s, c -> s to c }
        .flatMapLatest { (subjectId, classId) -> repository.observeTeacherOwn(teacherId, subjectId, classId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<ClassRoomTable>> =
        materiRepository.observeClasses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<MapelTable>> =
        materiRepository.observeSubjects("").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _listLoading = MutableStateFlow(false)
    val listLoading: StateFlow<Boolean> = _listLoading.asStateFlow()

    private var hasNextPage = true
    private var loadingMore = false

    /** Runs once per backstack entry (this ViewModel's own lifetime), not once per composition —
     * the screen itself must NOT also call this from a `LaunchedEffect(Unit)`, or returning from
     * the upload form (which independently triggers [onTugasChanged]) double-fetches the list.
     * This was a real, confirmed bug (found via device testing), and `MateriGuruViewModel`/
     * `MateriGuruScreen` had the exact same one — fixed there too, see that file's history. */
    init {
        viewModelScope.launch {
            try {
                materiRepository.refreshClasses()
            } catch (e: Exception) {
                // reference-data refresh failing is non-fatal — the list fetch below still runs
            }
        }
        viewModelScope.launch {
            try {
                materiRepository.ensureTeacherSubjectsFirstPage()
            } catch (e: Exception) {
                // same as above
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

    private fun ensureFirstPage() = launchWithHandling(showLoading = false) {
        _listLoading.value = true
        try {
            repository.ensureTeacherOwnFirstPage(teacherId, _subjectFilter.value, _classFilter.value)
        } finally {
            _listLoading.value = false
        }
    }

    fun loadMore() {
        if (loadingMore || !hasNextPage) return
        loadingMore = true
        launchWithHandling(showLoading = false, showError = false) {
            hasNextPage = repository.loadMoreTeacherOwn(teacherId, _subjectFilter.value, _classFilter.value, tugasList.value.size)
        }.invokeOnCompletion { loadingMore = false }
    }

    fun refresh() = launchWithHandling(showLoading = false) {
        _listLoading.value = true
        try {
            hasNextPage = repository.refreshTeacherOwn(teacherId, _subjectFilter.value, _classFilter.value)
        } finally {
            _listLoading.value = false
        }
    }

    /** Called when returning from the upload form after a successful create/edit. */
    fun onTugasChanged() = refresh()

    fun deleteTugas(id: Int) {
        launchWithHandling(customMessage = "Gagal menghapus tugas. Silakan coba lagi.") {
            repository.deleteTugas(id)
        }
    }
}
