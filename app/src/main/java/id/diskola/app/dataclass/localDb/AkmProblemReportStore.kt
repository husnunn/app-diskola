package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * A "Laporkan Masalah" report the student filed from the penalty dialog (doc `06` §13), kept until the server has it. Like legacy, only
 * `appVersion`, `androidVersion`, `deviceName` and `logcat` are sent; the student's own text, the checkbox and the violations are stored
 * here but not sent (decision: no backend contract for them yet — `docs/FLOW_QUESTIONS.md`). Rows stay after sending (`isSent`).
 */
@Entity(tableName = "akm_problem_report", indices = [Index("examId")])
data class AkmProblemReportEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val examId: Int,
    val studentExamId: Int,
    val message: String,
    val violationsJson: String,
    val isSystemFaultConfirmed: Boolean,
    val appVersion: String,
    val androidVersion: String,
    val deviceName: String,
    val logcat: String,
    val createdAt: Long,
    val isSent: Boolean = false,
)

@Dao
interface AkmProblemReportDao {
    @Insert
    suspend fun insert(row: AkmProblemReportEntity): Long

    @Query("SELECT * FROM akm_problem_report WHERE isSent = 0 ORDER BY localId ASC")
    suspend fun unsent(): List<AkmProblemReportEntity>

    @Query("UPDATE akm_problem_report SET isSent = 1 WHERE localId = :localId")
    suspend fun markSent(localId: Long)

    @Query("SELECT COUNT(*) FROM akm_problem_report WHERE isSent = 0")
    suspend fun countUnsent(): Int
}
