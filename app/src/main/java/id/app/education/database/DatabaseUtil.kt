package id.app.education.database

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import id.app.education.utils.DateUtil
import id.app.education.utils.FileUtils
import id.app.education.utils.PreferenceClass
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