package id.diskola.app.viewmodel

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.dataclass.ResponData.AkmExplanationData
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
import id.diskola.app.dataclass.akm.AkmExam
import id.diskola.app.dataclass.akm.AkmItem
import id.diskola.app.dataclass.akm.AkmPairSlot
import id.diskola.app.dataclass.akm.AkmPlayerAnswer
import id.diskola.app.dataclass.akm.AkmQuestion
import id.diskola.app.dataclass.akm.AkmScoreItem
import id.diskola.app.dataclass.akm.AkmStatus
import id.diskola.app.dataclass.akm.AkmSubtestScore
import id.diskola.app.dataclass.akm.buildAnswerPayload
import id.diskola.app.dataclass.akm.toAkmExams
import id.diskola.app.dataclass.akm.toAkmItem
import id.diskola.app.dataclass.akm.toAkmQuestion
import id.diskola.app.dataclass.akm.toAkmScoreItem
import id.diskola.app.dataclass.akm.toSubtestScores
import id.diskola.app.dataclass.localDb.AkmDownloadStatus
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.DeviceUtil
import id.diskola.app.worker.AkmDownloadWorker
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Wired to [AsesmenApiService] per the docs under `docs/repo lama/asesmen/` — see that interface's own KDoc
 * for how confident each endpoint/field is. Anything genuinely local (which question is open,
 * which answers have been picked, whether this device has downloaded the question set) stays
 * client-side, matching the legacy app's own Room-cache architecture; everything else is real.
 *
 * Scope is school-authored assessments only (`exam-schedules*`) — the parallel `akm` (national AKM)
 * endpoint group documented in `docs/repo lama/api/04-pembayaran-klaspay-ppob-akm.md` was dropped
 * per explicit user decision, see the "AKM Pemerintah" question logged in
 * `docs/FLOW_QUESTIONS.md`.
 */
