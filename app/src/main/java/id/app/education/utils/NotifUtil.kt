package id.app.education.utils


import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import id.app.education.apiservice.CommonApiService
import id.app.education.database.LocalDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotifUtil @Inject constructor(
    @ApplicationContext val context: Context,
    val localDatabase: LocalDatabase,
    val moshi: Moshi,
    val pref: PreferenceClass,
    val stringUtil: StringUtil,
    val api: CommonApiService
) {

    private val myId by lazy { pref.getString("klaspay_id") }

//    private val userTableAdapter by lazy { moshi.adapter(UserTable::class.java) }
//    val userTable: UserTable by lazy {
//        try {
//            userTableAdapter.fromJson(pref.getString("user"))
//                ?: UserTable(pref.getInt("user_id"))
//        } catch (e: Exception) {
//            UserTable(pref.getInt("user_id"))
//        }
//    }


}