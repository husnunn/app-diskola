package id.app.education.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PreferenceModule {

    @Provides
    @Reusable
    fun instance(@ApplicationContext context: Context) = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
}
