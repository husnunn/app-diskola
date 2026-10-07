package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.PoinItemTable
import kotlinx.coroutines.flow.Flow

/** Local cache of the three Poin master lists (violation / achievement / handling "jenis"). */
@Dao
interface PoinDao {

    @Query("SELECT * FROM poin_item WHERE poin_type = :type AND name LIKE '%' || :search || '%' ORDER BY name COLLATE NOCASE")
    fun observeItems(type: Int, search: String): Flow<List<PoinItemTable>>

    @Query("SELECT COUNT(*) FROM poin_item WHERE poin_type = :type AND name LIKE '%' || :search || '%'")
    suspend fun countItems(type: Int, search: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PoinItemTable>)

    @Query("DELETE FROM poin_item WHERE poin_type = :type")
    suspend fun clearType(type: Int)
}
