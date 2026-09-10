package id.app.education.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import id.app.education.di.module.NullToEmptyString

@JsonClass(generateAdapter = true)
@Keep
data class ContactResponse(val data_list: List<ContactItem> = emptyList())

@JsonClass(generateAdapter = true)
@Keep
data class ContactItem(
    val user_id: Int = 0,
    @NullToEmptyString val user_uuid: String = "",
    @NullToEmptyString val foto: String = "",
    @NullToEmptyString val wallet_id: String = "",
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val kelas: String = "",
    @NullToEmptyString val jurusan: String = "",
)
