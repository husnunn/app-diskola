package id.diskola.app.utils.session

import com.squareup.moshi.JsonClass

/**
 * JSON shapes stored under [SessionKeys.USER_JSON]/[SessionKeys.SCHOOL_JSON]/
 * [SessionKeys.STUDENT_JSON]/[SessionKeys.TEACHER_JSON] — the app's own on-disk cache of the
 * profile, independent of whichever endpoint (check-account/login-account/login-sso) last wrote
 * it. Mirrors the shape of the legacy app's `UserTable`/`SekolahItem`/`StudentItem`/`TeacherItem`
 * Room-adjacent tables closely enough that the same fields are available to Home/Akun screens.
 */
@JsonClass(generateAdapter = true)
data class SessionUser(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val email: String = "",
    val nisnNik: String = "",
    val nisNik: String = "",
    val phone: String = "",
    val avatar: String = "",
    val username: String = "",
    val schoolId: Int = 0,
)

@JsonClass(generateAdapter = true)
data class SessionSchool(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val image: String = "",
    val address: String = "",
    val cityName: String = "",
    val coordinateRadius: String = "50",
    val coordinateLatitude: Double = 0.0,
    val coordinateLongitude: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class SessionStudent(
    val id: Int = 0,
    val nisn: String = "",
    val nis: String = "",
    val name: String = "",
    val className: String = "",
    val classRoomId: Int = 0,
    val studentClassId: Int = 0,
)

@JsonClass(generateAdapter = true)
data class SessionTeacher(
    val id: Int = 0,
    val nik: String = "",
    val name: String = "",
)

/** Legacy "Tidak memiliki kelas" placeholder — doc 02 §4.3, shown whenever the real class name is blank. */
const val NO_CLASS_LABEL = "Tidak memiliki kelas"
