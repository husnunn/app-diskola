package id.app.education.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import id.app.education.dataclass.localDb.User
import id.app.education.dataclass.localDb.UserDao

@Database(
    version = 1,
    exportSchema = false,
    entities = [
        User::class,
//        Province::class,
//        City::class
    ]
)
@TypeConverters(DbConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
//    abstract fun provinceDao(): ProvinceDao
//    abstract fun cityDao(): CityDao
}