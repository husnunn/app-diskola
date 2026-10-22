package id.diskola.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import id.diskola.app.utils.AkmRules
import java.util.concurrent.TimeUnit

/**
 * Every WorkManager job of the exam module, by name. Two unique works per exam so neither can clobber the other (legacy used one
 * name with REPLACE, which cancelled a running manual upload whenever a list fetch re-scheduled the deadline):
 *  - `akm_submit_{id}` — "send my answers now" (manual submit while offline, time-up, retries),
 *  - `akm_deadline_{id}` — "send whatever is answered when the exam ends".
 * Both use KEEP: scheduling again while one exists is a no-op.
 */
object AkmWork {
    const val KEY_ID = "id"
    const val KEY_MANUAL = "manual"
    const val PROBLEM_REPORT_WORK = "akm_problem_report_uploader"
    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun submitName(examId: Int) = "akm_submit_$examId"
    fun deadlineName(examId: Int) = "akm_deadline_$examId"
    fun downloadName(examId: Int) = "akm_downloader_$examId"

    /** Same name as legacy (`exam_violation_uploader_{id}`). */
    fun violationUploaderName(examId: Int) = "exam_violation_uploader_$examId"

    fun enqueueDownload(context: Context, examId: Int) {
        val request = OneTimeWorkRequestBuilder<AkmDownloadWorker>()
            .setInputData(workDataOf(KEY_ID to examId))
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag(downloadName(examId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(downloadName(examId), ExistingWorkPolicy.KEEP, request)
    }

    fun enqueueSubmit(context: Context, examId: Int, manual: Boolean) {
        val request = OneTimeWorkRequestBuilder<AkmSubmitWorker>()
            .setInputData(workDataOf(KEY_ID to examId, KEY_MANUAL to manual))
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.LINEAR, AkmRules.BACKOFF_STEP_MS, TimeUnit.MILLISECONDS)
            .addTag(submitName(examId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(submitName(examId), ExistingWorkPolicy.KEEP, request)
    }

    /** Fires at [endAtMillis]; an end time in the past runs at once. [replace] is for the deadline worker rescheduling itself. */
    fun scheduleDeadline(context: Context, examId: Int, endAtMillis: Long, nowMillis: Long = System.currentTimeMillis(), replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<AkmSubmitWorker>()
            .setInputData(workDataOf(KEY_ID to examId, KEY_MANUAL to false))
            .setInitialDelay((endAtMillis - nowMillis).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.LINEAR, AkmRules.BACKOFF_STEP_MS, TimeUnit.MILLISECONDS)
            .addTag(deadlineName(examId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            deadlineName(examId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Every 15 minutes, with a connection, send the exam's recorded violations. KEEP: starting a second session adds nothing. The job
     * ends itself once the queue is empty and no session runs ([AkmViolationUploadWorker]).
     */
    fun enqueueViolationUploader(context: Context, examId: Int) {
        val request = PeriodicWorkRequestBuilder<AkmViolationUploadWorker>(15, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_ID to examId))
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag(violationUploaderName(examId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(violationUploaderName(examId), ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** One unique, global job for every unsent problem report (not per exam): KEEP, so filing a second report adds nothing. */
    fun enqueueProblemReportUpload(context: Context) {
        val request = OneTimeWorkRequestBuilder<AkmProblemReportWorker>()
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag(PROBLEM_REPORT_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(PROBLEM_REPORT_WORK, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelViolationUploader(context: Context, examId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(violationUploaderName(examId))
    }

    /** The answers reached the server: no pending job of this exam is needed any more. */
    fun cancelAll(context: Context, examId: Int) = cancelExcept(context, examId, keepRunning = null)

    /**
     * Cancels the exam's jobs other than the one calling this: a worker must not cancel its own unique work (that would cancel
     * the coroutine that is still finishing the upload).
     */
    fun cancelExcept(context: Context, examId: Int, keepRunning: String?) {
        val manager = WorkManager.getInstance(context)
        listOf(submitName(examId), deadlineName(examId)).filter { it != keepRunning }.forEach(manager::cancelUniqueWork)
    }
}
