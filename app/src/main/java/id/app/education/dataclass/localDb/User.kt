package id.app.education.dataclass.localDb

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "user",
    indices = [Index("id", unique = true)]
)
data class User(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val userAvatarImage: String = "",
    val version: Int = 0,
    val emailVerifiedAt: String = "",
    val roles: String = ""
)
