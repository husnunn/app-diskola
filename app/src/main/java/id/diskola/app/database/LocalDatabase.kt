package id.diskola.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmSyncedExam
import id.diskola.app.dataclass.localDb.SchoolDao
import id.diskola.app.dataclass.localDb.SchoolEntity
import id.diskola.app.dataclass.localDb.User
import id.diskola.app.dataclass.localDb.UserDao

@Database(
    version = 3,
    exportSchema = false,
    entities = [
        User::class,
        AkmSyncedExam::class,
        SchoolEntity::class,
//        Province::class,
//        City::class
    ]
)
@TypeConverters(DbConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun akmSyncDao(): AkmSyncDao
    abstract fun schoolDao(): SchoolDao
//    abstract fun provinceDao(): ProvinceDao
//    abstract fun cityDao(): CityDao
}