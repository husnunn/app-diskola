package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.dataclass.ResponData.AkmExplanationData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
import id.diskola.app.dataclass.akm.AkmItem
import id.diskola.app.dataclass.akm.AkmScoreItem
import id.diskola.app.dataclass.akm.AkmStatus
import id.diskola.app.dataclass.akm.AkmSubtestScore
import id.diskola.app.dataclass.akm.toAkmItem
import id.diskola.app.dataclass.akm.toAkmScoreItem
import id.diskola.app.dataclass.akm.toSubtestScores
import id.diskola.app.dataclass.localDb.AkmDownloadStatus
import id.diskola.app.repository.AkmRepository
import id.diskola.app.repository.AkmSettingsRepository
import id.diskola.app.repository.SubmitOutcome
import id.diskola.app.utils.AkmExamState
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.AkmSessionRules
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.DeviceUtil
import id.diskola.app.utils.Utils
import id.diskola.app.utils.session.SessionStore
import java.util.Calendar
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** What a re-upload attempt tells the score screen to show (texts are legacy `AkmScorePage`, doc 06 §7.5). */
sealed interface ReuploadResult {
    data object Sent : ReuploadResult
    data class Cooldown(val seconds: Int) : ReuploadResult
    data object NoLocalData : ReuploadResult
    data class Failed(val message: String) : ReuploadResult
}

/**
 * The Asesmen list, the detail gate and the score screens (school exams only). Data comes from [AkmRepository] — the server list
 * with its offline copy, the packages and submissions in Room — so nothing here is lost when the screen or the process goes away.
 */
