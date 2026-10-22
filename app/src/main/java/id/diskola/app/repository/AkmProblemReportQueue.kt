package id.diskola.app.repository

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.BuildConfig
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.dataclass.localDb.AkmProblemReportDao
import id.diskola.app.dataclass.localDb.AkmProblemReportEntity
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmViolationDao
import id.diskola.app.utils.AkmExamState
import id.diskola.app.utils.AkmReportRules
import id.diskola.app.utils.AkmViolationRules
import id.diskola.app.utils.AkmViolationRules.ChunkDecision
import id.diskola.app.utils.Utils
import id.diskola.app.worker.AkmWork
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * The "Laporkan Masalah" reports (doc `06` §13): [file] stores one at once (with the violations of the session at that moment and the
 * device facts), [flush] sends what is unsent — all exams together, one global pause (legacy `AkmProblemReportQueue`).
 *
 * Sending follows [AkmViolationRules.classify]: a 400 closes the row (the server will never take it; legacy `is_sent = 1`), 429/403/5xx
 * pause every report for the stated time, anything else leaves the row for the next try. Only [AkmReportRules.payload]'s four fields are
 * sent; the student's text, the checkbox and the violation list are kept in the row only.
 */
@Singleton
class AkmProblemReportQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AkmProblemReportDao,
    private val violationDao: AkmViolationDao,
    private val syncDao: AkmSyncDao,
    private val api: AsesmenApiService,
    private val utils: Utils,
    private val examState: AkmExamState,
    private val moshi: Moshi,
) {
    private val mutex = Mutex()
    private var pausedUntilElapsed = 0L

    private fun elapsed() = SystemClock.elapsedRealtime()

    /** Whole seconds until the next report may be filed (0 = now). */
    fun cooldownSecondsLeft(): Int = AkmReportRules.cooldownSecondsLeft(examState.problemReportLastSubmitAt(), System.currentTimeMillis())

    /**
     * Stores the report and starts the cooldown. Never throws into the caller: the student is told it was saved either way (legacy
     * toasted the same text "always, including when saving failed" — here a failure is at least logged).
     */
    suspend fun file(examId: Int, message: String, confirmedSystemFault: Boolean) {
        val now = System.currentTimeMillis()
        examState.setProblemReportLastSubmitAt(now)
        try {
            val studentExamId = syncDao.get(examId)?.studentExamId ?: 0
            val violations = violationDao.pending(examId)
            val lines = violations.map { row ->
                AkmReportRules.ViolationLine(
                    detectedAtIso = AkmViolationRules.isoDateTime(row.detectedAtMillis),
                    type = row.type,
                    description = if (row.metadataJson?.contains(AkmViolationRules.NOT_SUBMITTED_REASON) == true) {
                        AkmViolationRules.NOT_SUBMITTED_DESCRIPTION
                    } else {
                        AkmViolationRules.description(row.type)
                    },
                    metadataJson = row.metadataJson,
                )
            }
            val appVersion = BuildConfig.VERSION_NAME
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val listType = Types.newParameterizedType(List::class.java, Map::class.java)
            val violationsJson = moshi.adapter<List<Map<String, Any?>>>(listType).toJson(
                violations.map { AkmViolationRules.payloadItem(it.type, it.detectedAtMillis, AkmViolationRules.parseMetadata(it.metadataJson, moshi)) },
            )
            dao.insert(
                AkmProblemReportEntity(
                    examId = examId,
                    studentExamId = studentExamId,
                    message = AkmReportRules.clampMessage(message),
                    violationsJson = violationsJson,
                    isSystemFaultConfirmed = confirmedSystemFault,
                    appVersion = appVersion,
                    androidVersion = androidVersion,
                    deviceName = deviceName,
                    logcat = AkmReportRules.buildLog(
                        examId = examId,
                        studentExamId = studentExamId,
                        nowIso = AkmViolationRules.isoDateTime(now),
                        deviceName = deviceName,
                        androidVersion = androidVersion,
                        appVersion = appVersion,
                        violations = lines,
                    ),
                    createdAt = now,
                ),
            )
            AkmWork.enqueueProblemReportUpload(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Gagal menyimpan laporan masalah")
        }
    }

    suspend fun flush(reason: String): FlushResult = mutex.withLock {
        if (!utils.isInternetAvailable()) return@withLock FlushResult.Skipped
        if (elapsed() < pausedUntilElapsed) return@withLock FlushResult.Skipped
        val rows = try {
            dao.unsent()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Gagal membaca antrean laporan")
            return@withLock FlushResult.Skipped
        }
        if (rows.isEmpty()) return@withLock FlushResult.NothingToSend

        var anyLeft = false
        for (row in rows) {
            val studentExamId = syncDao.get(row.examId)?.studentExamId?.takeIf { it > 0 } ?: row.studentExamId
            if (studentExamId <= 0) {
                anyLeft = true
                continue
            }
            try {
                api.reportExamProblem(row.examId, studentExamId, AkmReportRules.payload(row.appVersion, row.androidVersion, row.deviceName, row.logcat)).close()
                dao.markSent(row.localId)
                Timber.tag(TAG).i("laporan %d terkirim (%s)", row.localId, reason)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (val decision = AkmViolationRules.classify(e)) {
                    ChunkDecision.Drop -> dao.markSent(row.localId) // the server will never accept it: closed, like legacy
                    is ChunkDecision.Pause -> {
                        pausedUntilElapsed = elapsed() + decision.seconds * 1000
                        anyLeft = true
                        break
                    }
                    ChunkDecision.Retry -> {
                        anyLeft = true
                        break
                    }
                }
            }
        }
        if (anyLeft) FlushResult.PartialOrFailed else FlushResult.Success
    }

    suspend fun countUnsent(): Int = dao.countUnsent()

    private companion object {
        const val TAG = "AKM-REPORT"
    }
}
