package id.diskola.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.akm.AkmPairSlot
import id.diskola.app.dataclass.akm.AkmPlayerAnswer
import id.diskola.app.dataclass.akm.AkmQuestion
import id.diskola.app.dataclass.akm.arranged
import id.diskola.app.dataclass.akm.swapped
import id.diskola.app.dataclass.akm.toAkmQuestion
import id.diskola.app.dataclass.akm.toArrangement
import id.diskola.app.repository.AkmRepository
import id.diskola.app.ui.navigation.Route
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.AkmRules.SubmissionState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionUiState(
    val loaded: Boolean = false,
    val questions: List<AkmQuestion> = emptyList(),
    /** The answers are queued or sent: the question screen is read-only. */
    val locked: Boolean = false,
)

/**
 * The question player for one instruction. Every interaction is written to Room straight away (essay text after a 200 ms pause, and
 * on leaving), so nothing the student answered is lost to a killed process. The question order is the one stored with the package.
 */
@HiltViewModel
class AkmQuestionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: AkmRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Akm.Question>()
    private val examId = route.id
    private val instructionId = route.instructionId

    /** Text typed but not yet in Room; shown immediately so the field never lags behind the write. */
    private val essayDrafts = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val essayJobs = mutableMapOf<Int, Job>()
    private val essayTypes = mutableMapOf<Int, String>()

    private val stored = repository.answers(examId)

    val answers: StateFlow<Map<Int, AkmPlayerAnswer>> = combine(stored, essayDrafts) { room, drafts ->
        room + drafts.mapValues { AkmPlayerAnswer.Essay(it.value) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val state: StateFlow<QuestionUiState> = combine(repository.examPackage(examId), repository.submission(examId)) { exam, submission ->
        val schedule = exam?.schedule ?: return@combine QuestionUiState(loaded = exam != null)
        val instruction = repository.instructionsOf(schedule).firstOrNull { it.id == instructionId }
        QuestionUiState(
            loaded = true,
            questions = instruction?.questions.orEmpty().map { it.toAkmQuestion(instructionId) },
            locked = submission?.state == SubmissionState.QUEUED || submission?.state == SubmissionState.SENT,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, QuestionUiState())

    /** Survives rotation and process death: the page the student was on. */
    val index: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_INDEX, (route.number - 1).coerceAtLeast(0))

    /** The end time passed while a question was open: the answers are queued and the student is told once. */
    private val _timeUp = MutableStateFlow(false)
    val timeUp: StateFlow<Boolean> = _timeUp.asStateFlow()

    /** The "Dibatalkan" mark of this exam: drawn as a diagonal watermark over the question page, nothing more (doc 06 §4.4). */
    private val _watermark = MutableStateFlow(AkmRules.AkmWatermark.None)
    val watermark: StateFlow<AkmRules.AkmWatermark> = _watermark.asStateFlow()

    private val watcher = DeadlineWatcher(viewModelScope)

    init {
        viewModelScope.launch { _watermark.value = repository.watermarkOf(examId) }
        viewModelScope.launch {
            val exam = repository.syncedExam(examId)
            val endAt = exam?.endAtMillis ?: repository.endAtFor(examId, exam?.schedule)
            watcher.start(endAt) {
                flush()
                if (!state.value.locked) repository.queueNow(examId)
                _timeUp.value = true
            }
        }
    }

    fun goToQuestion(target: Int) {
        val last = (state.value.questions.size - 1).coerceAtLeast(0)
        savedStateHandle[KEY_INDEX] = target.coerceIn(0, last)
    }

    fun nextQuestion() = goToQuestion(index.value + 1)

    fun previousQuestion() = goToQuestion(index.value - 1)

    // ---- Answering ----

    fun selectChoice(question: AkmQuestion, answerId: Int) = save(question, AkmPlayerAnswer.Choice(answerId))

    fun selectTrueFalse(question: AkmQuestion, isTrue: Boolean) = save(question, AkmPlayerAnswer.TrueFalse(isTrue))

    /** Legacy never un-answers a question by clearing its last tick: the answer stays, with nothing selected. */
    fun toggleMultiChoice(question: AkmQuestion, answerId: Int) {
        val current = (answers.value[question.id] as? AkmPlayerAnswer.MultiChoice)?.answerIds.orEmpty()
        save(question, AkmPlayerAnswer.MultiChoice(if (answerId in current) current - answerId else current + answerId))
    }

    fun selectTableRow(question: AkmQuestion, rowId: Int, isTrue: Boolean) {
        val current = (answers.value[question.id] as? AkmPlayerAnswer.TableAnswer)?.rows.orEmpty()
        save(question, AkmPlayerAnswer.TableAnswer(current + (rowId to isTrue)))
    }

    /** Swaps two right-hand items and stores the arrangement of **every** row. */
    fun swapPair(question: AkmQuestion, positionA: Int, positionB: Int) {
        val swapped = pairSlotsFor(question).swapped(positionA, positionB)
        save(question, AkmPlayerAnswer.PairAnswer(swapped.toArrangement()))
    }

    fun pairSlotsFor(question: AkmQuestion): List<AkmPairSlot> =
        question.pairSlots.arranged((answers.value[question.id] as? AkmPlayerAnswer.PairAnswer)?.arrangement)

    /**
     * Essay text: line breaks removed (a typed Enter is dropped, a pasted block is flattened to one line — `noNewLineFilter`, doc 06 §5.7),
     * then cleaned by type, shown at once, written to Room after 200 ms without typing.
     */
    fun setEssay(question: AkmQuestion, text: String) {
        if (state.value.locked) return
        val previous = (answers.value[question.id] as? AkmPlayerAnswer.Essay)?.text.orEmpty()
        val cleaned = AkmRules.cleanEssay(question.rawAnswerType, AkmRules.sanitizeEssayInput(previous, text))
        essayTypes[question.id] = question.rawAnswerType
        essayDrafts.update { it + (question.id to cleaned) }
        essayJobs[question.id]?.cancel()
        essayJobs[question.id] = viewModelScope.launch {
            delay(ESSAY_DEBOUNCE_MS)
            persistEssay(question.id)
        }
    }

    /** Writes every essay that is still waiting for its pause (leaving the screen, the app going to the background). */
    fun flush() {
        essayJobs.values.forEach { it.cancel() }
        essayJobs.clear()
        essayDrafts.value.keys.toList().forEach { questionId -> viewModelScope.launch { persistEssay(questionId) } }
    }

    private suspend fun persistEssay(questionId: Int) {
        val text = essayDrafts.value[questionId] ?: return
        repository.saveAnswer(examId, instructionId, questionId, essayTypes[questionId].orEmpty(), AkmPlayerAnswer.Essay(text))
        essayDrafts.update { drafts -> if (drafts[questionId] == text) drafts - questionId else drafts }
    }

    private fun save(question: AkmQuestion, answer: AkmPlayerAnswer) {
        if (state.value.locked) return
        viewModelScope.launch { repository.saveAnswer(examId, instructionId, question.id, question.rawAnswerType, answer) }
    }

    override fun onCleared() {
        watcher.stop()
        // viewModelScope is already cancelled here: hand what is still unwritten to the repository's own scope.
        essayDrafts.value.forEach { (questionId, text) ->
            repository.saveAnswerDetached(examId, instructionId, questionId, essayTypes[questionId].orEmpty(), AkmPlayerAnswer.Essay(text))
        }
    }

    private companion object {
        const val KEY_INDEX = "index"
        const val ESSAY_DEBOUNCE_MS = 200L
    }
}
