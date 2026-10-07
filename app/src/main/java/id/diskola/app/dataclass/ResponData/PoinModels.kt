package id.diskola.app.dataclass.ResponData

import androidx.room.Entity
import androidx.room.Index
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import id.diskola.app.di.module.NullToEmptyString

// ---- Master lists: `mobile/konseling/list-violation|list-achievement|list-handling` ----

@JsonClass(generateAdapter = true)
data class ListViolationResponse(val data: List<ViolationItem> = emptyList())

@JsonClass(generateAdapter = true)
data class ListAchievementResponse(val data: List<AchievementItem> = emptyList())

@JsonClass(generateAdapter = true)
data class ListHandlingResponse(val data: List<HandlingItem> = emptyList())

/** `violation_score` is a String on the list endpoints (legacy `PoinModels.kt:159-163`), unlike the
 * Int on `score-student` — Moshi's String adapter also accepts a bare JSON number. */
@JsonClass(generateAdapter = true)
data class ViolationItem(
    val violation_id: Int = 0,
    @NullToEmptyString val violation_name: String = "",
    @NullToEmptyString val violation_score: String = "",
)

@JsonClass(generateAdapter = true)
data class AchievementItem(
    val achievement_id: Int = 0,
    @NullToEmptyString val achievement_name: String = "",
    @NullToEmptyString val achievement_score: String = "",
)

@JsonClass(generateAdapter = true)
data class HandlingItem(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
)

/** Local cache of the three master lists (violation/achievement/handling "jenis"). The type id is
 * unique only within its [poinType] slot, hence the composite key. */
@Entity(
    tableName = "poin_item",
    primaryKeys = ["id", "poin_type"],
    indices = [Index("poin_type")],
)
data class PoinItemTable(
    val id: Int = 0,
    val poin_type: Int = 0,
    val name: String = "",
    val score: String = "",
) {
    companion object {
        const val TYPE_VIOLATION = 1
        const val TYPE_ACHIEVEMENT = 2
        const val TYPE_HANDLING = 3

        fun from(item: ViolationItem) = PoinItemTable(item.violation_id, TYPE_VIOLATION, item.violation_name, item.violation_score)
        fun from(item: AchievementItem) = PoinItemTable(item.achievement_id, TYPE_ACHIEVEMENT, item.achievement_name, item.achievement_score)
        fun from(item: HandlingItem) = PoinItemTable(item.id, TYPE_HANDLING, item.name, "")
    }
}

// ---- Teacher: student search `mobile/konseling/score` (name= / nisn=, not paged) ----

@JsonClass(generateAdapter = true)
data class SearchPoinStudentResponse(val data: List<SearchPoinStudentItem> = emptyList())

@JsonClass(generateAdapter = true)
data class SearchPoinStudentItem(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val user_avatar_image: String = "",
    @NullToEmptyString val role: String = "",
    @NullToEmptyString val student_class: String = "",
    @NullToEmptyString val nisn: String = "",
    val violation_score: Int? = null,
    val achievement_score: Int? = null,
)

// ---- Student: `mobile/konseling/score-student` (no params; student comes from the token) ----

@JsonClass(generateAdapter = true)
data class StudentPoinListResponse(val data: List<StudentPoinItem>? = emptyList())

@JsonClass(generateAdapter = true)
data class StudentPoinItem(
    val violation: StudentPoinViolation? = null,
    val achievement: StudentPoinAchievement? = null,
    val notHandled: List<StudentPoinHandling>? = null,
)

@JsonClass(generateAdapter = true)
data class StudentPoinViolation(
    val violation_recap: StudentViolationRecap? = null,
    val violation_detail: List<StudentViolationDetail>? = null,
)

@JsonClass(generateAdapter = true)
data class StudentViolationRecap(
    val violation_score: Int? = null,
    val recap: List<ViolationRecapRow>? = null,
)

@JsonClass(generateAdapter = true)
data class ViolationRecapRow(
    @NullToEmptyString val name_of_violation: String = "",
    val number_of_violations: Int? = null,
    val number_of_scores: Int? = null,
)

@JsonClass(generateAdapter = true)
data class StudentViolationDetail(
    val violation_id: Int = 0,
    @NullToEmptyString val violation_name: String = "",
    val violation_score: Int? = null,
    @NullToEmptyString val violation_image: String = "",
    @NullToEmptyString val violation_message: String = "",
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val time: String = "",
    @NullToEmptyString val creator_name: String = "",
    @NullToEmptyString val creator_nip: String = "",
)

@JsonClass(generateAdapter = true)
data class StudentPoinAchievement(
    val achievement_recap: StudentAchievementRecap? = null,
    val achievement_detail: List<StudentAchievementDetail>? = null,
)

@JsonClass(generateAdapter = true)
data class StudentAchievementRecap(
    val achievement_score: Int? = null,
    val recap: List<AchievementRecapRow>? = null,
)

@JsonClass(generateAdapter = true)
data class AchievementRecapRow(
    @NullToEmptyString val name_of_achievement: String = "",
    val number_of_achievements: Int? = null,
    val number_of_scores: Int? = null,
)

@JsonClass(generateAdapter = true)
data class StudentAchievementDetail(
    val achievement_id: Int = 0,
    @NullToEmptyString val achievement_name: String = "",
    val achievement_score: Int? = null,
    @NullToEmptyString val achievement_image: String = "",
    @NullToEmptyString val achievement_message: String = "",
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val time: String = "",
    @NullToEmptyString val creator_name: String = "",
    @NullToEmptyString val creator_nip: String = "",
)

@JsonClass(generateAdapter = true)
data class StudentPoinHandling(
    val id: Int = 0,
    val score_student_id: Int = 0,
    val violation_score: Int? = null,
    val school_handling_id: Int? = null,
    @NullToEmptyString val calling_at: String = "",
    @NullToEmptyString val calling_name: String = "",
    @NullToEmptyString val calling_message: String = "",
    @NullToEmptyString val handling_at: String = "",
    @NullToEmptyString val handling_message: String = "",
    val handled: Int? = null,
)

/** Which of the three teacher forms is open (route argument + picker list selector). */
@Serializable
enum class PoinFormMode(val poinType: Int) {
    VIOLATION(PoinItemTable.TYPE_VIOLATION),
    ACHIEVEMENT(PoinItemTable.TYPE_ACHIEVEMENT),
    HANDLING(PoinItemTable.TYPE_HANDLING),
}
