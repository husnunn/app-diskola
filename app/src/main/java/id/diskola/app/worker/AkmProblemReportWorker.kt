package id.diskola.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.diskola.app.repository.AkmProblemReportQueue
import id.diskola.app.repository.FlushResult
import id.diskola.app.utils.AkmViolationRules

/**
 * Sends the "Laporkan Masalah" reports still on the phone (doc `06` §13). Legacy kept an everlasting 15-minute job for this and never
 * cancelled it (§23.3 #20); here one unique job runs while something is unsent, retries with a linear back-off, and ends with the last
 * report. [AkmWork.enqueueProblemReportUpload] is called when a report is filed and again on app start if any is left.
 */
@HiltWorker
class AkmProblemReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val queue: AkmProblemReportQueue,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (queue.flush("worker")) {
        FlushResult.Success, FlushResult.NothingToSend -> Result.success()
        FlushResult.Skipped, FlushResult.PartialOrFailed ->
            if (runAttemptCount < AkmViolationRules.MAX_WORKER_ATTEMPTS) Result.retry() else Result.failure()
    }
}
