package id.diskola.app.dataclass.ResponData

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString
import id.diskola.app.di.module.ObjectToList

/**
 * Field names below mirror the docs under `docs/repo lama/asesmen/` — a different Diskola codebase's
 * confirmed production behavior, not a verified contract for this project's backend. Only
 * `mobile/setting-akm` (-> [AkmSettingResponse]) is independently confirmed real; everything else
 * is the best-documented starting shape, to be corrected against actual server responses.
 */

@Keep
@JsonClass(generateAdapter = true)
data class AkmScheduleResponse(val data: List<AkmScheduleData> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class AkmScheduleDetailResponse(val data: AkmScheduleData? = null)

@Keep
@JsonClass(generateAdapter = true)
data class AkmScheduleData(
    val id: Int = 0,
    val level: Int = 0,
    val isActive: Int = 0,
    @NullToEmptyString val password: String = "",
    @NullToEmptyString val date: String = "",
    @NullToEmptyString val startAt: String = "",
    @NullToEmptyString val endAt: String = "",
    @NullToEmptyString val status: String = "",
    val show_score: Boolean = true,
    val requires_password: Boolean = false,
    val password_checked: Boolean = false,
    val has_explain: Boolean? = null,
    val isDone: Boolean = false,
    val isAssessed: Boolean = false,
    val isQueued: Boolean = false,
    val gov_schedule: Boolean = false,
    val exam: AkmExamData? = null,
    // Backend sometimes sends a single exam as a JSON object instead of a one-element array —
    // confirmed live (Moshi crash: "Expected BEGIN_ARRAY but was BEGIN_OBJECT at path $.data.exams").
    // Same quirk the legacy app hits (`@ObjectToList`, `docs/repo lama/asesmen/asesmen-teknis-download-storage.md` §2.1).
    @ObjectToList val exams: List<AkmExamData>? = null,
    // Only present on the download-soal response (confirmed by reading the legacy source directly,
    // `android-portal/.../pages/akm/AkmModels.kt:133-141` — `AkmDownloadData.studentExam`). This is
    // the exam SESSION id the `{student_id}` path segment of submit/upload actually expects — NOT
    // the logged-in account's own user id, which was wrongly used here before this was verified.
    val studentExam: AkmStudentExamData? = null,
    val score: List<AkmScoreData> = emptyList(),
    val exam_score: Double? = -1.0,
    @NullToEmptyString val type: String = "",
)

/** Ported verbatim from the legacy `StudentExam` model — `id` is the exam session id, `student_id`
 * is unused here (legacy app doesn't read it either). */
@Keep
@JsonClass(generateAdapter = true)
data class AkmStudentExamData(
    val id: Int = 0,
    val student_id: Int = 0,
)

@Keep
@JsonClass(generateAdapter = true)
data class AkmExamData(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val type: String = "",
    val numberOfQuestions: Int = 0,
    val instructions: List<AkmInstructionData> = emptyList(),
    val score: Double = 0.0,
    @NullToEmptyString val assessment_category: String = "",
    @NullToEmptyString val assessment_period: String = "",
)

/** Matches to `exams[].score` by `id == exams[].id` on the scored endpoints. */
@Keep
@JsonClass(generateAdapter = true)
data class AkmScoreData(
    val id: Int = 0,
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val type: String = "",
    val scored: Double = 0.0,
)

@Keep
@JsonClass(generateAdapter = true)
data class AkmSettingResponse(val data: AkmSettingData? = null)

@Keep
@JsonClass(generateAdapter = true)
data class AkmSettingData(
    val penalty_times: Int = 0,
    val penalty_applied: Boolean = false,
    val absence_setting: Boolean = false,
    val exam_lock_mode: Boolean? = true,
)

@Keep
@JsonClass(generateAdapter = true)
data class ExamPasswordCheckResponse(val data: ExamPasswordCheckData? = null)

@Keep
@JsonClass(generateAdapter = true)
data class ExamPasswordCheckData(
    val checked: Boolean = false,
    val requires_password: Boolean = false,
)

@Keep
@JsonClass(generateAdapter = true)
data class AkmInstructionData(
    val id: Int = 0,
    @NullToEmptyString val instruction: String = "",
    @NullToEmptyString val description: String = "",
    val sequence: Int = 0,
    val isRandom: Boolean = false,
    val questions: List<AkmQuestionData> = emptyList(),
)

/** `answerType` decides which UI widget renders this question — see `QuestionType.from()`. */
@Keep
@JsonClass(generateAdapter = true)
data class AkmQuestionData(
    val id: Int = 0,
    @NullToEmptyString val question: String = "",
    @NullToEmptyString val image: String = "",
    @NullToEmptyString val answerType: String = "",
    val answers: List<AkmAnswerData> = emptyList(),
    val media: List<AkmQuestionMediaData> = emptyList(),
)

@Keep
@JsonClass(generateAdapter = true)
data class AkmAnswerData(
    val id: Int = 0,
    @NullToEmptyString val answer: String = "",
    @NullToEmptyString val filePath: String = "",
    val isTrue: Boolean = false,
    val showFalse: Boolean = false,
    @NullToEmptyString val firstStatement: String = "",
    @NullToEmptyString val firstFilePath: String = "",
    @NullToEmptyString val secondStatement: String = "",
    @NullToEmptyString val secondFilePath: String = "",
    val selected_id: Int = 0,
)

@Keep
@JsonClass(generateAdapter = true)
data class AkmQuestionMediaData(
    val id: Int = 0,
    @NullToEmptyString val type: String = "",
    @NullToEmptyString val url: String = "",
    val sequence: Int = 0,
    /** Local cache path after `AkmDownloadWorker` runs — never sent/received from the server. */
    val localPath: String = "",
)

/**
 * Confirmed real contract (`docs/repo lama/api/04-pembayaran-klaspay-ppob-akm.md:832-858`,
 * `GET .../exam-schedules-scored/{id}/explains`) — one row per question, each carrying its own
 * `file_path` (URL to the explanation asset), not a single combined document.
 */
@Keep
@JsonClass(generateAdapter = true)
data class AkmExplanationResponse(val data: List<AkmExplanationData> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class AkmExplanationData(
    val id: Int = 0,
    @NullToEmptyString val file_path: String = "",
    @NullToEmptyString val created_at: String = "",
    @NullToEmptyString val updated_at: String = "",
    val question: AkmQuestionData = AkmQuestionData(),
)
