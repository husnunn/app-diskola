package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.SchoolItem
import kotlinx.coroutines.flow.Flow

/**
 * Local cache of `GET mobile/app/authentication/schools` pages, backing the school picker's
 * local-only search (doc `02-auth-login-sesi.md` §4.2, decision Q3 — the server's own `name`
 * filter is never used; only this cache is searched).
 */
@Entity(tableName = "school")
data class SchoolEntity(
    @PrimaryKey val id: Int,
    val uuid: String,
    val name: String,
    val image: String,
    val address: String,
    val cityName: String,
    val coordinateRadius: String,
    val coordinateLatitude: Double,
    val coordinateLongitude: Double,
)

fun SchoolItem.toEntity() = SchoolEntity(
    id = id,
    uuid = uuid,
    name = name,
    image = image,
    address = address.orEmpty(),
    cityName = city_name.orEmpty(),
    coordinateRadius = coordinate_radius ?: "50",
    coordinateLatitude = coordinate_latitude ?: 0.0,
    coordinateLongitude = coordinate_longitude ?: 0.0,
)

fun SchoolEntity.toItem() = SchoolItem(
    id = id,
    uuid = uuid,
    name = name,
    image = image,
    address = address,
    city_name = cityName,
    coordinate_radius = coordinateRadius,
    coordinate_latitude = coordinateLatitude,
    coordinate_longitude = coordinateLongitude,
)

@Dao
interface SchoolDao {
    @Query("SELECT * FROM school WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun observe(query: String): Flow<List<SchoolEntity>>

    @Query("SELECT COUNT(*) FROM school")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SchoolEntity>)

    @Query("DELETE FROM school")
    suspend fun clear()
}
