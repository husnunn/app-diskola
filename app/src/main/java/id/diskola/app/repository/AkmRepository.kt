package id.diskola.app.repository

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
import id.diskola.app.dataclass.ResponData.AkmSettingData
import id.diskola.app.dataclass.akm.AkmAnswerCodec
import id.diskola.app.dataclass.akm.AkmPayload
import id.diskola.app.dataclass.akm.AkmPlayerAnswer
import id.diskola.app.dataclass.akm.AkmStatus
import id.diskola.app.dataclass.localDb.AkmAnswerDao
import id.diskola.app.dataclass.localDb.AkmAnswerEntity
import id.diskola.app.dataclass.localDb.AkmDownloadState
import id.diskola.app.dataclass.localDb.AkmScheduleCacheDao
import id.diskola.app.dataclass.localDb.AkmScheduleCacheEntity
import id.diskola.app.dataclass.localDb.AkmSubmissionDao
import id.diskola.app.dataclass.localDb.AkmSubmissionEntity
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmSyncedExam
import id.diskola.app.utils.AkmExamState
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.AkmRules.Failure
import id.diskola.app.utils.AkmRules.SubmissionState
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.DeviceUtil
import id.diskola.app.utils.PreferenceClass
import id.diskola.app.utils.Utils
import id.diskola.app.worker.AkmWork
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** A schedule list: [error] is set when it could not be fetched and [schedules] are what was last stored on this device. */
data class ScheduleLoad(val schedules: List<AkmScheduleData>, val error: String? = null)

/** How a "Kumpulkan" / re-upload attempt ended. */
sealed interface SubmitOutcome {
    /** The server accepted the answers. */
    data object Sent : SubmitOutcome

    /** Saved on this device and handed to WorkManager: it goes out as soon as there is a connection. */
    data object Queued : SubmitOutcome

    data class Failed(val message: String) : SubmitOutcome
}

/**
 * The single door to exam data (doc `06`): the schedule lists with an offline copy, the downloaded package, the student's answers
 * (Room, written on every interaction) and the submission pipeline. The ViewModels only ever hold what this gives them, so a killed
 * process loses nothing — everything that matters is in Room.
 */
