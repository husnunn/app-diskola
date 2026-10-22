package id.diskola.app.dataclass.ResponData

import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString

// Data behind the Pembelajaran hub (legacy `PembelajaranPage`): the class running right now, the account's
// verification progress, the session guard, the bell badge and today's attendance setting. Shapes follow the legacy
// models (`LoginModels.kt`, `VerificationModels.kt`, `PresensiModel.kt`, `KlaspayAktivasiModels.kt`) but every field is
// lenient — one JSON `null` must not fail the whole check-account response.

/** `check-account.current_subject` — the class the server says is in session for this student (null otherwise). */
@JsonClass(generateAdapter = true)
data class CurrentSubjectData(
    val id: Int? = null,
    val attendance_id: Int? = null,
    val user_id: Int? = null,
    @NullToEmptyString val plot_start_at: String = "",
    @NullToEmptyString val plot_end_at: String = "",
    @NullToEmptyString val late_at: String = "",
    @NullToEmptyString val teacher_name: String = "",
    @NullToEmptyString val teacher_nip: String = "",
    @NullToEmptyString val subject_name: String = "",
    @NullToEmptyString val subject_icon_image: String = "",
    val status: String? = null,
)

/** `check-account.userProgress` — data-verification request state: "" (clear) / approved / in_review / rejected. */
@JsonClass(generateAdapter = true)
data class UserProgressData(
    val requested_user_id: Int? = null,
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val message: String = "",
) {
    companion object {
        const val APPROVED = "approved"
        const val IN_REVIEW = "in_review"
        const val REJECTED = "rejected"
    }
}

/** `GET mobile/app/authentication/current-user` — session guard (single-device login). */
@JsonClass(generateAdapter = true)
data class CurrentUserResponse(
    val id: Int = 0,
    @NullToEmptyString val uuid: String = "",
    /** Null for tokens issued before one-account-one-device existed: never treat that as a conflict. */
    val device_id: String? = null,
    val single_device_login_enabled: Boolean = false,
)


/** `GET mobile/notification/summary?wallet_id=` */
@JsonClass(generateAdapter = true)
data class NotificationSummaryResponse(val data: NotificationSummaryData? = null)

@JsonClass(generateAdapter = true)
data class NotificationSummaryData(val count_unread: Int? = null)

/** `GET mobile/attendance/setting/me/today` — what kind of day it is for the signed-in staff member. */
@JsonClass(generateAdapter = true)
data class TodaySettingResponse(val data: TodaySettingData? = null)

@JsonClass(generateAdapter = true)
data class TodaySettingData(
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val day: String = "",
    /** "hari_kerja", "holiday", "no_agenda" … — only the last two change what the banner says. */
    @NullToEmptyString val status: String = "",
    val jam_masuk: String? = null,
    val events: List<TodaySettingEvent>? = null,
)

@JsonClass(generateAdapter = true)
data class TodaySettingEvent(
    val name: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val day_off: Boolean? = null,
)
