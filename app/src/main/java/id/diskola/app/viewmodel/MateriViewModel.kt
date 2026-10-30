package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.repository.MateriRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Backs the subject pickers (student `TheoryPage` / teacher's own "Mata Pelajaran") and the
 * per-subject materi list (`MateriPage`, shared by both roles) — doc
 * `05-pembelajaran-materi-tugas.md` §2.4/§3.4. "Materi Saya" (the teacher's filterable own-list)
 * has its own `MateriGuruViewModel`; upload/edit has `UploadMateriViewModel`; detail has
 * `MateriDetailViewModel` — kept separate per `docs/rules-global.md` §5.2 (no God ViewModel).
 */
@HiltViewModel
class MateriViewModel @Inject constructor(
    private val repository: MateriRepository,
) : BaseViewModel() {

    // ---- Subject picker ----

    private val _subjectQuery = MutableStateFlow("")
    val subjectQuery: StateFlow<String> = _subjectQuery.asStateFlow()

    /** Typing waits 400 ms for a pause; the empty box (the first value, and clearing the search) does not, or the list would be unknown for that long on every open. */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val subjectsLoad = ListLoad(
        viewModelScope,
        _subjectQuery
            .debounce { query -> if (query.isEmpty()) 0L else 400L }
            .flatMapLatest { query -> repository.observeSubjects(query) },
    )
    val subjects: StateFlow<List<MapelTable>> = subjectsLoad.items
    val subjectsLoading: StateFlow<Boolean> = subjectsLoad.loading

    private var hasNextSubjectPage = true
    private var loadingMoreSubjects = false

    fun onSubjectQueryChange(query: String) {
        _subjectQuery.value = query
    }

    fun openSubjectPicker(isTeacher: Boolean) {
        if (subjects.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                subjectsLoad.fetch {
                    if (isTeacher) repository.ensureTeacherSubjectsFirstPage() else repository.ensureStudentSubjectsFirstPage()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    /** Only while the search box is empty and a previous page came back full — mirrors the
     * legacy `PagedListBoundaryCallback` (doc §2.4). */
    fun loadMoreSubjects(isTeacher: Boolean) {
        if (_subjectQuery.value.isNotBlank() || loadingMoreSubjects || !hasNextSubjectPage) return
        loadingMoreSubjects = true
        viewModelScope.launch {
            try {
                hasNextSubjectPage = if (isTeacher) {
                    repository.loadMoreTeacherSubjects(subjects.value.size)
                } else {
                    repository.loadMoreStudentSubjects(subjects.value.size)
                }
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                loadingMoreSubjects = false
            }
        }
    }

    fun refreshSubjects(isTeacher: Boolean) {
        viewModelScope.launch {
            try {
                subjectsLoad.fetch {
                    hasNextSubjectPage = if (isTeacher) repository.refreshTeacherSubjects() else repository.refreshStudentSubjects()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    // ---- Materi within one subject ----

    private val _currentSubjectId = MutableStateFlow(0)
    private var currentIsTeacher = false

    /** No subject yet means no answer yet (not "empty"): [emptyFlow], so the list stays unknown until a subject is opened. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val materiLoad = ListLoad(
        viewModelScope,
        _currentSubjectId.flatMapLatest { id -> if (id > 0) repository.observeMateriBySubject(id) else emptyFlow() },
    )
    val materiOfSubject: StateFlow<List<MateriTable>> = materiLoad.items
    val materiLoading: StateFlow<Boolean> = materiLoad.loading

    private var hasNextMateriPage = true
    private var loadingMoreMateri = false

    fun openSubjectMateri(subjectId: Int, isTeacher: Boolean) {
        currentIsTeacher = isTeacher
        _currentSubjectId.value = subjectId
        viewModelScope.launch {
            try {
                materiLoad.fetch { repository.ensureSubjectMateriFirstPage(subjectId, isTeacher) }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun loadMoreMateri() {
        val subjectId = _currentSubjectId.value
        if (subjectId <= 0 || loadingMoreMateri || !hasNextMateriPage) return
        loadingMoreMateri = true
        viewModelScope.launch {
            try {
                hasNextMateriPage = repository.loadMoreSubjectMateri(subjectId, currentIsTeacher, materiOfSubject.value.size)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                loadingMoreMateri = false
            }
        }
    }

    fun refreshMateri() {
        val subjectId = _currentSubjectId.value
        if (subjectId <= 0) return
        viewModelScope.launch {
            try {
                materiLoad.fetch { hasNextMateriPage = repository.refreshSubjectMateri(subjectId, currentIsTeacher) }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
