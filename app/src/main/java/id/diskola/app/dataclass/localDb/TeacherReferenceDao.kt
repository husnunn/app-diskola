package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.MajorItem
import id.diskola.app.dataclass.ResponData.GradeTable
import kotlinx.coroutines.flow.Flow

/**
 * Small reference lists used by the "Materi Saya" filters and the upload form's "Ditampilkan ke"
 * dropdowns (doc `05-pembelajaran-materi-tugas.md` §3.4) — kelas, jurusan, jenjang. These aren't
 * paged (the legacy app fetches them whole too), just cached so the dropdowns survive
 * configuration changes without a re-fetch and so `MateriTable`'s `class_id`/`major_id` joins have
 * a name to resolve against later if needed.
 */
@Dao
interface TeacherReferenceDao {
    @Query("SELECT * FROM classroom ORDER BY grade ASC, name ASC")
    fun observeClasses(): Flow<List<ClassRoomTable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(items: List<ClassRoomTable>)

    @Query("DELETE FROM classroom")
    suspend fun clearClasses()

    @Query("SELECT * FROM major ORDER BY name ASC")
    fun observeMajors(): Flow<List<MajorItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMajors(items: List<MajorItem>)

    @Query("DELETE FROM major")
    suspend fun clearMajors()

    @Query("SELECT * FROM grade ORDER BY id ASC")
    fun observeGrades(): Flow<List<GradeTable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(items: List<GradeTable>)

    @Query("DELETE FROM grade")
    suspend fun clearGrades()
}
