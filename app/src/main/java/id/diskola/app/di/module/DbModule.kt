package id.diskola.app.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.diskola.app.database.LocalDatabase
import id.diskola.app.database.MIGRATION_7_8
import id.diskola.app.database.MIGRATION_8_9
import id.diskola.app.dataclass.localDb.AgendaDao
import id.diskola.app.dataclass.localDb.AkmAnswerDao
import id.diskola.app.dataclass.localDb.AkmProblemReportDao
import id.diskola.app.dataclass.localDb.AkmScheduleCacheDao
import id.diskola.app.dataclass.localDb.AkmSubmissionDao
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.AkmViolationDao
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
            // Exam answers and unsent violations must survive an update: v7 -> v8 -> v9 are explicit. Anything else (the other tables are
            // re-fetchable caches) may be dropped.
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun akmSyncDao(db: LocalDatabase): AkmSyncDao = db.akmSyncDao()

    @Provides
    fun akmAnswerDao(db: LocalDatabase): AkmAnswerDao = db.akmAnswerDao()

    @Provides
    fun akmSubmissionDao(db: LocalDatabase): AkmSubmissionDao = db.akmSubmissionDao()

    @Provides
    fun akmScheduleCacheDao(db: LocalDatabase): AkmScheduleCacheDao = db.akmScheduleCacheDao()

    @Provides
    fun akmViolationDao(db: LocalDatabase): AkmViolationDao = db.akmViolationDao()

    @Provides
    fun akmProblemReportDao(db: LocalDatabase): AkmProblemReportDao = db.akmProblemReportDao()

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
