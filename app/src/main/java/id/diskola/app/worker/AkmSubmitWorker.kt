package id.diskola.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.diskola.app.dataclass.localDb.AkmSubmissionDao
import id.diskola.app.dataclass.localDb.AkmSubmissionEntity
import id.diskola.app.repository.AkmRepository
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.AkmRules.DeadlineAction
import id.diskola.app.utils.AkmRules.Failure
import id.diskola.app.utils.AkmRules.SubmissionState
import id.diskola.app.utils.session.SessionManager
import timber.log.Timber

private const val LOG_TAG = "AKM-SUBMIT"

/**
 * Sends a student's answers in the background (legacy `AkmUploader`, doc `06` §7.4): at the end of the exam, or at once for a
 * submission queued while offline. Everything it needs is in Room, so it works after the app was closed.
 *
 *  - It never starts from "nothing to do" states: a `SENT` or `REJECTED` exam is skipped (legacy re-posted an exam already uploaded).
 *  - A deadline run that wakes up early reschedules itself; one that cannot read the end time does nothing (no guessing "now").
 *  - The POST happens outside any transaction; the state moves `QUEUED -> SENT` only after the server accepted the answers.
 *  - A failure is classified by [AkmRules.classifyFailure]: transient ones retry up to [AkmRules.MAX_ATTEMPTS] with WorkManager's
 *    linear backoff, a 401 retries without using the attempts up, a final refusal ends in `REJECTED`.
 *  - After the last pending exam is sent, a logout that was held back (FCM "logout", a 401) is carried out.
 */
@HiltWorker
class AkmSubmitWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AkmRepository,
    private val submissionDao: AkmSubmissionDao,
    private val sessionManager: SessionManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val examId = inputData.getInt(AkmWork.KEY_ID, 0)
        val manual = inputData.getBoolean(AkmWork.KEY_MANUAL, false)
        if (examId < 1) return Result.success()

        val existing = submissionDao.get(examId)
        if (existing?.state == SubmissionState.SENT || existing?.state == SubmissionState.REJECTED) return Result.success()

        val exam = repository.syncedExam(examId)
        if (exam?.schedule == null || exam.studentExamId <= 0) return Result.success()

        val now = System.currentTimeMillis()
        when (val action = AkmRules.deadlineAction(now, exam.endAtMillis ?: repository.endAtFor(examId, exam.schedule), manual)) {
            DeadlineAction.Skip -> {
                Timber.tag(LOG_TAG).w("exam=%d: waktu berakhir tidak terbaca, tidak dikirim otomatis", examId)
                return Result.success()
            }
            is DeadlineAction.Reschedule -> {
                AkmWork.scheduleDeadline(applicationContext, examId, now + action.delayMillis, now, replace = true)
                return Result.success()
            }
            DeadlineAction.Submit -> Unit
        }

        if (existing == null) submissionDao.insertIfAbsent(newRow(examId, manual, now))
        submissionDao.requeue(examId, manual)
        val attempts = submissionDao.get(examId)?.attempts ?: 0

        val runningWork = if (tags.contains(AkmWork.deadlineName(examId))) AkmWork.deadlineName(examId) else AkmWork.submitName(examId)
        val failure = repository.post(exam)
        if (failure == null) {
            repository.markSent(examId, runningWork)
            Timber.tag(LOG_TAG).i("exam=%d: jawaban terkirim", examId)
            AkmNotifications.showUploadResult(applicationContext, examId, examName(exam.schedule.exams?.firstOrNull()?.name), success = true)
            sessionManager.runDeferredLogoutIfNoPendingExam()
            return Result.success()
        }
        return when (failure) {
            Failure.Retry, Failure.AuthExpired -> {
                val used = if (failure == Failure.Retry) attempts + 1 else attempts
                submissionDao.recordAttempt(examId, used, System.currentTimeMillis(), failure.toString())
                if (AkmRules.mayRetry(used)) {
                    Result.retry()
                } else {
                    // Out of attempts: the answers stay QUEUED (and keep logout blocked) for a manual re-upload from the score screen.
                    AkmNotifications.showUploadResult(applicationContext, examId, examName(exam.schedule.exams?.firstOrNull()?.name), success = false)
                    Result.success()
                }
            }
            is Failure.Permanent -> {
                submissionDao.markRejected(examId, failure.message, System.currentTimeMillis())
                AkmNotifications.showUploadResult(applicationContext, examId, examName(exam.schedule.exams?.firstOrNull()?.name), success = false)
                sessionManager.runDeferredLogoutIfNoPendingExam()
                Result.success()
            }
        }
    }

    private fun examName(name: String?): String = name.orEmpty()

    private fun newRow(examId: Int, manual: Boolean, now: Long) = AkmSubmissionEntity(
        examId = examId,
        state = SubmissionState.QUEUED,
        manual = manual,
        attempts = 0,
        queuedAt = now,
        lastAttemptAt = 0,
        sentAt = 0,
        errorMessage = "",
    )
}
