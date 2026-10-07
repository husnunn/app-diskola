package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.MapelTable
import kotlinx.coroutines.flow.Flow

/**
 * Local cache backing the Materi subject picker (student `TheoryPage` / teacher hidden
 * `MapelTeacherPage`) — doc `05-pembelajaran-materi-tugas.md` §2.4: local-only search over
 * whatever pages have been fetched (name **or** `label`), 20/page infinite scroll.
 */
@Dao
interface MapelDao {
    @Query("SELECT * FROM mapel WHERE name LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' ORDER BY name ASC")
    fun observe(query: String): Flow<List<MapelTable>>

    @Query("SELECT COUNT(*) FROM mapel")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MapelTable>)

    @Query("DELETE FROM mapel")
    suspend fun clear()
}
