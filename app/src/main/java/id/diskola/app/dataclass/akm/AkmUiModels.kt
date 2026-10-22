package id.diskola.app.dataclass.akm

import id.diskola.app.dataclass.ResponData.AkmAnswerData
import id.diskola.app.dataclass.ResponData.AkmExamData
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmQuestionData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.resolveAssetUrl
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Client-local lifecycle of one assessment on this device. Only [BelumSinkron]/[MengunduhSoal]/
 * [SiapDikerjakan] are device state (whether the question set has been downloaded here) — the
 * rest are read straight from the schedule's `isDone`/`isAssessed`/`isQueued` server flags, per
 * the docs under `docs/repo lama/asesmen/`.
 */
enum class AkmStatus(val label: String) {
    BelumSinkron("Belum sinkron"),
    MengunduhSoal("Mengunduh soal"),
    SiapDikerjakan("Siap dikerjakan"),
    /** Submit requested but the server has not confirmed it yet (offline queue / auto-submit pending) — legacy `FINISHED`. */
    ProsesUpload("Proses upload jawaban"),
    SudahDikumpulkan("Sudah dikumpulkan"),
    MenungguPenilaian("Menunggu penilaian"),
    SudahDinilai("Sudah dinilai"),
    /** Labels below are the server's own `status` strings, kept verbatim. */
    TidakMengerjakan("Tidak Mengerjakan"),
    SesiTerlewat("Sesi Terlewat"),
}

data class AkmItem(
    val id: Int,
    val name: String,
    val endLabel: String,
    /** Calendar day-of-month [startAtMillis] falls on — the date strip filters on this. */
    val endDay: Int,
    val questionCount: Int,
    val needsPassword: Boolean,
    /** The server says this student already passed the password dialog (`password_checked`); the device's own record and the session marker refine it. */
    val passwordChecked: Boolean = false,
    /** First non-blank `exams[].assessment_category`, shown as the "Kategori" participant row. */
    val category: String,
    /** First non-blank `exams[].assessment_period`, shown as the "Periode" participant row. */
    val period: String,
    /** Epoch millis of `date`+`startAt`, null when the server sent none — a proctor "opening" a session isn't a real field; the
     * legacy app itself gates "Mulai" on `Date() > date_start`, so we do the same. */
    val startAtMillis: Long?,
    /** Epoch millis of `date`+`endAt`, null when unreadable (then nothing is auto-submitted on a guess, see `AkmRules.deadlineAction`). */
    val endAtMillis: Long?,
    val showScore: Boolean,
    val hasExplain: Boolean?,
    /** The "Dibatalkan" mark: label on the card, status row + stamp on the detail (doc 06 §4.4). Visual only. */
    val watermark: AkmRules.AkmWatermark = AkmRules.AkmWatermark.None,
) {
    fun hasStarted(now: Long = System.currentTimeMillis()) = startAtMillis?.let { now >= it } ?: true
    fun hasEnded(now: Long = System.currentTimeMillis()) = AkmRules.isTimeUp(now, endAtMillis)
}

data class AkmScoreItem(
    val id: Int,
    val name: String,
    val dateLabel: String,
    val score: Int,
    val status: AkmStatus,
    val hasExplanation: Boolean,
    /** The server's own `status` text ("Tidak Mengerjakan", "Sesi Terlewat", …): decides whether a re-upload is offered. */
    val scoreStatus: String = "",
    /** Same rows as the detail screen's participant table (doc 06 §14: "info seperti detail"). */
    val category: String = "",
    val period: String = "",
    val watermark: AkmRules.AkmWatermark = AkmRules.AkmWatermark.None,
)

data class AkmInstruction(
    val id: Int,
    val name: String,
    val total: Int,
    val answered: Int,
)

data class AkmExam(
    val id: Int,
    val name: String,
    val questionCount: Int,
    val instructions: List<AkmInstruction>,
)

enum class QuestionType(val label: String) {
    PilihanGanda("Pilihan ganda"),
    BenarSalah("Benar / salah"),
    MultiJawaban("Pilihan ganda multi jawaban"),
    EsaiSingkat("Esai singkat"),
    /** `STATEMENT` with `answers[0].showFalse == true` — one radio Benar/Salah per row. */
    TabelPernyataan("Tabel pernyataan"),
    /** `PAIR` — text or image variant, see [AkmQuestion.pairIsImage]. */
    Menjodohkan("Menjodohkan"),
}

data class AkmOption(val id: Int, val text: String, val imageUrl: String, val isTrue: Boolean)

/** `type` is "audio" or (anything else, treated as) "video" — same binary split as the legacy
 * `AkmQuestionMediaBinder.bind()`, which is the only place this list is ever branched on. */
data class AkmMedia(val id: Int, val type: String, val url: String, val localPath: String = "")

/**
 * One row of a Menjodohkan (pair-matching) question. `first*` is the fixed left-side anchor
 * (never changes); `current*`/`currentSelectedId` is the swappable right side, initialized from
 * the row's own `secondStatement`/`secondFilePath`/`selected_id` and updated by
 * [AkmViewModel.swapPair] — mirrors `QuestionPairVh.kt`/`QuestionPairImageVh.kt`'s drag-drop swap,
 * which only ever exchanges the right-side content and `selected_id` between two rows.
 */
