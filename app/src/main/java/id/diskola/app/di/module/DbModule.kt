package id.diskola.app.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.diskola.app.database.LocalDatabase
import id.diskola.app.dataclass.localDb.AgendaDao
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.HomeworkDao
import id.diskola.app.dataclass.localDb.MapelDao
import id.diskola.app.dataclass.localDb.MateriDao
import id.diskola.app.dataclass.localDb.PoinDao
import id.diskola.app.dataclass.localDb.PresensiDao
import id.diskola.app.dataclass.localDb.SchoolDao
import id.diskola.app.dataclass.localDb.TeacherReferenceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Singleton
    @Provides
    fun instance(@ApplicationContext context: Context): LocalDatabase =
        Room.databaseBuilder(context, LocalDatabase::class.java, "ebede.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun akmSyncDao(db: LocalDatabase): AkmSyncDao = db.akmSyncDao()

    @Provides
    fun schoolDao(db: LocalDatabase): SchoolDao = db.schoolDao()

    @Provides
    fun mapelDao(db: LocalDatabase): MapelDao = db.mapelDao()

    @Provides
    fun materiDao(db: LocalDatabase): MateriDao = db.materiDao()

    @Provides
    fun teacherReferenceDao(db: LocalDatabase): TeacherReferenceDao = db.teacherReferenceDao()

    @Provides
    fun homeworkDao(db: LocalDatabase): HomeworkDao = db.homeworkDao()

    @Provides
    fun poinDao(db: LocalDatabase): PoinDao = db.poinDao()

    @Provides
    fun agendaDao(db: LocalDatabase): AgendaDao = db.agendaDao()

    @Provides
    fun presensiDao(db: LocalDatabase): PresensiDao = db.presensiDao()

}
