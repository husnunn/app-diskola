package id.diskola.app.dataclass.ResponData

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val email: String,
    val username: String,
    val password: String
)