package id.diskola.app.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

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
