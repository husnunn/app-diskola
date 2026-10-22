package id.diskola.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.diskola.app.exam.ExamSessionController
import id.diskola.app.repository.AkmViolationQueue
import id.diskola.app.repository.FlushResult
import id.diskola.app.utils.AkmViolationRules
import timber.log.Timber

/**
 * The safety net under the foreground service (doc `06` §10.4, legacy `AkmViolationUploader`): every 15 minutes, with a connection,
 * send what the queue still holds. It also covers a session that was left with "Ya, keluar" — legacy stopped sending then, so the
 * record of leaving the exam sat on the phone until the next session. Here the job keeps going until the queue is empty and no session
 * is running, then removes itself.
 */
@HiltWorker
class AkmViolationUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val queue: AkmViolationQueue,
    private val session: ExamSessionController,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val examId = inputData.getInt(AkmWork.KEY_ID, 0)
        if (examId < 1) return Result.success()

        return when (queue.flush(examId, "worker")) {
            FlushResult.Success, FlushResult.NothingToSend -> {
                val idle = !session.isActiveFor(examId) && queue.pendingCount(examId) == 0
                if (idle) {
                    Timber.tag(TAG).i("exam=%d: antrean kosong dan sesi selesai, pekerja berhenti", examId)
                    AkmWork.cancelViolationUploader(applicationContext, examId)
                }
                Result.success()
            }
            FlushResult.Skipped, FlushResult.PartialOrFailed ->
                if (runAttemptCount < AkmViolationRules.MAX_WORKER_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val TAG = "AKM-VIOLATION"
    }
}
