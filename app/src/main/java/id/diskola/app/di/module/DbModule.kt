package id.diskola.app.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.diskola.app.database.LocalDatabase
import id.diskola.app.dataclass.localDb.AkmSyncDao
import id.diskola.app.dataclass.localDb.SchoolDao
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

}
