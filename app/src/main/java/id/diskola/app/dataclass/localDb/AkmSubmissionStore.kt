package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Where the answers of one exam stand. No row = nothing was asked to be sent yet. States are [id.diskola.app.utils.AkmRules.SubmissionState];
 * `SENT` is only ever set after the server accepted the answers, and no update here can move a `SENT` row back.
 */
@Entity(tableName = "akm_submission")
data class AkmSubmissionEntity(
    @PrimaryKey val examId: Int,
    val state: String,
    /** The student pressed "Kumpulkan" (true) or the deadline fired (false). */
    val manual: Boolean,
    /** Upload attempts made so far, for the retry cap. */
    val attempts: Int,
    val queuedAt: Long,
    val lastAttemptAt: Long,
    val sentAt: Long,
    /** The reason of the last failed attempt, or the server's refusal for `REJECTED`. */
    val errorMessage: String,
)

@Dao
interface AkmSubmissionDao {
    @Query("SELECT * FROM akm_submission WHERE examId = :examId")
    suspend fun get(examId: Int): AkmSubmissionEntity?

    @Query("SELECT * FROM akm_submission WHERE examId = :examId")
    fun observe(examId: Int): Flow<AkmSubmissionEntity?>

    @Query("SELECT * FROM akm_submission")
    fun observeAll(): Flow<List<AkmSubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: AkmSubmissionEntity): Long

    /** Asks for the answers to be sent. A `SENT` exam stays `SENT`; a refused one may be asked again (manual re-upload). */
    @Query(
        """
        UPDATE akm_submission
        SET state = 'QUEUED', manual = (manual OR :manual), attempts = 0, errorMessage = ''
        WHERE examId = :examId AND state != 'SENT'
        """,
    )
    suspend fun requeue(examId: Int, manual: Boolean): Int

    /** Takes back a `QUEUED` row whose first, online attempt failed so the student can edit and press "Kumpulkan" again. */
    @Query("DELETE FROM akm_submission WHERE examId = :examId AND state = 'QUEUED'")
    suspend fun deleteQueued(examId: Int)

    @Query("UPDATE akm_submission SET state = 'SENT', sentAt = :now, errorMessage = '' WHERE examId = :examId")
    suspend fun markSent(examId: Int, now: Long)

    @Query("UPDATE akm_submission SET state = 'REJECTED', lastAttemptAt = :now, errorMessage = :message WHERE examId = :examId AND state != 'SENT'")
    suspend fun markRejected(examId: Int, message: String, now: Long)

    @Query("UPDATE akm_submission SET attempts = :attempts, lastAttemptAt = :now, errorMessage = :message WHERE examId = :examId AND state = 'QUEUED'")
    suspend fun recordAttempt(examId: Int, attempts: Int, now: Long, message: String)
}
