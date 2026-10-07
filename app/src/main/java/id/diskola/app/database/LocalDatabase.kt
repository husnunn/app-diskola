package id.diskola.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.GradeTable
import id.diskola.app.dataclass.ResponData.HomeworkAnswerFileTable
import id.diskola.app.dataclass.ResponData.HomeworkCollected
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.dataclass.ResponData.MajorItem
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.dataclass.ResponData.LeaveRequestTable
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.dataclass.ResponData.PoinItemTable
import id.diskola.app.dataclass.ResponData.PresensiDayTable
import id.diskola.app.dataclass.ResponData.PresensiRekapTable
import id.diskola.app.dataclass.ResponData.StaffAgendaDayTable
import id.diskola.app.dataclass.ResponData.StaffAgendaItemTable
import id.diskola.app.dataclass.localDb.AgendaDao
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmSyncedExam
import id.diskola.app.dataclass.localDb.HomeworkDao
import id.diskola.app.dataclass.localDb.MapelDao
import id.diskola.app.dataclass.localDb.MateriDao
import id.diskola.app.dataclass.localDb.PoinDao
import id.diskola.app.dataclass.localDb.PresensiDao
import id.diskola.app.dataclass.localDb.SchoolDao
import id.diskola.app.dataclass.localDb.SchoolEntity
import id.diskola.app.dataclass.localDb.TeacherReferenceDao
import id.diskola.app.dataclass.localDb.User
import id.diskola.app.dataclass.localDb.UserDao

@Database(
    version = 7,
    exportSchema = false,
    entities = [
        User::class,
        AkmSyncedExam::class,
        SchoolEntity::class,
        MapelTable::class,
        MateriTable::class,
        ClassRoomTable::class,
        MajorItem::class,
        GradeTable::class,
        HomeworkTable::class,
        HomeworkAnswerFileTable::class,
        HomeworkCollected::class,
        PoinItemTable::class,
        StaffAgendaDayTable::class,
        StaffAgendaItemTable::class,
        PresensiDayTable::class,
        PresensiRekapTable::class,
        LeaveRequestTable::class,
//        Province::class,
//        City::class
    ]
)
@TypeConverters(DbConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun akmSyncDao(): AkmSyncDao
    abstract fun schoolDao(): SchoolDao
    abstract fun mapelDao(): MapelDao
    abstract fun materiDao(): MateriDao
    abstract fun teacherReferenceDao(): TeacherReferenceDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun poinDao(): PoinDao
    abstract fun agendaDao(): AgendaDao
    abstract fun presensiDao(): PresensiDao
//    abstract fun provinceDao(): ProvinceDao
//    abstract fun cityDao(): CityDao
}