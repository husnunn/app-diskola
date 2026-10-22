package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * The last schedule list the server sent, so the list can be opened offline. It only ever holds what the list endpoints returned and
 * never touches downloaded packages or answers (legacy deleted exams "missing from the response" — including one being worked on).
 */
@Entity(tableName = "akm_schedule_cache", primaryKeys = ["id", "kind"])
data class AkmScheduleCacheEntity(
    val id: Int,
    /** [KIND_LIST] (`exam-schedules`) or [KIND_SCORED] (`exam-schedules-scored`). */
    val kind: String,
    /** The schedule as Moshi JSON. */
    val scheduleJson: String,
    val savedAt: Long,
) {
    companion object {
        const val KIND_LIST = "LIST"
        const val KIND_SCORED = "SCORED"
    }
}

@Dao
abstract class AkmScheduleCacheDao {
    @Query("SELECT * FROM akm_schedule_cache WHERE kind = :kind")
    abstract suspend fun getAll(kind: String): List<AkmScheduleCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<AkmScheduleCacheEntity>)

    @Query("DELETE FROM akm_schedule_cache WHERE kind = :kind")
    abstract suspend fun clear(kind: String)

    /** The server's list is authoritative for the cache: replace it in one step so it never shows a half-written list. */
    @Transaction
    open suspend fun replace(kind: String, entities: List<AkmScheduleCacheEntity>) {
        clear(kind)
        insertAll(entities)
    }
}
