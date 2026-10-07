package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.ScheduleDetailData
import id.diskola.app.repository.JurnalRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.JurnalRules
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class EditableStudent(
    val id: Int,
    val name: String,
    val nisn: String,
    val status: String,
    val source: String?,
    val overridable: Boolean,
)

/**
 * "Detail Jurnal" (doc `07` §9). A teacher with a started class edits tujuan, status sesi and each student's
 * H/I/S/A, then saves everything in one call (non-overridable students included, as legacy). Anyone else gets a
 * read-only list — and, unlike legacy, izin/sakit are shown as they are instead of collapsing into alpha.
 */
@HiltViewModel
class JurnalDetailViewModel @Inject constructor(
    private val repository: JurnalRepository,
) : BaseViewModel() {

    val isTeacher: Boolean get() = repository.isTeacher && !repository.isStudent

    private var started = false
    private var attendanceId = 0
    private var scheduleId = 0
    private var previewDate: String? = null

    private val _detail = MutableStateFlow<ScheduleDetailData?>(null)
    val detail: StateFlow<ScheduleDetailData?> = _detail.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private val _objective = MutableStateFlow("")
    val objective: StateFlow<String> = _objective.asStateFlow()

    private val _sessionStatus = MutableStateFlow("")
    val sessionStatus: StateFlow<String> = _sessionStatus.asStateFlow()

    private val _students = MutableStateFlow<List<EditableStudent>>(emptyList())
    val students: StateFlow<List<EditableStudent>> = _students.asStateFlow()

    private var initialObjective = ""
    private var initialStatus = ""
    private var initialStudents: List<EditableStudent> = emptyList()

    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _canEdit = MutableStateFlow(false)
    val canEdit: StateFlow<Boolean> = _canEdit.asStateFlow()

    fun start(attendanceId: Int, createdAt: String) {
        if (started) return
        started = true
        this.attendanceId = attendanceId
        previewDate = createdAt.take(10).takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _loadError.value = null
            try {
                val detail = repository.scheduleDetail(attendanceId) ?: throw IllegalStateException("Jurnal tidak ditemukan")
                _detail.value = detail
                scheduleId = detail.school_subject_schedule_id ?: 0
                if (isTeacher) loadTeacher(detail) else loadReadOnly(detail)
                _canEdit.value = JurnalRules.canEditDetail(isTeacher, _isStarted.value, scheduleId)
                rememberInitial()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _loadError.value = AppErrorHandler.getMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadTeacher(detail: ScheduleDetailData) {
        val preview = repository.preview(scheduleId, previewDate)
        if (preview?.school_subject_schedule_id != null && preview.school_subject_schedule_id > 0) scheduleId = preview.school_subject_schedule_id
        _isStarted.value = preview?.is_started == true
        _objective.value = preview?.learning_objective ?: detail.learning_objective
        // "Penugasan" is kept (legacy dropped it and cleared the radio).
        _sessionStatus.value = preview?.session_status.orEmpty()
        _students.value = preview?.students.orEmpty().map {
            EditableStudent(it.student_id, it.name, it.nisn, JurnalRules.normalizeStudentStatus(it.status), it.status_source, it.is_overridable != false)
        }
    }

    private suspend fun loadReadOnly(detail: ScheduleDetailData) {
        _objective.value = detail.learning_objective
        _sessionStatus.value = detail.status
        val fromDetail = detail.student_attendances.orEmpty().map {
            EditableStudent(it.student?.id ?: 0, it.student?.name.orEmpty(), it.student?.nisn.orEmpty(), JurnalRules.normalizeStudentStatus(it.status), it.status_source, false)
        }
        _students.value = fromDetail.ifEmpty {
            repository.listStudent(attendanceId).map {
                EditableStudent(it.school_attendance_id ?: 0, it.name, it.nisn, if (it.attend_at.isNotBlank()) "hadir" else "alpha", null, false)
            }
        }
    }

    private fun rememberInitial() {
        initialObjective = _objective.value
        initialStatus = _sessionStatus.value
        initialStudents = _students.value
        _dirty.value = false
    }

    // ---- Editing ----

    fun onObjectiveSaved(value: String) {
        _objective.value = value.trim()
        recomputeDirty()
    }

    fun onSessionStatus(value: String) {
        _sessionStatus.value = value
        recomputeDirty()
    }

    fun onStudentStatus(studentId: Int, status: String) {
        _students.value = _students.value.map { if (it.id == studentId && it.overridable) it.copy(status = status) else it }
        recomputeDirty()
    }

    /** "Buang perubahan?" → Buang. */
    fun discard() {
        _objective.value = initialObjective
        _sessionStatus.value = initialStatus
        _students.value = initialStudents
        _dirty.value = false
    }

    private fun recomputeDirty() {
        _dirty.value = _objective.value != initialObjective || _sessionStatus.value != initialStatus || _students.value != initialStudents
    }

    /** "H x · I x · S x · A x": the server's breakdown until something is edited, then counted from the rows. */
    fun breakdownLabel(): String {
        val server = _detail.value?.status_breakdown
        val list = _students.value
        val h: Int; val i: Int; val s: Int; val a: Int
        if (!_dirty.value && server != null) {
            h = server.hadir ?: 0; i = server.izin ?: 0; s = server.sakit ?: 0; a = server.alpha ?: 0
        } else {
            h = list.count { it.status == "hadir" }; i = list.count { it.status == "izin" }
            s = list.count { it.status == "sakit" }; a = list.count { it.status == "alpha" }
        }
        return "H $h · I $i · S $s · A $a"
    }

    fun consumeSaved() { _saved.value = false }

    fun save() {
        if (_sessionStatus.value.isBlank()) {
            emitError("Status pembelajaran wajib dipilih")
            return
        }
        if (_students.value.isEmpty()) {
            emitError("Daftar siswa kosong")
            return
        }
        viewModelScope.launch {
            _loading.value = true
            clearError()
            try {
                repository.save(scheduleId, _objective.value, _sessionStatus.value, _students.value.map { it.id to it.status })
                rememberInitial()
                _saved.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                emitError(AppErrorHandler.getMessage(e))
            } finally {
                _loading.value = false
            }
        }
    }
}
