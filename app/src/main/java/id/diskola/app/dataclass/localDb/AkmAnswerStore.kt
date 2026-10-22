package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One answered question. Only answered questions have a row (an essay left empty still does — legacy counts it as answered), so a
 * missing row means "not answered". [answerJson] is an [id.diskola.app.dataclass.akm.StoredAnswer], keyed by answer ids.
 */
@Entity(tableName = "akm_answer", primaryKeys = ["examId", "questionId"])
data class AkmAnswerEntity(
    val examId: Int,
    val questionId: Int,
    val instructionId: Int,
    /** The API `answerType` — what the payload sends as `answerType`. */
    val typeLabel: String,
    val answerJson: String,
    val updatedAt: Long,
)

@Dao
interface AkmAnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AkmAnswerEntity)

    @Query("DELETE FROM akm_answer WHERE examId = :examId AND questionId = :questionId")
    suspend fun delete(examId: Int, questionId: Int)

    @Query("SELECT * FROM akm_answer WHERE examId = :examId")
    fun observe(examId: Int): Flow<List<AkmAnswerEntity>>

    @Query("SELECT * FROM akm_answer WHERE examId = :examId")
    suspend fun getAll(examId: Int): List<AkmAnswerEntity>

    @Query("SELECT COUNT(*) FROM akm_answer WHERE examId = :examId")
    suspend fun count(examId: Int): Int
}
