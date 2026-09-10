package id.app.education.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.app.education.dataclass.mock.AkmItem
import id.app.education.dataclass.mock.AkmStatus
import id.app.education.dataclass.mock.MockAkm
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** An answer to one question — the shape depends on the question type. */
sealed interface AkmAnswer {
    data class Choice(val index: Int) : AkmAnswer
    data class MultiChoice(val indices: Set<Int>) : AkmAnswer
    data class Essay(val text: String) : AkmAnswer
}

/**
 * UI-only for now — Asesmen/AKM has no backend yet (see `ref/fase3`). Questions, schedules and
 * scores come from [MockAkm]; swap these StateFlow sources for repository calls once the
 * assessment API exists.
 */
@HiltViewModel
class AkmViewModel @Inject constructor() : ViewModel() {

    val items = MockAkm.items
    val scores = MockAkm.scores
    val exams = MockAkm.exams
    val questions = MockAkm.questions

    private val _statuses = MutableStateFlow(MockAkm.initialStatus)
    val statuses: StateFlow<Map<Int, AkmStatus>> = _statuses.asStateFlow()

    private val _selectedDay = MutableStateFlow(MockAkm.TODAY)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    /** Strict mode locks the screen during the exam and requires being online to start. */
    private val _strictMode = MutableStateFlow(true)
    val strictMode: StateFlow<Boolean> = _strictMode.asStateFlow()

    /** Set when the student is caught leaving the app; freezes the questions until it clears. */
    private val _penaltyActive = MutableStateFlow(false)
    val penaltyActive: StateFlow<Boolean> = _penaltyActive.asStateFlow()

    /** Questions downloaded so far for the assessment currently syncing. */
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _answers = MutableStateFlow<Map<Int, AkmAnswer>>(emptyMap())
    val answers: StateFlow<Map<Int, AkmAnswer>> = _answers.asStateFlow()

    private val _questionIndex = MutableStateFlow(0)
    val questionIndex: StateFlow<Int> = _questionIndex.asStateFlow()

    private var syncJob: Job? = null

    fun scheduleFor(day: Int): List<AkmItem> = items.filter { it.endDay == day }

    fun statusOf(id: Int): AkmStatus = _statuses.value[id] ?: AkmStatus.BelumSinkron

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun toggleStrictMode() {
        _strictMode.value = !_strictMode.value
    }

    fun triggerPenalty() {
        _penaltyActive.value = true
    }

    fun clearPenalty() {
        _penaltyActive.value = false
    }

    /** Mock download — steps to the question count over roughly two seconds, then marks ready. */
    fun syncQuestions(id: Int) {
        val item = items.find { it.id == id } ?: return
        if (syncJob?.isActive == true) return

        setStatus(id, AkmStatus.MengunduhSoal)
        _downloadProgress.value = 0
        val step = (item.questionCount + 7) / 8

        syncJob = viewModelScope.launch {
            while (_downloadProgress.value < item.questionCount) {
                delay(260)
                _downloadProgress.value = (_downloadProgress.value + step).coerceAtMost(item.questionCount)
            }
            setStatus(id, AkmStatus.SiapDikerjakan)
        }
    }

    fun submitExam(id: Int) {
        setStatus(id, AkmStatus.SudahDikumpulkan)
    }

    private fun setStatus(id: Int, status: AkmStatus) {
        _statuses.value = _statuses.value + (id to status)
    }

    // --- Question player ---

    fun goToQuestion(index: Int) {
        _questionIndex.value = index.coerceIn(0, questions.lastIndex)
    }

    fun nextQuestion() = goToQuestion(_questionIndex.value + 1)

    fun previousQuestion() = goToQuestion(_questionIndex.value - 1)

    fun selectChoice(questionId: Int, index: Int) {
        _answers.value = _answers.value + (questionId to AkmAnswer.Choice(index))
    }

    fun toggleMultiChoice(questionId: Int, index: Int) {
        val current = (_answers.value[questionId] as? AkmAnswer.MultiChoice)?.indices.orEmpty()
        val next = if (index in current) current - index else current + index
        _answers.value = if (next.isEmpty()) {
            _answers.value - questionId
        } else {
            _answers.value + (questionId to AkmAnswer.MultiChoice(next))
        }
    }

    /** Short-essay answers are a single word — whitespace is stripped as it is typed. */
    fun setEssay(questionId: Int, text: String) {
        val cleaned = text.filterNot { it.isWhitespace() }
        _answers.value = if (cleaned.isEmpty()) {
            _answers.value - questionId
        } else {
            _answers.value + (questionId to AkmAnswer.Essay(cleaned))
        }
    }

    fun isAnswered(questionId: Int): Boolean = _answers.value.containsKey(questionId)

    fun answeredCount(): Int = questions.count { isAnswered(it.id) }

    /** Live instruction progress — the one instruction backed by the real question player. */
    fun instructionAnswered(instructionId: Int): Int {
        val instruction = exams.flatMap { it.instructions }.find { it.id == instructionId }
        return instruction?.answered ?: answeredCount()
    }

    fun resetPlayer() {
        _questionIndex.value = 0
    }
}
