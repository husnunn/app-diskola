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

    @Query("SELECT COUNT(*) FROM akm_synced_exam WHERE downloadStatus = 'DOWNLOADED'")
    suspend fun countPendingSubmission(): Int

    @Query("UPDATE akm_synced_exam SET downloadStatus = 'SUBMITTED' WHERE id = :id")
    suspend fun markSubmitted(id: Int)
}
