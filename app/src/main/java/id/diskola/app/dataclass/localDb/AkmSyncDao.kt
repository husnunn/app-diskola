package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AkmSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AkmSyncedExam)

    @Query("SELECT * FROM akm_synced_exam WHERE id = :id")
    fun observe(id: Int): Flow<AkmSyncedExam?>

    @Query("SELECT * FROM akm_synced_exam WHERE id = :id")
    suspend fun get(id: Int): AkmSyncedExam?

    @Query("SELECT * FROM akm_synced_exam")
    suspend fun getAll(): List<AkmSyncedExam>

    /** Everything except the (large) downloaded package, so progress updates never reload it. */
    @Query("SELECT id, downloadStatus, downloadProgress, totalQuestions, errorMessage, studentExamId, endAtMillis FROM akm_synced_exam")
    fun observeStates(): Flow<List<AkmDownloadState>>

    /** Downloaded rows written before `studentExamId`/`endAtMillis` existed: the id lives only inside the stored schedule. */
    @Query("SELECT * FROM akm_synced_exam WHERE downloadStatus = 'DOWNLOADED' AND studentExamId = 0 AND schedule IS NOT NULL")
    suspend fun getDownloadedWithoutSession(): List<AkmSyncedExam>

    @Query("UPDATE akm_synced_exam SET studentExamId = :studentExamId, endAtMillis = :endAtMillis WHERE id = :id")
    suspend fun updateSession(id: Int, studentExamId: Int, endAtMillis: Long?)

    /**
     * Exams whose answers may still be on their way — the logout block (doc 06 §15). A downloaded exam with a session counts from
     * download on; a submission that is `SENT` or `REJECTED` closes it. Must mirror `AkmRules.isPending`.
     */
    @Query(
        """
        SELECT COUNT(*) FROM akm_synced_exam e
        LEFT JOIN akm_submission s ON s.examId = e.id
        WHERE s.state = 'QUEUED'
           OR (s.state IS NULL AND e.downloadStatus = 'DOWNLOADED' AND e.studentExamId > 0)
        """,
    )
    suspend fun countPending(): Int

    /** Ids of the exams counted by [countPending] with the end time the deadline worker needs (null = unreadable, never guessed). */
    @Query(
        """
        SELECT e.id AS id, e.endAtMillis AS endAtMillis FROM akm_synced_exam e
        LEFT JOIN akm_submission s ON s.examId = e.id
        WHERE s.state = 'QUEUED'
           OR (s.state IS NULL AND e.downloadStatus = 'DOWNLOADED' AND e.studentExamId > 0)
        """,
    )
    suspend fun pendingDeadlines(): List<PendingDeadline>
}

data class PendingDeadline(val id: Int, val endAtMillis: Long?)

/** One exam's download bookkeeping, without the package itself. */
data class AkmDownloadState(
    val id: Int,
    val downloadStatus: String,
    val downloadProgress: Int,
    val totalQuestions: Int,
    val errorMessage: String,
    val studentExamId: Int,
    val endAtMillis: Long?,
)
