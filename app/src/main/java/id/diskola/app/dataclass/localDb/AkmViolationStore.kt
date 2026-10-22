package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One thing the exam session recorded about the student (doc `06` §10), waiting to be sent to the school. Rows are deleted once the
 * server took them (or said it never will, a 400). [detectedAtMillis] is the wall-clock moment; the ISO text of the payload is made when
 * the row is sent. Kept in Room, not memory: a violation noticed just before the phone dies must still go out later.
 */
@Entity(tableName = "akm_violation", indices = [Index("examId")])
data class AkmViolationEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val examId: Int,
    /** The session id known when the row was written; 0 while it was not known yet (the URL then waits for the real one). */
    val studentExamId: Int,
    val type: String,
    val detectedAtMillis: Long,
    /** JSON object text (`AkmViolationRules.metadataJson`), or null. */
    val metadataJson: String?,
    val createdAtElapsed: Long,
)

@Dao
interface AkmViolationDao {
    @Insert
    suspend fun insert(row: AkmViolationEntity): Long

    /** Oldest first: the school sees them in the order they happened (two rows written a millisecond apart may land out of order, hence the time first). */
    @Query("SELECT * FROM akm_violation WHERE examId = :examId ORDER BY detectedAtMillis ASC, localId ASC")
    suspend fun pending(examId: Int): List<AkmViolationEntity>

    @Query("DELETE FROM akm_violation WHERE localId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM akm_violation WHERE examId = :examId")
    suspend fun count(examId: Int): Int

    /** Exams that still have rows to send, so the background worker can be put back after the app was killed. */
    @Query("SELECT DISTINCT examId FROM akm_violation")
    suspend fun examIdsWithRows(): List<Int>
}
