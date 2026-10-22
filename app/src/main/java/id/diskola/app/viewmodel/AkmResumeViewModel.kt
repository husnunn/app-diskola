package id.diskola.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.akm.AkmExam
import id.diskola.app.dataclass.akm.toAkmExams
import id.diskola.app.repository.AkmRepository
import id.diskola.app.repository.AkmSettingsRepository
import id.diskola.app.repository.SubmitOutcome
import id.diskola.app.ui.navigation.Route
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.AkmRules.SubmissionState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResumeUiState(
    val loaded: Boolean = false,
    val name: String = "",
    val exams: List<AkmExam> = emptyList(),
    val answered: Int = 0,
    val total: Int = 0,
    /** Every question has an answer: "Kumpulkan" is enabled (SCHOOL scope, doc 06 §5.5). */
    val allAnswered: Boolean = false,
    /** The answers are queued or sent: nothing can be edited or submitted again. */
    val locked: Boolean = false,
)

/** What "Kumpulkan" is showing. Texts are legacy's (doc 06 §5.5): "Sedang memproses jawaban", "Berhasil", "Gagal". */
sealed interface SubmitUi {
    data object Idle : SubmitUi
    data object Processing : SubmitUi
    data object Done : SubmitUi
    data class Failed(val message: String) : SubmitUi
}

/**
 * The exam lobby (`AkmTakeResumePage`): the instruction tree with its progress, "Kumpulkan", and the end-of-exam alert. Reads
 * everything from [AkmRepository], so a restart of the process shows the same answers.
 */
@HiltViewModel
class AkmResumeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AkmRepository,
    private val settings: AkmSettingsRepository,
) : ViewModel() {

    private val examId = savedStateHandle.toRoute<Route.Akm.Resume>().id

    /** Whether leaving now is told to cost a penalty (the text of the leave warning depends on it, doc 06 §12.1). */
    val penaltyApplied: Boolean get() = settings.isPenaltyApplied()

    val state: StateFlow<ResumeUiState> = combine(
        repository.examPackage(examId),
        repository.answers(examId),
        repository.submission(examId),
    ) { exam, answers, submission ->
        val schedule = exam?.schedule ?: return@combine ResumeUiState(loaded = exam != null)
        val exams = schedule.toAkmExams(answers.keys)
        val questionIds = repository.instructionsOf(schedule).flatMap { it.questions }.map { it.id }
        ResumeUiState(
            loaded = true,
            name = (schedule.exams ?: listOfNotNull(schedule.exam)).map { it.name }.distinct().joinToString(", ").ifBlank { "Asesmen" },
            exams = exams,
            answered = exams.sumOf { it.instructions.sumOf { i -> i.answered } },
            total = exams.sumOf { it.instructions.sumOf { i -> i.total } },
            allAnswered = AkmRules.allAnswered(questionIds, answers.keys),
            locked = submission?.state == SubmissionState.QUEUED || submission?.state == SubmissionState.SENT,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ResumeUiState())

    private val _submit = MutableStateFlow<SubmitUi>(SubmitUi.Idle)
    val submit: StateFlow<SubmitUi> = _submit.asStateFlow()

    /** The end time has passed: the answers were handed to WorkManager and the student is told so (single-action alert). */
    private val _timeUp = MutableStateFlow(false)
    val timeUp: StateFlow<Boolean> = _timeUp.asStateFlow()

    /** The "Dibatalkan" mark: the lobby shows a banner for it (a deviation from the legacy app, decided 2026-10-16). Visual only. */
    private val _watermark = MutableStateFlow(AkmRules.AkmWatermark.None)
    val watermark: StateFlow<AkmRules.AkmWatermark> = _watermark.asStateFlow()

    private val watcher = DeadlineWatcher(viewModelScope)

    init {
        viewModelScope.launch { _watermark.value = repository.watermarkOf(examId) }
        viewModelScope.launch {
            val exam = repository.syncedExam(examId)
            val endAt = exam?.endAtMillis ?: repository.endAtFor(examId, exam?.schedule)
            watcher.start(endAt) {
                if (!state.value.locked) repository.queueNow(examId)
                _timeUp.value = true
            }
        }
    }

    /** "Kumpulkan". Runs to completion even if the student leaves the screen ([AkmRepository.submit] is not cancellable). */
    fun submit() {
        if (_submit.value == SubmitUi.Processing || !state.value.allAnswered || state.value.locked) return
        _submit.value = SubmitUi.Processing
        viewModelScope.launch {
            _submit.value = when (val outcome = repository.submit(examId)) {
                SubmitOutcome.Sent, SubmitOutcome.Queued -> SubmitUi.Done
                is SubmitOutcome.Failed -> SubmitUi.Failed(outcome.message)
            }
        }
    }

    fun dismissSubmitResult() = _submit.update { SubmitUi.Idle }

    fun acknowledgeTimeUp() {
        _timeUp.value = false
    }

    override fun onCleared() {
        watcher.stop()
    }
}