@HiltViewModel
class AkmListViewModel @Inject constructor(
    private val repository: AkmRepository,
    private val api: AsesmenApiService,
    private val deviceUtil: DeviceUtil,
    private val settings: AkmSettingsRepository,
    private val examState: AkmExamState,
    private val utils: Utils,
    val sessionStore: SessionStore,
) : BaseViewModel() {

    private val _schedules = MutableStateFlow<List<AkmScheduleData>>(emptyList())
    private val _scored = MutableStateFlow<List<AkmScheduleData>>(emptyList())

    private val _items = MutableStateFlow<List<AkmItem>>(emptyList())
    val items: StateFlow<List<AkmItem>> = _items.asStateFlow()

    /** What the server last said about each exam; the screens see it merged with what only this device knows ([statuses]). */
    private val serverStatuses = MutableStateFlow<Map<Int, AkmStatus>>(emptyMap())

    val statuses: StateFlow<Map<Int, AkmStatus>> = repository.statuses(serverStatuses)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** The Nilai tab: scored exams plus anything this device submitted that the scored list does not show yet. */
    val scores: StateFlow<List<AkmScoreItem>> = combine(_scored, _schedules, statuses) { scored, listed, status ->
        val scoredIds = scored.map { it.id }.toSet()
        val fromServer = scored.map { it.toAkmScoreItem(status[it.id] ?: serverStatusOf(it)) }
        val local = listed
            .filter { it.id !in scoredIds && status[it.id] in uploadStates }
            .map { it.toAkmScoreItem(status.getValue(it.id)) }
        local + fromServer
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    /** `(downloadProgress, totalQuestions)` per exam, for "Mengunduh soal… (x/y)". */
    val syncProgress: StateFlow<Map<Int, Pair<Int, Int>>> =
        repository.downloads().map { rows -> rows.associate { it.id to (it.downloadProgress to it.totalQuestions) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Set when the list could not be refreshed and the copy kept on this device is shown instead. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val announcedFailures = mutableSetOf<String>()
    private var inFlight = false

    init {
        refresh()
        viewModelScope.launch {
            // A failed download is reported once, with the reason, instead of silently flipping the button back to "Sinkronisasi Soal".
            repository.downloads().collect { rows ->
                rows.filter { it.downloadStatus == AkmDownloadStatus.FAILED && it.errorMessage.isNotBlank() }.forEach { row ->
                    if (announcedFailures.add("${row.id}:${row.errorMessage}")) emitError(row.errorMessage)
                }
            }
        }
    }

    /** One load of both lists; a second call while one is running does nothing. */
    fun refresh() {
        if (inFlight) return
        inFlight = true
        _loading.value = true
        viewModelScope.launch {
            try {
                coroutineScope {
                    val listed = async { repository.schedules() }
                    val scored = async { repository.scored() }
                    val list = listed.await()
                    val scoredLoad = scored.await()
                    _schedules.value = list.schedules
                    _scored.value = scoredLoad.schedules
                    _items.value = list.schedules.map { it.toAkmItem() }.sortedBy { it.startAtMillis ?: 0L }
                    serverStatuses.value = (list.schedules + scoredLoad.schedules).associate { it.id to serverStatusOf(it) }
                    _notice.value = list.error ?: scoredLoad.error
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _notice.value = AppErrorHandler.getMessage(e)
            } finally {
                inFlight = false
                _loading.value = false
            }
        }
    }

    private fun serverStatusOf(schedule: AkmScheduleData): AkmStatus =
        AkmRules.serverStatus(schedule.status, schedule.isAssessed, schedule.isQueued, schedule.isDone)

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun statusOf(id: Int): AkmStatus = statuses.value[id] ?: AkmStatus.BelumSinkron

    fun subtestScoresFor(id: Int): List<AkmSubtestScore> = (_scored.value + _schedules.value).firstOrNull { it.id == id }?.toSubtestScores().orEmpty()

    fun scoreItemFor(id: Int): AkmScoreItem? = scores.value.find { it.id == id }

    // ---- Download ----

    /** Hands the package download to WorkManager: it survives leaving this screen, backgrounding and a killed process. */
    fun syncQuestions(id: Int) = repository.enqueueDownload(id)

    // ---- Password gate ----

    /**
     * Whether "Mulai" must ask for the password: not when the exam has none, or when this student passed it and the last session was
     * closed properly (a session marker still set means the exam was left without handing in — the one-time password is asked again).
     */
    fun passwordRequired(item: AkmItem): Boolean = !AkmSessionRules.skipPassword(
        needsPassword = item.needsPassword,
        passwordChecked = examState.passwordChecked(item.id) ?: item.passwordChecked,
        markerActive = examState.isSessionActive(item.id),
    )

    fun checkPassword(id: Int, password: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                api.checkExamSchoolPassword(id, deviceUtil.getFingerprint(), mapOf("password" to password)).data?.checked == true
            }
            // The device remembers the pass at once, so the dialog does not come back before the next list refresh says so.
            if (result.getOrNull() == true) examState.setPasswordChecked(id, true)
            onResult(result)
        }
    }

    // ---- Terms gate (strict mode, doc 06 §5.3) ----

    /**
     * Right before the terms are shown the switches are fetched afresh (`force`, 4 s at most, an old value is kept when it fails), so the
     * text and the online check follow the server's current `exam_lock_mode`. [onReady] gets whether strict mode applies.
     */
    fun prepareTerms(onReady: (strict: Boolean) -> Unit) {
        viewModelScope.launch {
            settings.refresh(force = true)
            onReady(settings.isStrict())
        }
    }

    /** Real internet (a captive portal does not count), for the strict-mode start check. */
    fun isOnlineValidated(): Boolean = utils.isInternetValidated()

    // ---- Re-upload (Nilai tab) ----

    fun reupload(examId: Int, onResult: (ReuploadResult) -> Unit) {
        val left = repository.cooldownSecondsLeft(examId)
        if (left > 0) return onResult(ReuploadResult.Cooldown(left))
        viewModelScope.launch {
            if (!repository.hasStoredAnswers(examId)) return@launch onResult(ReuploadResult.NoLocalData)
            // Locked before the result is known, so a success and a failure both wait a minute (legacy `AkmScorePage`).
            repository.startCooldown(examId)
            onResult(
                when (val outcome = repository.reupload(examId)) {
                    SubmitOutcome.Sent, SubmitOutcome.Queued -> ReuploadResult.Sent
                    is SubmitOutcome.Failed -> if (outcome.message == AkmRepository.NO_LOCAL_DATA) ReuploadResult.NoLocalData else ReuploadResult.Failed(outcome.message)
                },
            )
            refresh()
        }
    }

    fun cooldownSecondsLeft(examId: Int): Int = repository.cooldownSecondsLeft(examId)

    // ---- Explanations (Nilai tab → Pembahasan) ----

    private val _explanations = MutableStateFlow<List<AkmExplanationData>>(emptyList())
    val explanations: StateFlow<List<AkmExplanationData>> = _explanations.asStateFlow()

    private val _explanationLoading = MutableStateFlow(false)
    val explanationLoading: StateFlow<Boolean> = _explanationLoading.asStateFlow()

    private val _explanationError = MutableStateFlow<String?>(null)
    val explanationError: StateFlow<String?> = _explanationError.asStateFlow()

    /** One row per question (`asesmen-teknis-nilai-pembahasan.md` §5.3). `gov_schedule` is always 0, school scope only. */
    fun fetchExplanation(id: Int) {
        _explanationLoading.value = true
        _explanationError.value = null
        _explanations.value = emptyList()
        viewModelScope.launch {
            try {
                _explanations.value = api.examExplanations(id).data
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _explanationError.value = AppErrorHandler.getMessage(e)
            } finally {
                _explanationLoading.value = false
            }
        }
    }

    private companion object {
        /** An upload waiting on, or accepted by, the server — shown on the Nilai tab even before the scored list has the exam. */
        val uploadStates = setOf(AkmStatus.ProsesUpload, AkmStatus.SudahDikumpulkan)
    }
}
