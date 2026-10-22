package id.diskola.app.repository

import android.os.SystemClock
import com.squareup.moshi.Moshi
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmViolationDao
import id.diskola.app.dataclass.localDb.AkmViolationEntity
import id.diskola.app.utils.AkmViolationRules
import id.diskola.app.utils.AkmViolationRules.ChunkDecision
import id.diskola.app.utils.Utils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/** How one [AkmViolationQueue.flush] ended. */
sealed interface FlushResult {
    /** Everything that was queued is on the server. */
    data object Success : FlushResult

    data object NothingToSend : FlushResult

    /** Not tried: offline, paused by the server, or the session id is not known yet. The rows stay. */
    data object Skipped : FlushResult

    /** Some rows are still queued (a chunk failed or the server asked for a pause). */
    data object PartialOrFailed : FlushResult
}

/**
 * The violations of an exam session, from the moment they are noticed to the moment the school has them (doc `06` §10.3–10.4).
 * [record] writes a row to Room at once; [flush] sends the rows in chunks of 50, oldest first, and deletes what the server took.
 * One `Mutex` guards every flush **and** every pause change, so two triggers (the foreground loop, the worker, a submit) can never send
 * the same row twice.
 *
 * What an answer means is [AkmViolationRules.classify]: 400 drops the chunk, 429 pauses for `Retry-After`, 403/5xx pause 30 s, anything
 * else (offline included) keeps the rows. A row is never sent under session id 0 (legacy did, and a 400 then threw it away): until the
 * real id is known the rows wait.
 */
@Singleton
class AkmViolationQueue @Inject constructor(
    private val dao: AkmViolationDao,
    private val syncDao: AkmSyncDao,
    private val api: AsesmenApiService,
    private val utils: Utils,
    private val moshi: Moshi,
) {
    private val mutex = Mutex()
    private val pausedUntilElapsed = mutableMapOf<Int, Long>()

    private fun elapsed(): Long = SystemClock.elapsedRealtime()

    /** Stores one violation. Never throws into the caller: a failed write is logged (the student must not be hurt by our own bug). */
    suspend fun record(
        examId: Int,
        type: String,
        metadata: Map<String, Any>? = null,
        detectedAtMillis: Long = System.currentTimeMillis(),
    ) {
        try {
            val studentExamId = syncDao.get(examId)?.studentExamId ?: 0
            dao.insert(
                AkmViolationEntity(
                    examId = examId,
                    studentExamId = studentExamId,
                    type = AkmViolationRules.truncateType(type),
                    detectedAtMillis = detectedAtMillis,
                    metadataJson = AkmViolationRules.metadataJson(metadata),
                    createdAtElapsed = elapsed(),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Gagal menyimpan pelanggaran %s", type)
        }
    }

    suspend fun flush(examId: Int, reason: String): FlushResult = mutex.withLock {
        if (examId < 1) return@withLock FlushResult.Skipped
        // The legacy test: a network with INTERNET. Not VALIDATED — a school Wi-Fi that blocks the connectivity check can still reach the server.
        if (!utils.isInternetAvailable()) return@withLock FlushResult.Skipped
        if (isPaused(examId)) return@withLock FlushResult.Skipped
        val rows = try {
            dao.pending(examId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Gagal membaca antrean pelanggaran")
            return@withLock FlushResult.Skipped
        }
        if (rows.isEmpty()) return@withLock FlushResult.NothingToSend

        val studentExamId = syncDao.get(examId)?.studentExamId?.takeIf { it > 0 } ?: rows.firstOrNull { it.studentExamId > 0 }?.studentExamId ?: 0
        if (studentExamId <= 0) {
            Timber.tag(TAG).w("exam=%d: id sesi belum diketahui, %d pelanggaran ditunda (%s)", examId, rows.size, reason)
            return@withLock FlushResult.Skipped
        }

        var anyFailure = false
        for (chunk in AkmViolationRules.chunks(rows)) {
            if (isPaused(examId)) {
                anyFailure = true
                break
            }
            val body = chunk.map { row ->
                AkmViolationRules.payloadItem(row.type, row.detectedAtMillis, AkmViolationRules.parseMetadata(row.metadataJson, moshi))
            }
            try {
                api.reportExamViolations(examId, studentExamId, body).close()
                dao.deleteByIds(chunk.map { it.localId })
                Timber.tag(TAG).i("exam=%d: %d pelanggaran terkirim (%s)", examId, chunk.size, reason)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (val decision = AkmViolationRules.classify(e)) {
                    ChunkDecision.Drop -> {
                        Timber.tag(TAG).w(e, "exam=%d: server menolak %d pelanggaran (400), dibuang", examId, chunk.size)
                        dao.deleteByIds(chunk.map { it.localId })
                    }
                    is ChunkDecision.Pause -> {
                        pausedUntilElapsed[examId] = elapsed() + decision.seconds * 1000
                        Timber.tag(TAG).w(e, "exam=%d: antrean dijeda %d detik", examId, decision.seconds)
                        anyFailure = true
                        break
                    }
                    ChunkDecision.Retry -> {
                        anyFailure = true
                        break
                    }
                }
            }
        }
        if (anyFailure) FlushResult.PartialOrFailed else FlushResult.Success
    }

    /** Sends the rows of every exam that has some (the background worker and the app start use this). */
    suspend fun flushAll(reason: String) {
        dao.examIdsWithRows().forEach { flush(it, reason) }
    }

    suspend fun pendingCount(examId: Int): Int = dao.count(examId)

    suspend fun examIdsWithRows(): List<Int> = dao.examIdsWithRows()

    /** Forgets the server's pause for [examId] (a new session starts with a clean slate). Under the same lock as [flush]. */
    suspend fun clearPause(examId: Int) = mutex.withLock { pausedUntilElapsed.remove(examId); Unit }

    private fun isPaused(examId: Int): Boolean {
        val until = pausedUntilElapsed[examId] ?: return false
        if (elapsed() >= until) {
            pausedUntilElapsed.remove(examId)
            return false
        }
        return true
    }

    private companion object {
        const val TAG = "AKM-VIOLATION"
    }
}
