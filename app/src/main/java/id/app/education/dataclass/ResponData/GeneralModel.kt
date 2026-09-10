package id.app.education.dataclass.ResponData

import id.app.education.apiservice.CommonApiService
import id.app.education.utils.IntentUtil
import id.app.education.utils.PreferenceClass
import kotlin.jvm.java

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(
    val pref: PreferenceClass,
    val moshi: Moshi,
    val intentUtil: IntentUtil,
    val commonApiService: CommonApiService
) : id.app.education.viewmodel.BaseViewModel() {

    suspend fun checkVersion(): CheckVersion? = try {
        val resp = commonApiService.checkVersion().data
        moshi.adapter(CheckVersion::class.java).fromJson(resp.setting_globals_value)
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    suspend fun policy(): PolicyData? = try {
        val resp = commonApiService.policy()

        moshi.adapter(PolicyData::class.java).fromJson(resp.data.content)
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

}

@JsonClass(generateAdapter = true)
@Keep
data class CheckVersionResponse(
    val data: CheckVersionRespData = CheckVersionRespData()
)

@JsonClass(generateAdapter = true)
@Keep
data class CheckVersionRespData(
    val setting_globals_lable: String = "Android Version Apps",
    val setting_globals_name: String = "android-version",
    val setting_globals_value: String = ""
)


@JsonClass(generateAdapter = true)
@Keep
data class CheckVersionData(
    val setting_globals_lable: String = "Android Version Apps",
    val setting_globals_name: String = "android-version",
    val setting_globals_value: CheckVersion = CheckVersion()
)

@JsonClass(generateAdapter = true)
@Keep
data class CheckVersion(val version: String = "1.0.0")
