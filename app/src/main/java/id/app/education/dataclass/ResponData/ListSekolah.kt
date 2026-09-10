package id.app.education.dataclass.ResponData

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SchoolListResponse(
    val data: List<SchoolItem>
)

@JsonClass(generateAdapter = true)
data class SchoolItem(
    val id: Int,
    val uuid: String,
    val name: String,
    val address: String?
)

@JsonClass(generateAdapter = true)
data class CheckAccountRequest(
    val school_id: String,
    val nisn_nik: String
)

@JsonClass(generateAdapter = true)
data class CheckAccountApiResponse(
    val data: CheckAccountUserData?,
    val is_klaspay_activated: Boolean?,
    val is_email_verified: Boolean?,
    val is_verified: Boolean?,
    val rule_label: String?,
    val rule: RuleData?,
    val is_active: Boolean?,
    val date: String?,
    val time: String?
)

@JsonClass(generateAdapter = true)
data class CheckAccountUserData(
    val id: Int,
    val is_registered_sso: Boolean?,
    val uuid: String,
    val name: String?,
    val email: String?,
    val nis_nik: String?,
    val place_of_birth: String?,
    val date_of_birth: String?,
    val gender: String?,
    val user_avatar_image: String?,
    val religion: String?,
    val address: String?,
    val school: SchoolDetail?,
    val roles: List<RoleItem>?,
    val student: StudentData?,
    val teacher: TeacherData?
)

@JsonClass(generateAdapter = true)
data class SchoolDetail(
    val id: Int,
    val uuid: String,
    val name: String?,
    val image: String?,
    val address: String?,
    val coordinate_latitude: Double?,
    val coordinate_longitude: Double?,
    val coordinate_radius: String?
)

@JsonClass(generateAdapter = true)
data class RoleItem(
    val id: Int,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class RuleData(
    val is_student: Boolean?,
    val is_teacher: Boolean?,
    val is_librarian: Boolean?
)

@JsonClass(generateAdapter = true)
data class StudentData(
    val id: Int?,
    val nisn: String?,
    val nis: String?,
    val name: String?,
    val place_of_birth: String?,
    val date_of_birth: String?,
    val gender: String?,
    val religion: String?,
    @com.squareup.moshi.Json(name = "class")
    val className: String?,
    val user: StudentUserData?,
    val student_class: StudentClassData?
)

@JsonClass(generateAdapter = true)
data class StudentUserData(
    val id: Int?,
    val uuid: String?,
    val name: String?,
    val email: String?,
    val nis_nik: String?,
    val place_of_birth: String?,
    val date_of_birth: String?,
    val gender: String?,
    val user_avatar_image: String?,
    val religion: String?,
    val address: String?
)

@JsonClass(generateAdapter = true)
data class StudentClassData(
    val id: Int?,
    val class_room: ClassRoomData?
)

@JsonClass(generateAdapter = true)
data class ClassRoomData(
    val id: Int?,
    val name: String?,
    val grade: Int?,
    val active: Boolean?
)

@JsonClass(generateAdapter = true)
data class TeacherData(
    val id: Int? = null
)




