package id.app.education.network

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import id.app.education.di.module.NullToEmptyString

@Keep
@JsonClass(generateAdapter = true)
data class NotificationResponse(
    @NullToEmptyString val parent: String = "",
    @NullToEmptyString val child: String = "",
    @NullToEmptyString val child_id: String = ""
)