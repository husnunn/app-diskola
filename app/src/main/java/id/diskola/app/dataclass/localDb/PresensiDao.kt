package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.diskola.app.dataclass.ResponData.LeaveRequestTable
import id.diskola.app.dataclass.ResponData.PresensiDayTable
import id.diskola.app.dataclass.ResponData.PresensiRekapTable
import kotlinx.coroutines.flow.Flow

/** Cache for Data Absensi (per month), Rekap (per year) and the Izin list. */
@Dao
abstract class PresensiDao {

    // ---- Data Absensi ----

    /** [prefix] is `yyyy-MM`. Oldest first, like the legacy list. */
    @Query("SELECT * FROM presensi_day WHERE date LIKE :prefix || '-%' ORDER BY date")
    abstract suspend fun getMonth(prefix: String): List<PresensiDayTable>

    @Query("SELECT * FROM presensi_day WHERE date = :date LIMIT 1")
    abstract suspend fun getDay(date: String): PresensiDayTable?

    @Query("DELETE FROM presensi_day WHERE date LIKE :prefix || '-%'")
    protected abstract suspend fun clearMonth(prefix: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDays(rows: List<PresensiDayTable>)

    @Transaction
    open suspend fun replaceMonth(prefix: String, rows: List<PresensiDayTable>) {
        clearMonth(prefix)
        insertDays(rows)
    }

    // ---- Rekap ----

    @Query("SELECT * FROM presensi_rekap WHERE year = :year ORDER BY orderIndex")
    abstract suspend fun getRekap(year: Int): List<PresensiRekapTable>

    @Query("DELETE FROM presensi_rekap WHERE year = :year")
    protected abstract suspend fun clearRekap(year: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRekap(rows: List<PresensiRekapTable>)

    @Transaction
    open suspend fun replaceRekap(year: Int, rows: List<PresensiRekapTable>) {
        clearRekap(year)
        insertRekap(rows)
    }

    // ---- Izin ----

    /** [status] = `pending`/`approved`/`rejected`, or null for all. Newest first. */
    @Query("SELECT * FROM leave_request WHERE (:status IS NULL OR approvalStatus = :status) ORDER BY date DESC, uuid DESC")
    abstract fun observeLeaves(status: String?): Flow<List<LeaveRequestTable>>

    @Query("SELECT * FROM leave_request WHERE uuid = :uuid LIMIT 1")
    abstract suspend fun getLeave(uuid: String): LeaveRequestTable?

    @Query("SELECT * FROM leave_request WHERE date = :date ORDER BY uuid DESC LIMIT 1")
    abstract suspend fun getLeaveByDate(date: String): LeaveRequestTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertLeaves(rows: List<LeaveRequestTable>)

    @Query("DELETE FROM leave_request")
    protected abstract suspend fun clearLeaves()

    @Transaction
    open suspend fun replaceLeaves(rows: List<LeaveRequestTable>) {
        clearLeaves()
        upsertLeaves(rows)
    }
}
