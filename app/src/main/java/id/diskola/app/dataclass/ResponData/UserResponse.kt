package id.diskola.app.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class UserResponseItem(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val maidenName: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val username: String? = null,
    val image: String? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class attendanceOfficeDate(
    val radius: String? = null,
    val lat: String = "",
    val lng: String = "",
)

@Keep
@JsonClass(generateAdapter = true)
data class UserRole(
    val userId: Int = 0,
    val roleId: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val role: Role
)

@Keep
@JsonClass(generateAdapter = true)
data class Role(
    val id: Int = 0,
    val name: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class UserCompany(
    val userId: Int = 0,
    val companyId: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val company: Company
)

@Keep
@JsonClass(generateAdapter = true)
data class Company(
    val id: Int = 0,
    val name: String = "",
    val npwp: String = "",
    val email: String = "",
    val phone: String = "",
    val uuid: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class OwnerData(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val emailVerifiedAt: String = "",
    val phone: String = "",
    val phoneVerifiedAt: String = "",
    val userAvatarImage: String = "",
    val uuid: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val companyUuid: String = "",
    val owner: Any? // Because in your JSON, owner inside owner = null
)
