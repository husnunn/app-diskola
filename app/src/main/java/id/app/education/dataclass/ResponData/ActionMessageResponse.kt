package id.app.education.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class ActionMessageResponse(
    val message: String = ""
)