@Singleton
class AkmRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AsesmenApiService,
    private val syncDao: AkmSyncDao,
    private val answerDao: AkmAnswerDao,
    private val submissionDao: AkmSubmissionDao,
    private val cacheDao: AkmScheduleCacheDao,
    private val deviceUtil: DeviceUtil,
    private val preference: PreferenceClass,
    private val utils: Utils,
    private val examState: AkmExamState,
    private val violationQueue: AkmViolationQueue,
    moshi: Moshi,
) {
    private val scheduleAdapter = moshi.adapter(AkmScheduleData::class.java)
    private val codec = AkmAnswerCodec(moshi)

    /** Outlives any screen: a draft still being typed when the student leaves is written all the same. */
    private val detachedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- Schedule lists (online first, last copy when offline) ----

    /** The list, plus any exam that still has answers to send even if the list no longer (or not yet) mentions it. */
    suspend fun schedules(): ScheduleLoad {
        val loaded = load(AkmScheduleCacheEntity.KIND_LIST) { api.examSchedules().data }
        val listed = loaded.schedules.map { it.id }.toSet()
        val extras = syncDao.getAll()
            .filter { it.id !in listed && it.schedule != null && AkmRules.isPending(AkmRules.LocalExam(it.downloadStatus, it.studentExamId, submissionDao.get(it.id)?.state)) }
            .mapNotNull { it.schedule }
        return loaded.copy(schedules = loaded.schedules + extras)
    }

    suspend fun scored(): ScheduleLoad = load(AkmScheduleCacheEntity.KIND_SCORED) { api.examSchedulesScored().data }

    private suspend fun load(kind: String, fetch: suspend () -> List<AkmScheduleData>): ScheduleLoad {
        return try {
            val fresh = store(kind, fetch())
            reconcile(fresh)
            ScheduleLoad(fresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gagal memuat daftar asesmen ($kind)")
            ScheduleLoad(cached(kind), AppErrorHandler.getMessage(e))
        }
    }

    /**
     * Stores the list and returns it with one fix applied: a schedule that arrives without `watermark_bool` (e.g. from the scored
     * endpoint, or a server that does not send it) keeps the "Dibatalkan" mark already known on this device (doc 06 §4.4) — the mark
     * would otherwise vanish when an exam moves from the schedule list to the scores, or after a refresh.
     */
    private suspend fun store(kind: String, schedules: List<AkmScheduleData>): List<AkmScheduleData> {
        val known = knownWatermarks()
        val carried = schedules.map { schedule ->
            val remembered = known[schedule.id]
            if (schedule.watermark_bool == null && remembered != null) {
                schedule.copy(watermark_bool = remembered.enabled, watermark_label = remembered.label)
            } else {
                schedule
            }
        }
        val now = System.currentTimeMillis()
        cacheDao.replace(kind, carried.map { AkmScheduleCacheEntity(it.id, kind, scheduleAdapter.toJson(it), now) })
        return carried
    }

    /** The mark of every exam seen so far, from what the server last said (the schedule list first, then the scored list). */
    private suspend fun knownWatermarks(): Map<Int, AkmRules.AkmWatermark> {
        val result = mutableMapOf<Int, AkmRules.AkmWatermark>()
        listOf(AkmScheduleCacheEntity.KIND_SCORED, AkmScheduleCacheEntity.KIND_LIST).forEach { kind ->
            cached(kind).forEach { schedule ->
                if (schedule.watermark_bool != null) result[schedule.id] = AkmRules.watermark(schedule.watermark_bool, schedule.watermark_label, null)
            }
        }
        return result
    }

    /** The "Dibatalkan" mark of one exam (list cache, then scored cache, then its downloaded package); off when none is known. */
    suspend fun watermarkOf(examId: Int): AkmRules.AkmWatermark =
        knownWatermarks()[examId]
            ?: syncDao.get(examId)?.schedule?.let { AkmRules.watermark(it.watermark_bool, it.watermark_label, null) }
            ?: AkmRules.AkmWatermark.None

    private suspend fun cached(kind: String): List<AkmScheduleData> =
        cacheDao.getAll(kind).mapNotNull { runCatching { scheduleAdapter.fromJson(it.scheduleJson) }.getOrNull() }

    /**
     * The server says it already holds the answers (it queued, or even graded them) but this device still waits to send them — close
     * the local queue instead of sending a second time and keeping logout blocked for nothing.
     */
    private suspend fun reconcile(schedules: List<AkmScheduleData>) {
        val now = System.currentTimeMillis()
        schedules.forEach { schedule ->
            val status = AkmRules.serverStatus(schedule.status, schedule.isAssessed, schedule.isQueued, schedule.isDone)
            if (!AkmRules.serverAlreadyHasAnswers(status)) return@forEach
            if (syncDao.get(schedule.id) == null) return@forEach
            if (submissionDao.get(schedule.id)?.state == SubmissionState.SENT) return@forEach
            submissionDao.insertIfAbsent(newSubmission(schedule.id, manual = false, now = now, state = SubmissionState.SENT))
            submissionDao.markSent(schedule.id, now)
            examState.markHandedIn(schedule.id)
            AkmWork.cancelAll(context, schedule.id)
        }
    }

    suspend fun settings(): AkmSettingData? = try {
        api.getAkmSettings().data
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Gagal memuat setelan asesmen")
        null
    }

    // ---- Local state ----

    /** The downloaded package of one exam, or null while there is none. */
    fun examPackage(examId: Int): Flow<AkmSyncedExam?> = syncDao.observe(examId)

    /**
     * The stored exam. A row written before `studentExamId`/`endAtMillis` were columns has both only inside its package: they are
     * copied out here, once, so every caller (submit, the workers, the screens' deadline) sees the session of an already-downloaded exam.
     */
    suspend fun syncedExam(examId: Int): AkmSyncedExam? {
        val exam = syncDao.get(examId) ?: return null
        val schedule = exam.schedule
        val sessionId = schedule?.studentExam?.id ?: 0
        if (exam.studentExamId > 0 || sessionId <= 0) return exam
        val endAt = exam.endAtMillis ?: endAtFor(examId, schedule)
        syncDao.updateSession(examId, sessionId, endAt)
        return exam.copy(studentExamId = sessionId, endAtMillis = endAt)
    }

    /** Download progress/failures of every exam, without loading the packages. */
    fun downloads(): Flow<List<AkmDownloadState>> = syncDao.observeStates()

    /** Asks WorkManager to download the question package (idempotent: KEEP). */
    fun enqueueDownload(examId: Int) = AkmWork.enqueueDownload(context, examId)

    /** Every exam on this device with its submission, to merge into statuses. */
    fun localStates(): Flow<Map<Int, AkmRules.LocalExam>> =
        combine(syncDao.observeStates(), submissionDao.observeAll()) { exams, submissions ->
            val byExam = submissions.associateBy { it.examId }
            exams.associate { it.id to AkmRules.LocalExam(it.downloadStatus, it.studentExamId, byExam[it.id]?.state) }
        }

    fun submission(examId: Int): Flow<AkmSubmissionEntity?> = submissionDao.observe(examId)

    // ---- Answers ----

    fun answers(examId: Int): Flow<Map<Int, AkmPlayerAnswer>> = answerDao.observe(examId).map { rows ->
        rows.mapNotNull { row -> codec.decode(row.answerJson)?.let { row.questionId to it } }.toMap()
    }

    /** Written the moment the student interacts; an empty essay is stored too (legacy counts it as answered). */
    suspend fun saveAnswer(examId: Int, instructionId: Int, questionId: Int, typeLabel: String, answer: AkmPlayerAnswer) {
        if (isLocked(examId)) return
        answerDao.upsert(
            AkmAnswerEntity(
                examId = examId,
                questionId = questionId,
                instructionId = instructionId,
                typeLabel = typeLabel,
                answerJson = codec.encode(answer),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** [saveAnswer] for a ViewModel that is being cleared and can no longer launch work of its own. */
    fun saveAnswerDetached(examId: Int, instructionId: Int, questionId: Int, typeLabel: String, answer: AkmPlayerAnswer) {
        detachedScope.launch { saveAnswer(examId, instructionId, questionId, typeLabel, answer) }
    }

    suspend fun hasStoredAnswers(examId: Int): Boolean = answerDao.count(examId) > 0

    /** Once the answers are queued or sent the exam can no longer be edited (legacy: status FINISHED/UPLOADED closes the pages). */
    private suspend fun isLocked(examId: Int): Boolean {
        val state = submissionDao.get(examId)?.state
        return state == SubmissionState.QUEUED || state == SubmissionState.SENT
    }

    // ---- Submit ----

    /**
     * "Kumpulkan". Online it is sent right away; offline it is queued for WorkManager. The caller learns which one happened — and a
     * failed online attempt leaves the exam editable again (legacy rolled the transaction back), with the failure to show.
     */
    suspend fun submit(examId: Int): SubmitOutcome = withContext(NonCancellable) {
        val exam = syncedExam(examId)
        if (exam?.schedule == null || exam.studentExamId <= 0) {
            return@withContext SubmitOutcome.Failed("Sesi ujian tidak ditemukan — coba sinkronisasi ulang soal.")
        }
        val now = System.currentTimeMillis()
        submissionDao.insertIfAbsent(newSubmission(examId, manual = true, now = now, state = SubmissionState.QUEUED))
        submissionDao.requeue(examId, manual = true)
        if (submissionDao.get(examId)?.state == SubmissionState.SENT) {
            examState.markHandedIn(examId)
            return@withContext SubmitOutcome.Sent
        }

        if (!utils.isInternetAvailable()) {
            // Handed over to WorkManager: from here on the exam counts as handed in (the session marker is the "left without handing in" flag).
            examState.markHandedIn(examId)
            AkmWork.enqueueSubmit(context, examId, manual = true)
            return@withContext SubmitOutcome.Queued
        }
        when (val result = post(exam)) {
            null -> {
                markSent(examId)
                SubmitOutcome.Sent
            }
            is Failure.Permanent -> {
                submissionDao.markRejected(examId, result.message, System.currentTimeMillis())
                SubmitOutcome.Failed(result.message)
            }
            else -> {
                // A transient failure while online: give the student the choice ("Coba lagi") instead of a silent background retry.
                submissionDao.deleteQueued(examId)
                SubmitOutcome.Failed(FAILED_MESSAGE)
            }
        }
    }

    /**
     * The deadline passed while the student was in the exam, or the app asks for the answers to be sent on its own: queue them for
     * WorkManager, which sends as soon as it can. Not a student's choice, so [AkmRules.deadlineAction] is bypassed (`manual`).
     */
    suspend fun queueNow(examId: Int) {
        val exam = syncedExam(examId) ?: return
        if (exam.schedule == null || exam.studentExamId <= 0) return
        val now = System.currentTimeMillis()
        submissionDao.insertIfAbsent(newSubmission(examId, manual = false, now = now, state = SubmissionState.QUEUED))
        submissionDao.requeue(examId, manual = false)
        if (submissionDao.get(examId)?.state == SubmissionState.QUEUED) {
            examState.markHandedIn(examId)
            AkmWork.enqueueSubmit(context, examId, manual = true)
        }
    }

    /**
     * Sends the stored answers again (score screen). Does not touch the submission state on failure; a success keeps/sets `SENT`.
     * Offline is a failure here: legacy queued the job and still toasted "Jawaban berhasil dikirim".
     */
    suspend fun reupload(examId: Int): SubmitOutcome = withContext(NonCancellable) {
        val exam = syncedExam(examId)
        if (exam?.schedule == null || exam.studentExamId <= 0 || !hasStoredAnswers(examId)) return@withContext SubmitOutcome.Failed(NO_LOCAL_DATA)
        if (!utils.isInternetAvailable()) return@withContext SubmitOutcome.Failed(FAILED_MESSAGE)
        submissionDao.insertIfAbsent(newSubmission(examId, manual = true, now = System.currentTimeMillis(), state = SubmissionState.QUEUED))
        when (val result = post(exam)) {
            null -> {
                markSent(examId)
                SubmitOutcome.Sent
            }
            is Failure.Permanent -> SubmitOutcome.Failed(result.message)
            else -> SubmitOutcome.Failed(FAILED_MESSAGE)
        }
    }

    /** The one place that talks to `…/answer/{student_exam_id}`: null on success, otherwise how it failed. */
    suspend fun post(exam: AkmSyncedExam): Failure? {
        val schedule = exam.schedule ?: return Failure.Permanent("Data soal tidak ditemukan di perangkat ini.")
        val instructions = (schedule.exams ?: listOfNotNull(schedule.exam)).flatMap { it.instructions }
        val answers = answerDao.getAll(exam.id).mapNotNull { row -> codec.decode(row.answerJson)?.let { row.questionId to it } }.toMap()
        val payload = AkmPayload.build(instructions, answers)
        return try {
            api.submitExamSchoolAnswer(exam.id, exam.studentExamId, deviceUtil.getFingerprint(), request = payload).close()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gagal mengirim jawaban ujian ${exam.id}")
            AkmRules.classifyFailure(e)
        }
    }

    /**
     * The server accepted the answers. [runningWork] is the unique work calling this from inside a worker: it must not cancel itself
     * (that would cancel the coroutine that is still finishing), so only the exam's *other* jobs are cancelled.
     */
    suspend fun markSent(examId: Int, runningWork: String? = null) {
        val now = System.currentTimeMillis()
        submissionDao.insertIfAbsent(newSubmission(examId, manual = false, now = now, state = SubmissionState.SENT))
        submissionDao.markSent(examId, now)
        examState.markHandedIn(examId)
        AkmWork.cancelExcept(context, examId, runningWork)
        // What the session recorded goes to the school together with the answers (legacy flushed after a submit): awaited inside a
        // worker, which has the time, and in the background for a student who is waiting on "Berhasil".
        if (runningWork != null) {
            violationQueue.flush(examId, "kirim-jawaban-worker")
        } else {
            detachedScope.launch { violationQueue.flush(examId, "kirim-jawaban") }
        }
    }

    // ---- Re-upload cooldown (60 s, per exam, set before the result so success and failure both lock it) ----

    fun cooldownSecondsLeft(examId: Int): Int = AkmRules.cooldownSecondsLeft(System.currentTimeMillis(), preference.getLong(cooldownKey(examId), 0L))

    fun startCooldown(examId: Int) {
        preference.putLong(cooldownKey(examId), System.currentTimeMillis() + AkmRules.REUPLOAD_COOLDOWN_MS)
    }

    private fun cooldownKey(examId: Int) = "akm_upload_cooldown_$examId"

    // ---- Logout block and scheduling ----

    /** Whether any exam's answers may still be on their way — the single predicate behind every logout path (doc 06 §15). */
    suspend fun hasPending(): Boolean {
        repairSessions()
        return syncDao.countPending() > 0
    }

    /**
     * Downloaded rows from before `studentExamId`/`endAtMillis` were columns keep both only inside the stored package; copy them out
     * once so the pending query and the deadline scheduling see them.
     */
    suspend fun repairSessions() {
        syncDao.getDownloadedWithoutSession().forEach { exam ->
            val schedule = exam.schedule ?: return@forEach
            val sessionId = schedule.studentExam?.id ?: 0
            if (sessionId > 0) syncDao.updateSession(exam.id, sessionId, endAtFor(exam.id, schedule))
        }
    }

    /** (Re)schedules the deadline job of every exam that still has answers to send; KEEP makes this safe to repeat. */
    suspend fun scheduleDeadlines() {
        repairSessions()
        syncDao.pendingDeadlines().forEach { pending ->
            val endAt = pending.endAtMillis ?: return@forEach
            AkmWork.scheduleDeadline(context, pending.id, endAt)
        }
    }

    /** `date`+`endAt` of the package, else of the last list the server sent for this exam. */
    suspend fun endAtFor(examId: Int, schedule: AkmScheduleData?): Long? {
        schedule?.let { AkmRules.parseDateTime(it.date, it.endAt)?.let { millis -> return millis } }
        val listed = cached(AkmScheduleCacheEntity.KIND_LIST).firstOrNull { it.id == examId }
            ?: cached(AkmScheduleCacheEntity.KIND_SCORED).firstOrNull { it.id == examId }
        return listed?.let { AkmRules.parseDateTime(it.date, it.endAt) }
    }

    /** Questions of the downloaded package, instruction by instruction. */
    fun instructionsOf(schedule: AkmScheduleData?): List<AkmInstructionData> =
        schedule?.let { (it.exams ?: listOfNotNull(it.exam)).flatMap { exam -> exam.instructions } }.orEmpty()

    /** The effective status of every exam: what the server last said, upgraded by what only this device knows. */
    fun statuses(serverStatuses: Flow<Map<Int, AkmStatus>>): Flow<Map<Int, AkmStatus>> =
        combine(serverStatuses, localStates()) { server, local ->
            (server.keys + local.keys).associateWith { id -> AkmRules.effectiveStatus(server[id] ?: AkmStatus.BelumSinkron, local[id]) }
        }

    private fun newSubmission(examId: Int, manual: Boolean, now: Long, state: String) = AkmSubmissionEntity(
        examId = examId,
        state = state,
        manual = manual,
        attempts = 0,
        queuedAt = now,
        lastAttemptAt = 0,
        sentAt = 0,
        errorMessage = "",
    )

    companion object {
        const val FAILED_MESSAGE = "Terdapat kesalahan dalam pengunggahan jawaban"
        const val NO_LOCAL_DATA = "Data jawaban tidak ditemukan di perangkat ini. anda tidak bisa melakukan upload ulang jawaban !!"
    }
}