data class AkmPairSlot(
    val id: Int,
    val label: String,
    val firstText: String,
    val firstImageUrl: String,
    val currentSecondText: String,
    val currentSecondImageUrl: String,
    val currentSelectedId: Int,
)

data class AkmQuestion(
    val id: Int,
    val type: QuestionType,
    /** The exact API `answerType` string — needed verbatim to build the submit payload. */
    val rawAnswerType: String,
    val instructionId: Int,
    val text: String,
    val imageUrl: String,
    val options: List<AkmOption> = emptyList(),
    val media: List<AkmMedia> = emptyList(),
    /** Only populated for [QuestionType.Menjodohkan]. */
    val pairSlots: List<AkmPairSlot> = emptyList(),
    /** `true` for the image-pairing variant (`QuestionPairImageVh`), `false` for text (`QuestionPairVh`) —
     * same test as the legacy app: `answers[0].firstStatement` blank means image-based pairing. */
    val pairIsImage: Boolean = false,
)

data class AkmSubtestScore(val label: String, val score: Int)

// SimpleDateFormat is not thread-safe and caches the time zone it was created in: build one per use.
private fun dateFormat(pattern: String) = SimpleDateFormat(pattern, Locale("id", "ID"))

/** Ported from `OnKlasDbUtil.processAkmResponse()` — same field derivation as the legacy AkmTable. */
fun AkmScheduleData.toAkmItem(known: AkmRules.AkmWatermark? = null): AkmItem {
    val examList = exams ?: listOfNotNull(exam)
    val name = examList.distinctBy { it.name }.joinToString(", ") { it.name }.ifBlank { "Asesmen" }
    val questionCount = examList.sumOf { it.numberOfQuestions }
    val startMillis = AkmRules.parseDateTime(date, startAt)
    val endMillis = AkmRules.parseDateTime(date, endAt)
    val endLabel = endMillis?.let { runCatching { dateFormat("d MMM yyyy · HH.mm").format(it) }.getOrNull() } ?: "$date $endAt".trim()
    val endDay = (startMillis ?: endMillis)?.let { runCatching { dateFormat("d").format(it).toInt() }.getOrNull() } ?: 0

    return AkmItem(
        id = id,
        name = name,
        endLabel = endLabel,
        endDay = endDay,
        questionCount = questionCount,
        needsPassword = requires_password,
        passwordChecked = password_checked,
        // Docs (`asesmen-akm-daftar-detail-nilai.md:224-225`): first exam carrying a non-blank
        // value wins. The legacy fallback to a previously cached value has no equivalent here —
        // there is no Room cache — so a blank stays blank and renders as "-".
        category = examList.firstOrNull { it.assessment_category.isNotBlank() }?.assessment_category.orEmpty(),
        period = examList.firstOrNull { it.assessment_period.isNotBlank() }?.assessment_period.orEmpty(),
        startAtMillis = startMillis,
        endAtMillis = endMillis,
        showScore = show_score,
        hasExplain = hasExplanation,
        watermark = AkmRules.watermark(watermark_bool, watermark_label, known),
    )
}

fun AkmScheduleData.toAkmScoreItem(status: AkmStatus, known: AkmRules.AkmWatermark? = null): AkmScoreItem {
    val examList = exams ?: listOfNotNull(exam)
    val name = examList.distinctBy { it.name }.joinToString(", ") { it.name }.ifBlank { "Asesmen" }
    val dateMillis = AkmRules.parseDateTime(date, endAt)
    val dateLabel = dateMillis?.let { runCatching { dateFormat("d MMM yyyy").format(it) }.getOrNull() } ?: date
    val score = (exam_score?.takeIf { it >= 0 } ?: examList.firstOrNull()?.score ?: 0.0).toInt()

    return AkmScoreItem(
        id = id,
        name = name,
        dateLabel = dateLabel,
        score = score,
        status = status,
        hasExplanation = hasExplanation == true,
        scoreStatus = this@toAkmScoreItem.status,
        category = examList.firstOrNull { it.assessment_category.isNotBlank() }?.assessment_category.orEmpty(),
        period = examList.firstOrNull { it.assessment_period.isNotBlank() }?.assessment_period.orEmpty(),
        watermark = AkmRules.watermark(watermark_bool, watermark_label, known),
    )
}

fun AkmScheduleData.toSubtestScores(): List<AkmSubtestScore> =
    (exams ?: listOfNotNull(exam)).map { AkmSubtestScore(it.name, it.score.toInt()) }

/**
 * "Answered" isn't a server field on [AkmQuestionData] — it's purely local state (docs:
 * `AkmQuestionTable.answered` is set by the app when the student picks an answer), so the caller
 * supplies the live per-question answered set from the ViewModel's own answer map.
 */
fun AkmScheduleData.toAkmExams(answeredQuestionIds: Set<Int>): List<AkmExam> =
    (exams ?: listOfNotNull(exam)).map { it.toAkmExam(answeredQuestionIds) }

