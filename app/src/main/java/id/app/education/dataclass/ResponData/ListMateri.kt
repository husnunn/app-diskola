package id.app.education.dataclass.ResponData

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ListMateriResponse(
    val id: Int,
    val name : String,
    val icon_image : String
)


@JsonClass(generateAdapter = true)
data class TheoryListResponse(
    val data: List<TheoryItem>
)

@JsonClass(generateAdapter = true)
data class TheoryItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val uri: TheoryUriWrapper? = null,
    val file_name: String? = null,
    val file_path: String? = null,
    val file_type: String? = null,
    val file_size: String? = null,
    val file_edited: String? = null,
    val is_explained: Boolean? = null,
    val explanations: Map<String, Any?>? = null,
    val is_explanation: Boolean? = null,
    val grade: Int? = null,
    val subject: SubjectItem? = null,
    val school_class: SchoolClassItem? = null,
    val school_major: SchoolMajorItem? = null,
    val teacher: TheoryTeacherItem? = null,
    val created_at_label: String? = null
)

@JsonClass(generateAdapter = true)
data class TheoryUriWrapper(
    val link: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SubjectItem(
    val id: Int,
    val name: String?,
    val icon_image: String? = null
)

@JsonClass(generateAdapter = true)
data class SchoolClassItem(
    val id: Int,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class SchoolMajorItem(
    val id: Int,
    val name: String?,
    val grade: Int? = null
)

@JsonClass(generateAdapter = true)
data class TheoryTeacherItem(
    val id: Int,
    val name: String?,
    val nip: String? = null,
    val user: TeacherUserItem? = null
)

@JsonClass(generateAdapter = true)
data class TeacherUserItem(
    val id: Int,
    val uuid: String?,
    val name: String?,
    val user_username: String? = null,
    val email: String? = null,
    val nis_nik: String? = null,
    val phone: String? = null,
    val user_avatar_image: String? = null
)