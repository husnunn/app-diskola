package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import id.diskola.app.dataclass.ResponData.StaffAgendaDayTable
import id.diskola.app.dataclass.ResponData.StaffAgendaItemTable

/** Agenda Mingguan cache — one day row + its session rows per date, replaced atomically. */
@Dao
abstract class AgendaDao {

    @Query("SELECT * FROM staff_agenda_day WHERE date = :date LIMIT 1")
    abstract suspend fun getDay(date: String): StaffAgendaDayTable?

    @Query("SELECT * FROM staff_agenda_item WHERE date = :date ORDER BY sort_order, agenda_id")
    abstract suspend fun getItems(date: String): List<StaffAgendaItemTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDay(day: StaffAgendaDayTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertItems(items: List<StaffAgendaItemTable>)

    @Query("DELETE FROM staff_agenda_day WHERE date = :date")
    protected abstract suspend fun deleteDay(date: String)

    @Query("DELETE FROM staff_agenda_item WHERE date = :date")
    protected abstract suspend fun deleteItems(date: String)

    @Transaction
    open suspend fun replaceDay(day: StaffAgendaDayTable, items: List<StaffAgendaItemTable>) {
        deleteItems(day.date)
        deleteDay(day.date)
        insertDay(day)
        insertItems(items)
    }
}
