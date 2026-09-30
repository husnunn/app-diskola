package id.diskola.app.database

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.utils.DateUtil
import id.diskola.app.utils.FileUtils
import id.diskola.app.utils.PreferenceClass
import javax.inject.Inject

class DatabaseUtil @Inject constructor(
    @ApplicationContext val context: Context,
    val dateUtil: DateUtil,
    val localDatabase: LocalDatabase,
    val fileUtils: FileUtils,
    val pref: PreferenceClass,
    val moshi: Moshi
) {

}