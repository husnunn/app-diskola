package id.app.education.dataclass.ResponData


import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import id.app.education.di.module.NullToEmptyString

@JsonClass(generateAdapter = true)
data class LoginAccountRequest(
    val uuid: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginAccountApiResponse(
    val data: LoginAccountUserData?,
    val is_klaspay_activated: Boolean?,
    val is_email_verified: Boolean?,
    val is_verified: Boolean?,
    val rule_label: String?,
    val rule: LoginRuleData?,
    val meta: LoginMetaData?,
    val product_school: ProductSchoolData?,
    val credential: CredentialData?
)

@Keep
@JsonClass(generateAdapter = true)
data class StudentClass(
    val id: Int = 0,
    val class_room: ClassRoomTable = ClassRoomTable(),
    var name:String = "",
    val grade : Int = 0,
)

@Keep
@JsonClass(generateAdapter = true)
data class UserResponseData(
    val id: Int = 0,
    val uuid: String = "",
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val email: String = "",
    @NullToEmptyString val nisn_nik: String = "",
    @NullToEmptyString val nis_nik: String = "",
    @NullToEmptyString val phone: String = "",
    @NullToEmptyString val place_of_birth: String = "",
    @NullToEmptyString val date_of_birth: String = "",
    @NullToEmptyString val gender: String = "",
    @NullToEmptyString var user_avatar_image: String = "",
    @NullToEmptyString val religion: String = "",
    @NullToEmptyString val blood_type: String = "",
    @NullToEmptyString var address: String = "",
    @NullToEmptyString var sub_district_id: String = "",
    @NullToEmptyString var sub_district: String = "",
    @NullToEmptyString var city_id: String = "",
    @NullToEmptyString var city: String = "",
    @NullToEmptyString var province_id: String = "",
    @NullToEmptyString var province: String = "",
    @NullToEmptyString val user_username: String = "",
    val student: StudentItem? = null,
    val teacher: TeacherItem? = null,
    val school: SekolahItem? = null,
    val roles: List<UserRoles> = emptyList()
)

data class UserRoles(
    @PrimaryKey val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val guard_name: String = ""
)

data class SekolahItem(
    @PrimaryKey val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val image: String = "",
    val address: String = "",
    val coordinate_radius: String? = "50",
    val coordinate_latitude: Double? = 0.0,
    val coordinate_longitude: Double? = 0.0
)

@Keep
@JsonClass(generateAdapter = true)
data class StudentItem(
    val id: Int = 0,
    @NullToEmptyString val nisn: String = "",
    @NullToEmptyString val nis: String = "",
    var name: String = "",
    @NullToEmptyString var place_of_birth: String = "",
    @NullToEmptyString var date_of_birth: String = "",
    @NullToEmptyString var gender: String = "",
    @NullToEmptyString val religion: String = "",
    @NullToEmptyString val blood_type: String = "",
    @NullToEmptyString val height: String = "",
    @NullToEmptyString val weight: String = "",
    @NullToEmptyString val uniform_size: String = "",
    @NullToEmptyString val sport_shirt_size: String = "",
    @NullToEmptyString val previous_school: String = "",
    @NullToEmptyString val previous_class: String = "",
    @NullToEmptyString val class_of: String = "",
    @NullToEmptyString val `class`: String = "",
    @NullToEmptyString val grade_class: String = "",
    val student_class: StudentClass = StudentClass(),
    val user: UserResponseData = UserResponseData()
)

@JsonClass(generateAdapter = true)
data class LoginAccountUserData(
    val id: Int,
    val is_registered_sso: Boolean?,
    val referer: String?,
    val uuid: String,
    val name: String?,
    val user_username: String?,
    val email: String?,
    val nis_nik: String?,
    val gender: String?,
    val user_avatar_image: String?,
    val religion: String?,
    val last_logged_in_at: String?,
    val school: LoginSchoolData?,
    val roles: List<LoginRoleItem>?,
    val is_klaspay_activated: Boolean?,
    val is_email_verified: Boolean?,
    val is_verified: Boolean?,
    val rule_label: String?,
    val rule: LoginRuleData?,
    val meta: LoginMetaData?,
    val product_school: ProductSchoolData?,
    val credential: CredentialData?
)

@JsonClass(generateAdapter = true)
data class LoginSchoolData(
    val id: Int?,
    val uuid: String?,
    val name: String?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class LoginRoleItem(
    val id: Int?,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class LoginRuleData(
    val is_student: Boolean?,
    val is_teacher: Boolean?,
    val is_librarian: Boolean?
)

@JsonClass(generateAdapter = true)
data class LoginMetaData(
    val token: String?
)

@JsonClass(generateAdapter = true)
data class ProductSchoolData(
    val diskola_lite: Boolean?,
    val diskola_pro: Boolean?,
    val klastime: Boolean?
)

@JsonClass(generateAdapter = true)
data class CredentialData(
    val using_default_password: String?
)

@Keep
@JsonClass(generateAdapter = true)
data class PembelajaranSekolahItem(
    val row_id: Int = 0,
    val uuid: String = "",
    val id: String = "",
    val name: String = "",
    val image: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class SessionResponse(val data: List<SessionData> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
@Entity(tableName = "sessions", indices = [Index("id", unique = true)])
data class SessionData(
    @PrimaryKey val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val last_used_at: String = ""
)