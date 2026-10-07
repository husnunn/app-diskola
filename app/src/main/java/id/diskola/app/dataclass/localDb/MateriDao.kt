package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.MateriTable
import kotlinx.coroutines.flow.Flow

/**
 * Local cache for Materi lists — doc `05-pembelajaran-materi-tugas.md` §2.4/§3.4: 20/page paging
 * from Room, `id DESC`. Two scopes share the same `materi` table: one subject's materials (student
 * `MateriPage` and the teacher's own subject browse) and a teacher's whole "Materi Saya" list
 * (optionally filtered by class **and** subject together — decision: combine filters instead of
 * replicating the legacy app's exclusive-filter bug).
 */
@Dao
interface MateriDao {
    @Query("SELECT * FROM materi WHERE subject_id = :subjectId ORDER BY id DESC")
    fun observeBySubject(subjectId: Int): Flow<List<MateriTable>>

    @Query("SELECT COUNT(*) FROM materi WHERE subject_id = :subjectId")
    suspend fun countBySubject(subjectId: Int): Int

    @Query("DELETE FROM materi WHERE subject_id = :subjectId")
    suspend fun clearBySubject(subjectId: Int)

    @Query(
        "SELECT * FROM materi WHERE teacher_id = :teacherId " +
            "AND (:subjectId IS NULL OR subject_id = :subjectId) " +
            "AND (:classId IS NULL OR class_id = :classId) ORDER BY id DESC"
    )
    fun observeByTeacher(teacherId: Int, subjectId: Int?, classId: Int?): Flow<List<MateriTable>>

    @Query(
        "SELECT COUNT(*) FROM materi WHERE teacher_id = :teacherId " +
            "AND (:subjectId IS NULL OR subject_id = :subjectId) " +
            "AND (:classId IS NULL OR class_id = :classId)"
    )
    suspend fun countByTeacher(teacherId: Int, subjectId: Int?, classId: Int?): Int

    @Query("DELETE FROM materi WHERE teacher_id = :teacherId")
    suspend fun clearByTeacher(teacherId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MateriTable>)

    @Query("SELECT * FROM materi WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): MateriTable?

    @Query("DELETE FROM materi WHERE id = :id")
    suspend fun deleteById(id: Int)
}
