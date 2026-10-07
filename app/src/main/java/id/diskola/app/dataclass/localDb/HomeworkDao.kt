package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.HomeworkAnswerFileTable
import id.diskola.app.dataclass.ResponData.HomeworkCollected
import id.diskola.app.dataclass.ResponData.HomeworkTable
import kotlinx.coroutines.flow.Flow

/** Tugas's local cache — one `homework` table serves both the student tabs (`type`-scoped) and the
 * teacher's own list (AND-combined Kelas/Mapel filters), same shape as `MateriDao`. */
@Dao
interface HomeworkDao {

    @Query("SELECT * FROM homework WHERE type = :type ORDER BY id DESC")
    fun observeByType(type: Int): Flow<List<HomeworkTable>>

    @Query("SELECT COUNT(*) FROM homework WHERE type = :type")
    suspend fun countByType(type: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HomeworkTable>)

    @Query("SELECT * FROM homework WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): HomeworkTable?

    /** Doc §5.5: a successful "Kirim Tugas" removes the row from the "Belum" tab. */
    @Query("DELETE FROM homework WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query(
        "SELECT * FROM homework WHERE teacher_id = :teacherId " +
            "AND (:subjectId IS NULL OR subject_id = :subjectId) " +
            "AND (:classId IS NULL OR class_id = :classId) ORDER BY id DESC",
    )
    fun observeByTeacher(teacherId: Int, subjectId: Int?, classId: Int?): Flow<List<HomeworkTable>>

    @Query(
        "SELECT COUNT(*) FROM homework WHERE teacher_id = :teacherId " +
            "AND (:subjectId IS NULL OR subject_id = :subjectId) " +
            "AND (:classId IS NULL OR class_id = :classId)",
    )
    suspend fun countByTeacher(teacherId: Int, subjectId: Int?, classId: Int?): Int

    @Query("DELETE FROM homework WHERE teacher_id = :teacherId")
    suspend fun clearByTeacher(teacherId: Int)

    @Query("SELECT * FROM homework_collected ORDER BY id DESC")
    fun observeCollected(): Flow<List<HomeworkCollected>>

    @Query("SELECT COUNT(*) FROM homework_collected")
    suspend fun countCollected(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollected(items: List<HomeworkCollected>)

    @Query("DELETE FROM homework_collected")
    suspend fun clearCollected()

    @Query("SELECT * FROM homework_answer_file WHERE homework_id = :homeworkId")
    fun observeAnswerFiles(homeworkId: Int): Flow<List<HomeworkAnswerFileTable>>

    @Query("SELECT COUNT(*) FROM homework_answer_file WHERE homework_id = :homeworkId")
    suspend fun countAnswerFiles(homeworkId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswerFiles(items: List<HomeworkAnswerFileTable>)

    /** Called when a student removes every answer file/link after having added some — keeps the
     * "upload" requirement reactive instead of a one-way latch (decision: fix, don't replicate). */
    @Query("DELETE FROM homework_answer_file WHERE homework_id = :homeworkId")
    suspend fun clearAnswerFiles(homeworkId: Int)
}
