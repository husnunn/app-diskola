package id.diskola.app.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString


@Keep
@JsonClass(generateAdapter = true)
data class FeedUser(
    val id: Int = 0,
    @NullToEmptyString val uuid: String = "",
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val email: String = "",
    @NullToEmptyString val user_username: String = "",
    @NullToEmptyString val nisn_nik: String = "",
    @NullToEmptyString val nis_nik: String = "",
    @NullToEmptyString val phone: String = "",
    @NullToEmptyString val user_avatar_image: String = "",
    val is_verified: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
data class FeedComment(
    val user: FeedUser = FeedUser(),
    @Json(name = "id") val row_id: Int = 0,
    @NullToEmptyString val feed_comments_body: String = "",
    @NullToEmptyString val created_at_label: String = ""
)


@Keep
@JsonClass(generateAdapter = true)
data class PolicyResponse(val data: PolicyData = PolicyData())
@Keep
@JsonClass(generateAdapter = true)
data class PolicyData(val content: String = "")