@HiltViewModel
class AkmViewModel @Inject constructor(
    private val asesmenApiService: AsesmenApiService,
    private val deviceUtil: DeviceUtil,
    private val akmSyncDao: AkmSyncDao,
    @ApplicationContext private val appContext: Context,
    val sessionStore: id.diskola.app.utils.session.SessionStore,
) : BaseViewModel() {

    private val _items = MutableStateFlow<List<AkmItem>>(emptyList())
    val items: StateFlow<List<AkmItem>> = _items.asStateFlow()

    private val _scores = MutableStateFlow<List<AkmScoreItem>>(emptyList())
    val scores: StateFlow<List<AkmScoreItem>> = _scores.asStateFlow()

    private val _statuses = MutableStateFlow<Map<Int, AkmStatus>>(emptyMap())
    val statuses: StateFlow<Map<Int, AkmStatus>> = _statuses.asStateFlow()

    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    /** From `exam_lock_mode` (`mobile/setting-akm`) — default true/strict until settings load. */
    private val _strictMode = MutableStateFlow(true)
    val strictMode: StateFlow<Boolean> = _strictMode.asStateFlow()

    private val _penaltyActive = MutableStateFlow(false)
    val penaltyActive: StateFlow<Boolean> = _penaltyActive.asStateFlow()

    private val _answers = MutableStateFlow<Map<Int, AkmPlayerAnswer>>(emptyMap())
    val answers: StateFlow<Map<Int, AkmPlayerAnswer>> = _answers.asStateFlow()

    private val _currentQuestions = MutableStateFlow<List<AkmQuestion>>(emptyList())
    val currentQuestions: StateFlow<List<AkmQuestion>> = _currentQuestions.asStateFlow()

    private val _questionIndex = MutableStateFlow(0)
    val questionIndex: StateFlow<Int> = _questionIndex.asStateFlow()

    private val rawSchedules = mutableMapOf<Int, AkmScheduleData>()
    private val lastSubmittedPayload = mutableMapOf<Int, List<Map<String, Any?>>>()

    /**
     * `rawSchedules` is a plain map, not Compose state — a screen that only reads [examsFor]/
     * [instructionsFor] as bare function calls has no reactive reason to recompose once
     * [observeSyncState] populates it asynchronously after the screen's first render. This counter
     * is bumped on every `rawSchedules` write; screens that depend on it (`AkmResumeScreen`,
     * `AkmQuestionScreen`) collect it via `collectAsStateWithLifecycle()` purely to force a
     * recomposition when fresh data lands — this was the real cause behind "soal tidak muncul,
     * cuma instruksi/perintah saja" happening intermittently: the exam-level question count came
     * from the schedule LIST endpoint (already loaded), so nothing else ever re-triggered
     * recomposition once sync finished a moment after the screen first opened.
     */
    private val _rawSchedulesVersion = MutableStateFlow(0)
    val rawSchedulesVersion: StateFlow<Int> = _rawSchedulesVersion.asStateFlow()
    private fun bumpRawSchedulesVersion() {
        _rawSchedulesVersion.value++
    }

    /**
     * `isRandom` shuffles are generated once and kept for the rest of this session — matches the
     * legacy app shuffling once at download time and persisting the order
     * (`docs/repo lama/asesmen/asesmen-teknis-download-storage.md` §2.4/§2.7). We have no local
     * Room store to persist it across app restarts, but within one attempt it must stay stable
     * (re-opening the same instruction must show questions/options in the same order every time),
     * and the SAME answer order must be reused when building the submit payload (see
     * `buildAnswerPayload`'s `answerOrderFor`) since both work off "index into what's displayed".
     */
    private val shuffledQuestionOrder = mutableMapOf<Int, List<Int>>()
    private val shuffledAnswerOrder = mutableMapOf<Int, List<Int>>()

    /** Guards against launching a duplicate Room collector per exam id from [observeSyncState]. */
    private val observedSyncIds = mutableSetOf<Int>()

    /** `(downloadProgress, totalQuestions)` per exam id, for the "Mengunduh soal… (x/y)" label —
     * populated by [observeSyncState]. A `StateFlow` (not a plain map) so the screen recomposes as
     * `AkmDownloadWorker` reports progress. */
    private val _syncProgress = MutableStateFlow<Map<Int, Pair<Int, Int>>>(emptyMap())
    val syncProgress: StateFlow<Map<Int, Pair<Int, Int>>> = _syncProgress.asStateFlow()

    /** Set once the deadline watcher auto-submits — the UI shows a one-time "waktu habis" alert
     * for this id, then calls [consumeTimeUpEvent]. */
    private val _timeUpEvent = MutableStateFlow<Int?>(null)
    val timeUpEvent: StateFlow<Int?> = _timeUpEvent.asStateFlow()

    private var deadlineJob: Job? = null
    private var watchedExamId: Int? = null

    init {
        fetchSchedule()
        fetchScores()
        fetchSettings()
    }

    fun fetchSchedule() {
        launchWithHandling {
            val school = runCatching { asesmenApiService.examSchedules().data }.getOrElse {
                Timber.e(it, "examSchedules failed")
                emptyList()
            }
            cacheSchedules(school)

            val seeded = school.map { it.id }.associateWith { id -> initialStatusFor(rawSchedules.getValue(id)) }
            _statuses.value = seeded + _statuses.value

            _items.value = school.map { it.toAkmItem() }.sortedBy { it.startAtMillis }
        }
    }

    fun fetchScores() {
        viewModelScope.launch {
            val school = runCatching { asesmenApiService.examSchedulesScored().data }.getOrElse {
                Timber.e(it, "examSchedulesScored failed")
                emptyList()
            }
            cacheSchedules(school)

            val seeded = school.map { it.id }.associateWith { id -> initialStatusFor(rawSchedules.getValue(id)) }
            _statuses.value = seeded + _statuses.value

            _scores.value = school.map { it.toAkmScoreItem(_statuses.value.getValue(it.id)) }
        }
    }

    fun fetchSettings() {
        viewModelScope.launch {
            runCatching { asesmenApiService.getAkmSettings().data }
                .onFailure { Timber.e(it, "getAkmSettings failed") }
                .getOrNull()
                ?.let { setting -> _strictMode.value = setting.exam_lock_mode ?: true }
        }
    }

    private fun cacheSchedules(schedules: List<AkmScheduleData>) {
        schedules.forEach { schedule -> rawSchedules[schedule.id] = schedule }
        bumpRawSchedulesVersion()
    }

    /**
     * The server's `status` string wins whenever it is present (only the scored endpoint sends it).
     * Verified against live data: `isDone` marks the *session window* as closed, NOT that the
     * student submitted — 42 of 54 live rows had `isDone=true` while `status` said the student
     * never took it, so the flags alone would mislabel those as "Menunggu penilaian".
     */
    private fun initialStatusFor(schedule: AkmScheduleData): AkmStatus = when {
        schedule.status.equals("COMPLETED", ignoreCase = true) -> AkmStatus.SudahDinilai
        schedule.status.equals("Sesi Terlewat", ignoreCase = true) -> AkmStatus.SesiTerlewat
        schedule.status.equals("Tidak Mengerjakan", ignoreCase = true) -> AkmStatus.TidakMengerjakan
        schedule.isAssessed -> AkmStatus.SudahDinilai
        schedule.isQueued -> AkmStatus.SudahDikumpulkan
        schedule.isDone -> AkmStatus.MenungguPenilaian
        else -> AkmStatus.BelumSinkron
    }

    fun statusOf(id: Int): AkmStatus = _statuses.value[id] ?: AkmStatus.BelumSinkron

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun triggerPenalty() {
        _penaltyActive.value = true
    }

    fun clearPenalty() {
        _penaltyActive.value = false
    }

    fun subtestScoresFor(id: Int): List<AkmSubtestScore> = rawSchedules[id]?.toSubtestScores().orEmpty()

    fun scoreItemFor(id: Int): AkmScoreItem? = _scores.value.find { it.id == id }

    private val _explanations = MutableStateFlow<List<AkmExplanationData>>(emptyList())
    val explanations: StateFlow<List<AkmExplanationData>> = _explanations.asStateFlow()

    private val _explanationLoading = MutableStateFlow(false)
    val explanationLoading: StateFlow<Boolean> = _explanationLoading.asStateFlow()

    private val _explanationError = MutableStateFlow<String?>(null)
    val explanationError: StateFlow<String?> = _explanationError.asStateFlow()

    /** One row per question (`docs/repo lama/asesmen/asesmen-teknis-nilai-pembahasan.md` §5.3) —
     * confirmed real shape, not a guess. `gov_schedule` is always 0, school-scope only. */
    fun fetchExplanation(id: Int) {
        _explanationLoading.value = true
        _explanationError.value = null
        _explanations.value = emptyList()
        viewModelScope.launch {
            try {
                _explanations.value = asesmenApiService.examExplanations(id).data
            } catch (e: Exception) {
                Timber.e(e)
                _explanationError.value = AppErrorHandler.getMessage(e)
            } finally {
                _explanationLoading.value = false
            }
        }
    }

    /**
     * Enqueues [AkmDownloadWorker] to download the question set (images/media to disk, everything
     * persisted to Room) instead of calling the API inline — a `WorkManager` job survives the
     * student leaving this screen or the app being backgrounded/killed mid-download, unlike the
     * previous plain-coroutine version. `KEEP` makes repeat taps idempotent (won't restart a job
     * already running), matching legacy `AkmViewModel.kt:510-537`.
     */
    fun syncQuestions(id: Int) {
        observeSyncState(id)
        val request = OneTimeWorkRequestBuilder<AkmDownloadWorker>()
            .setInputData(workDataOf("id" to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag("akm_downloader_$id")
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork("akm_downloader_$id", ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Hydrates [rawSchedules] and [_statuses] from Room's live sync state for [id] — call this
     * whenever a screen that needs this exam's data opens, not just from [syncQuestions], so a
     * process restart re-reads already-downloaded data instead of forcing a re-sync. A `null` row
     * (sync never triggered) is left alone — it must NOT downgrade a status already derived from the
     * schedule list (e.g. `SudahDinilai`) back to [AkmStatus.BelumSinkron].
     */
    fun observeSyncState(id: Int) {
        if (!observedSyncIds.add(id)) return
        viewModelScope.launch {
            akmSyncDao.observe(id).collect { synced ->
                if (synced == null) return@collect
                _syncProgress.value = _syncProgress.value + (id to (synced.downloadProgress to synced.totalQuestions))
                // Populate rawSchedules BEFORE flipping status to SiapDikerjakan — the "Mulai
                // Asesmen" button reacts to _statuses, so the data must already be there the
                // instant that becomes true, not a line later.
                if (synced.schedule != null) {
                    rawSchedules[id] = synced.schedule
                    bumpRawSchedulesVersion()
                }
                when (synced.downloadStatus) {
                    AkmDownloadStatus.DOWNLOADING -> setStatus(id, AkmStatus.MengunduhSoal)
                    AkmDownloadStatus.DOWNLOADED -> setStatus(id, AkmStatus.SiapDikerjakan)
                    AkmDownloadStatus.FAILED -> {
                        setStatus(id, AkmStatus.BelumSinkron)
                        if (synced.errorMessage.isNotBlank()) emitError(synced.errorMessage)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun checkPassword(id: Int, password: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val body = mapOf("password" to password)
                asesmenApiService.checkExamSchoolPassword(id, deviceUtil.getFingerprint(), body).data?.checked == true
            }
            onResult(result)
        }
    }

    fun submitExam(id: Int) {
        val instructions = instructionsFor(id)
        if (instructions.isEmpty()) {
            emitError("Data soal tidak ditemukan di perangkat ini.")
            return
        }
        // Confirmed by reading the legacy source directly (`AkmViewModel.kt:661`,
        // `akmDao.getStudentExamId(akmId)`) — the `{student_id}` path segment is the exam SESSION
        // id from the download response, not the logged-in account's own user id.
        val studentExamId = rawSchedules[id]?.studentExam?.id
        if (studentExamId == null || studentExamId <= 0) {
            emitError("Sesi ujian tidak ditemukan — coba sinkronisasi ulang soal.")
            return
        }
        if (watchedExamId == id) deadlineJob?.cancel()
        val payload = buildAnswerPayload(instructions, _answers.value) { questionId -> shuffledAnswerOrder[questionId] }
        lastSubmittedPayload[id] = payload

        launchWithHandling {
            asesmenApiService.submitExamSchoolAnswer(id, studentExamId, deviceUtil.getFingerprint(), request = payload)
            akmSyncDao.markSubmitted(id)
            setStatus(id, AkmStatus.SudahDikumpulkan)
        }
    }

    /**
     * Watches this assessment's own end time (`date`+`endAt`, the same deadline shown as
     * "Berakhir ..." on its card) and auto-submits whatever is answered once it passes — mirrors
     * the legacy app's "Waktu habis, Asesmen telah dikumpulkan" behavior
     * (`asesmen-akm-daftar-detail-nilai.md:196-199`), except triggered live while the student is
     * mid-exam rather than only detected when reopening the detail page.
     *
     * Safe to call repeatedly (e.g. from both the resume lobby and the question player) — a watch
     * already running for the same [id] is left alone; switching to a different exam restarts it.
     */
    fun watchDeadline(id: Int) {
        if (watchedExamId == id && deadlineJob?.isActive == true) return
        deadlineJob?.cancel()
        watchedExamId = id
        val deadline = _items.value.find { it.id == id }?.endAtMillis ?: return
        deadlineJob = viewModelScope.launch {
            delay((deadline - System.currentTimeMillis()).coerceAtLeast(0))
            if (statusOf(id) == AkmStatus.SiapDikerjakan) {
                submitExam(id)
                _timeUpEvent.value = id
            }
        }
    }

    fun consumeTimeUpEvent() {
        _timeUpEvent.value = null
    }

    /** Returns false when there's no cached payload to resend — the caller should show the
     * legacy "Data jawaban tidak ditemukan di perangkat ini" dialog in that case. */
    fun reuploadAnswer(id: Int): Boolean {
        val payload = lastSubmittedPayload[id] ?: return false
        val studentExamId = rawSchedules[id]?.studentExam?.id
        if (studentExamId == null || studentExamId <= 0) return false
        launchWithHandling {
            asesmenApiService.submitExamSchoolAnswer(id, studentExamId, deviceUtil.getFingerprint(), request = payload)
            akmSyncDao.markSubmitted(id)
        }
        return true
    }

    // --- Exam/instruction tree (AkmResumeScreen) ---

    private fun instructionsFor(id: Int): List<AkmInstructionData> {
        val schedule = rawSchedules[id] ?: return emptyList()
        return (schedule.exams ?: listOfNotNull(schedule.exam)).flatMap { it.instructions }
    }

    fun examsFor(id: Int): List<AkmExam> = rawSchedules[id]?.toAkmExams(_answers.value.keys).orEmpty()

    fun allAnswered(id: Int): Boolean {
        val instructions = instructionsFor(id)
        return instructions.isNotEmpty() && instructions.all { instr -> instr.questions.all { _answers.value.containsKey(it.id) } }
    }

    // --- Question player ---

    fun openInstruction(akmId: Int, instructionId: Int) {
        val instruction = instructionsFor(akmId).find { it.id == instructionId }
        val questions = instruction?.questions.orEmpty()
        val orderedQuestions = if (instruction?.isRandom == true) {
            val order = shuffledQuestionOrder.getOrPut(instructionId) { questions.map { it.id }.shuffled() }
            order.mapNotNull { qId -> questions.find { it.id == qId } }
        } else {
            questions
        }
        _currentQuestions.value = orderedQuestions.map { q ->
            val answerOrder = if (instruction?.isRandom == true) {
                shuffledAnswerOrder.getOrPut(q.id) { q.answers.map { it.id }.shuffled() }
            } else {
                null
            }
            q.toAkmQuestion(instructionId, answerOrder)
        }
        _questionIndex.value = 0
    }

    fun goToQuestion(index: Int) {
        _questionIndex.value = index.coerceIn(0, (_currentQuestions.value.size - 1).coerceAtLeast(0))
    }

    fun nextQuestion() = goToQuestion(_questionIndex.value + 1)

    fun previousQuestion() = goToQuestion(_questionIndex.value - 1)

    fun selectChoice(questionId: Int, index: Int) {
        _answers.value = _answers.value + (questionId to AkmPlayerAnswer.Choice(index))
    }

    fun toggleMultiChoice(questionId: Int, index: Int) {
        val current = (_answers.value[questionId] as? AkmPlayerAnswer.MultiChoice)?.indices.orEmpty()
        val next = if (index in current) current - index else current + index
        _answers.value = if (next.isEmpty()) {
            _answers.value - questionId
        } else {
            _answers.value + (questionId to AkmPlayerAnswer.MultiChoice(next))
        }
    }

    /**
     * Cleanup depends on the raw `answerType` (docs, `QuestionEssayVh.kt:96-114`): `SHORT_ESSAY_WORD`
     * strips whitespace as it is typed (single-word answer), `SHORT_ESSAY_NUM` keeps digits only,
     * and plain `ESSAY` is left untouched — it's free text and needs its spaces.
     */
    fun setEssay(questionId: Int, text: String) {
        val rawType = _currentQuestions.value.find { it.id == questionId }?.rawAnswerType
        val cleaned = when (rawType) {
            "SHORT_ESSAY_WORD" -> text.filterNot { it.isWhitespace() }
            "SHORT_ESSAY_NUM" -> text.filter { it.isDigit() }
            else -> text
        }
        _answers.value = if (cleaned.isEmpty()) {
            _answers.value - questionId
        } else {
            _answers.value + (questionId to AkmPlayerAnswer.Essay(cleaned))
        }
    }

    /** Tabel Pernyataan — one row toggled is enough to mark the whole question answered, matching
     * legacy `answerChoice()` (`AkmQuestionsPage.kt:855-874`), which calls `setAnswered()` on every
     * row interaction rather than waiting for all rows to be resolved. */
    fun selectTableRow(questionId: Int, rowId: Int, isTrue: Boolean) {
        val current = (_answers.value[questionId] as? AkmPlayerAnswer.TableAnswer)?.rows.orEmpty()
        _answers.value = _answers.value + (questionId to AkmPlayerAnswer.TableAnswer(current + (rowId to isTrue)))
    }

    /**
     * Menjodohkan — swaps the right-side content (`currentSecondText`/`currentSecondImageUrl`/
     * `currentSelectedId`) between two slots, mirroring the drag-drop swap in
     * `QuestionPairVh.kt:96-107` / `QuestionPairImageVh.kt:96-109` (only the right side moves; the
     * left-side anchor and row id never do). Snapshots ALL slots' current arrangement every call —
     * not just the swapped pair — so [buildAnswerPayload] never has to fall back to a stale default
     * for a row this question's `PairAnswer` hasn't "seen" yet.
     */
    fun swapPair(questionId: Int, slots: List<AkmPairSlot>, positionA: Int, positionB: Int) {
        if (positionA == positionB || positionA !in slots.indices || positionB !in slots.indices) return
        val a = slots[positionA]
        val b = slots[positionB]
        val swapped = slots.toMutableList().apply {
            this[positionA] = a.copy(currentSecondText = b.currentSecondText, currentSecondImageUrl = b.currentSecondImageUrl, currentSelectedId = b.currentSelectedId)
            this[positionB] = b.copy(currentSecondText = a.currentSecondText, currentSecondImageUrl = a.currentSecondImageUrl, currentSelectedId = a.currentSelectedId)
        }
        _pairArrangements[questionId] = swapped
        _answers.value = _answers.value + (questionId to AkmPlayerAnswer.PairAnswer(
            arrangement = swapped.associate { it.id to it.currentSelectedId },
            secondFilePaths = swapped.associate { it.id to it.currentSecondImageUrl },
        ))
    }

    /** Live arrangement per Menjodohkan question id, so re-rendering the question (e.g. after
     * `goToQuestion`/`previousQuestion`) shows the student's swaps instead of resetting to the
     * server's original order. Read by the screen via [pairSlotsFor]. */
    private val _pairArrangements = mutableMapOf<Int, List<AkmPairSlot>>()

    fun pairSlotsFor(question: AkmQuestion): List<AkmPairSlot> = _pairArrangements[question.id] ?: question.pairSlots

    fun isAnswered(questionId: Int): Boolean = _answers.value.containsKey(questionId)

    fun answeredCount(): Int = _currentQuestions.value.count { isAnswered(it.id) }

    private fun setStatus(id: Int, status: AkmStatus) {
        _statuses.value = _statuses.value + (id to status)
    }
}
