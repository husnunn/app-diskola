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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val subjects: StateFlow<List<MapelTable>> = _subjectQuery
        .debounce(400)
        .flatMapLatest { query -> repository.observeSubjects(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _subjectsLoading = MutableStateFlow(false)
    val subjectsLoading: StateFlow<Boolean> = _subjectsLoading.asStateFlow()

    private var hasNextSubjectPage = true
    private var loadingMoreSubjects = false

    fun onSubjectQueryChange(query: String) {
        _subjectQuery.value = query
    }

    fun openSubjectPicker(isTeacher: Boolean) {
        if (subjects.value.isNotEmpty()) return
        viewModelScope.launch {
            _subjectsLoading.value = true
            try {
                if (isTeacher) repository.ensureTeacherSubjectsFirstPage() else repository.ensureStudentSubjectsFirstPage()
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _subjectsLoading.value = false
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
            _subjectsLoading.value = true
            try {
                hasNextSubjectPage = if (isTeacher) repository.refreshTeacherSubjects() else repository.refreshStudentSubjects()
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _subjectsLoading.value = false
            }
        }
    }

    // ---- Materi within one subject ----

    private val _currentSubjectId = MutableStateFlow(0)
    private var currentIsTeacher = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val materiOfSubject: StateFlow<List<MateriTable>> = _currentSubjectId
        .flatMapLatest { id -> if (id > 0) repository.observeMateriBySubject(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _materiLoading = MutableStateFlow(false)
    val materiLoading: StateFlow<Boolean> = _materiLoading.asStateFlow()

    private var hasNextMateriPage = true
    private var loadingMoreMateri = false

    fun openSubjectMateri(subjectId: Int, isTeacher: Boolean) {
        currentIsTeacher = isTeacher
        _currentSubjectId.value = subjectId
        viewModelScope.launch {
            _materiLoading.value = true
            try {
                repository.ensureSubjectMateriFirstPage(subjectId, isTeacher)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _materiLoading.value = false
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
            _materiLoading.value = true
            try {
                hasNextMateriPage = repository.refreshSubjectMateri(subjectId, currentIsTeacher)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                _materiLoading.value = false
            }
        }
    }
}