fun AkmExamData.toAkmExam(answeredQuestionIds: Set<Int>): AkmExam = AkmExam(
    id = id,
    name = name,
    questionCount = numberOfQuestions,
    instructions = instructions.map { it.toAkmInstruction(answeredQuestionIds) },
)

fun AkmInstructionData.toAkmInstruction(answeredQuestionIds: Set<Int>): AkmInstruction = AkmInstruction(
    id = id,
    name = instruction.ifBlank { "Instruksi" },
    total = questions.size,
    answered = questions.count { it.id in answeredQuestionIds },
)

fun AkmQuestionData.toQuestionType(): QuestionType = when (answerType) {
    "MULTIPLE CHOICE" -> QuestionType.PilihanGanda
    "STATEMENT" -> when {
        // doc 06 §4.3: the table flag decides first — a one-row table is still a table, not a single Benar/Salah.
        answers.firstOrNull()?.showFalse == true -> QuestionType.TabelPernyataan
        answers.size == 1 -> QuestionType.BenarSalah
        else -> QuestionType.MultiJawaban
    }
    "PAIR" -> QuestionType.Menjodohkan
    // ESSAY, SHORT_ESSAY_NUM, SHORT_ESSAY_WORD and any type this version does not know: legacy renders them as an essay box,
    // so the student can still answer and the "Kumpulkan" button is never blocked by one unsupported question.
    else -> QuestionType.EsaiSingkat
}

/**
 * Options keep the order stored in the downloaded package: an `isRandom` instruction is shuffled once by `AkmDownloadWorker` and
 * persisted, so the order shown is the order stored and nothing here reshuffles it.
 */
fun AkmQuestionData.toAkmQuestion(instructionId: Int): AkmQuestion {
    val type = toQuestionType()
    val orderedAnswers = answers
    val options = when (type) {
        QuestionType.BenarSalah -> listOf(
            AkmOption(id = orderedAnswers.getOrNull(0)?.id ?: 0, text = "Benar", imageUrl = "", isTrue = orderedAnswers.getOrNull(0)?.isTrue == true),
            AkmOption(id = orderedAnswers.getOrNull(0)?.id ?: 0, text = "Salah", imageUrl = "", isTrue = orderedAnswers.getOrNull(0)?.isTrue == false),
        )
        else -> orderedAnswers.map { AkmOption(id = it.id, text = it.answer, imageUrl = resolveAssetUrl(it.filePath), isTrue = it.isTrue) }
    }
    val pairIsImage = type == QuestionType.Menjodohkan && orderedAnswers.firstOrNull()?.firstStatement.isNullOrBlank()
    val pairSlots = if (type == QuestionType.Menjodohkan) {
        val alphabet = ('A'..'Z').toList()
        orderedAnswers.mapIndexed { index, row ->
            AkmPairSlot(
                id = row.id,
                label = alphabet.getOrElse(index) { '?' }.toString(),
                firstText = row.firstStatement,
                firstImageUrl = resolveAssetUrl(row.firstFilePath),
                currentSecondText = row.secondStatement,
                currentSecondImageUrl = resolveAssetUrl(row.secondFilePath),
                currentSelectedId = row.selected_id,
            )
        }
    } else {
        emptyList()
    }
    return AkmQuestion(
        id = id,
        type = type,
        rawAnswerType = answerType,
        instructionId = instructionId,
        text = question,
        imageUrl = resolveAssetUrl(image),
        options = options,
        media = media.sortedBy { it.sequence }.map { AkmMedia(id = it.id, type = it.type, url = resolveAssetUrl(it.url), localPath = it.localPath) },
        pairSlots = pairSlots,
        pairIsImage = pairIsImage,
    )
}

/**
 * An answer to one question, keyed by **answer ids** (not by position on screen) so it stays valid whatever order the options are
 * drawn in and survives a restart — it is what `akm_answer` persists ([AkmAnswerCodec]).
 */
sealed interface AkmPlayerAnswer {
    /** Pilihan ganda: the chosen option's answer id. */
    data class Choice(val answerId: Int) : AkmPlayerAnswer

    /** Benar/Salah over the single statement row: [isTrue] = "Benar" was picked. */
    data class TrueFalse(val isTrue: Boolean) : AkmPlayerAnswer

    /** Pilihan ganda multi jawaban: the ticked options' answer ids. */
    data class MultiChoice(val answerIds: Set<Int>) : AkmPlayerAnswer

    /** Esai / isian — an empty text still counts as answered, like legacy (`QuestionEssayVh`, doc 06 §7.1). */
    data class Essay(val text: String) : AkmPlayerAnswer

    /** Tabel pernyataan: row id -> Benar (true) / Salah (false); only rows the student touched. */
    data class TableAnswer(val rows: Map<Int, Boolean>) : AkmPlayerAnswer

    /** Menjodohkan: row id -> `selected_id` of the item currently paired with it, for **every** row. */
    data class PairAnswer(val arrangement: Map<Int, Int>) : AkmPlayerAnswer
}
