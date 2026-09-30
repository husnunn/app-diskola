package id.diskola.app.dataclass.akm

import id.diskola.app.dataclass.ResponData.AkmAnswerData
import id.diskola.app.dataclass.ResponData.AkmExamData
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmQuestionData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
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
    /** First non-blank `exams[].assessment_category`, shown as the "Kategori" participant row. */
    val category: String,
    /** First non-blank `exams[].assessment_period`, shown as the "Periode" participant row. */
    val period: String,
    /** Epoch millis of `date`+`startAt` — a proctor "opening" a session isn't a real field; the
     * legacy app itself gates "Mulai" on `Date() > date_start`, so we do the same. */
    val startAtMillis: Long,
    /** Epoch millis of `date`+`endAt` — once passed, the schedule stops being an active exam to
     * join (it moves to the Nilai tab instead, driven by the scored endpoint). */
    val endAtMillis: Long,
    val showScore: Boolean,
    val hasExplain: Boolean?,
) {
    fun hasStarted(now: Long = System.currentTimeMillis()) = now >= startAtMillis
    fun hasEnded(now: Long = System.currentTimeMillis()) = now >= endAtMillis
}

data class AkmScoreItem(
    val id: Int,
    val name: String,
    val dateLabel: String,
    val score: Int,
    val status: AkmStatus,
    val hasExplanation: Boolean,
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
    TidakDidukung("Tipe soal belum didukung"),
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

private val API_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID"))
private val DAY_MONTH_YEAR_TIME = SimpleDateFormat("d MMM yyyy · HH.mm", Locale("id", "ID"))

/** Ported from `OnKlasDbUtil.processAkmResponse()` — same field derivation as the legacy AkmTable. */
fun AkmScheduleData.toAkmItem(): AkmItem {
    val examList = exams ?: listOfNotNull(exam)
    val name = examList.distinctBy { it.name }.joinToString(", ") { it.name }.ifBlank { "Asesmen" }
    val questionCount = examList.sumOf { it.numberOfQuestions }
    val startMillis = runCatching { API_DATE_FORMAT.parse("$date $startAt")?.time }.getOrNull() ?: 0L
    val endMillis = runCatching { API_DATE_FORMAT.parse("$date $endAt")?.time }.getOrNull() ?: startMillis
    val endLabel = runCatching { DAY_MONTH_YEAR_TIME.format(endMillis) }.getOrDefault("$date $endAt")
    val endDay = runCatching { SimpleDateFormat("d", Locale("id", "ID")).format(startMillis).toInt() }.getOrDefault(0)

    return AkmItem(
        id = id,
        name = name,
        endLabel = endLabel,
        endDay = endDay,
        questionCount = questionCount,
        needsPassword = requires_password,
        // Docs (`asesmen-akm-daftar-detail-nilai.md:224-225`): first exam carrying a non-blank
        // value wins. The legacy fallback to a previously cached value has no equivalent here —
        // there is no Room cache — so a blank stays blank and renders as "-".
        category = examList.firstOrNull { it.assessment_category.isNotBlank() }?.assessment_category.orEmpty(),
        period = examList.firstOrNull { it.assessment_period.isNotBlank() }?.assessment_period.orEmpty(),
        startAtMillis = startMillis,
        endAtMillis = endMillis,
        showScore = show_score,
        hasExplain = has_explain,
    )
}

fun AkmScheduleData.toAkmScoreItem(status: AkmStatus): AkmScoreItem {
    val examList = exams ?: listOfNotNull(exam)
    val name = examList.distinctBy { it.name }.joinToString(", ") { it.name }.ifBlank { "Asesmen" }
    val dateMillis = runCatching { API_DATE_FORMAT.parse("$date $endAt")?.time }.getOrNull()
    val dateLabel = dateMillis?.let { runCatching { SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(it) }.getOrNull() } ?: date
    val score = (exam_score?.takeIf { it >= 0 } ?: examList.firstOrNull()?.score ?: 0.0).toInt()

    return AkmScoreItem(
        id = id,
        name = name,
        dateLabel = dateLabel,
        score = score,
        status = status,
        hasExplanation = has_explain == true,
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
        answers.size == 1 -> QuestionType.BenarSalah
        answers.firstOrNull()?.showFalse == true -> QuestionType.TabelPernyataan
        else -> QuestionType.MultiJawaban
    }
    "ESSAY", "SHORT_ESSAY_NUM", "SHORT_ESSAY_WORD" -> QuestionType.EsaiSingkat
    "PAIR" -> QuestionType.Menjodohkan
    else -> QuestionType.TidakDidukung
}

/**
 * [answerOrder] is the (possibly shuffled) list of answer ids in display order — when the
 * instruction is random (`docs/repo lama/asesmen/asesmen-teknis-download-storage.md` §2.4/§2.7),
 * the same order MUST be used again when building the submit payload
 * ([buildAnswerPayload]/[buildStatementAnswer]), since both work off "index into the displayed
 * options" — a mismatch there would submit the wrong answer for whatever the student actually
 * picked on screen.
 */
fun AkmQuestionData.toAkmQuestion(instructionId: Int, answerOrder: List<Int>? = null): AkmQuestion {
    val type = toQuestionType()
    val orderedAnswers = answerOrder?.let { order -> order.mapNotNull { id -> answers.find { it.id == id } } } ?: answers
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
 * Builds the `List<Map>` submit payload exactly per `AkmAnswerPayload.kt` in the legacy app.
 * [answerOrderFor] must return the SAME per-question shuffle order used to render the options the
 * student actually saw and picked from (see [toAkmQuestion]) — otherwise a selected index maps back
 * to the wrong answer row.
 */
fun buildAnswerPayload(
    instructions: List<AkmInstructionData>,
    answers: Map<Int, AkmPlayerAnswer>,
    answerOrderFor: (Int) -> List<Int>? = { null },
): List<Map<String, Any?>> = instructions.flatMap { instruction ->
    instruction.questions.mapNotNull { q ->
        val answer = answers[q.id] ?: return@mapNotNull null
        val orderedAnswers = answerOrderFor(q.id)?.let { order -> order.mapNotNull { id -> q.answers.find { it.id == id } } } ?: q.answers
        val value: Any? = when (q.answerType) {
            "MULTIPLE CHOICE" -> (answer as? AkmPlayerAnswer.Choice)?.let { orderedAnswers.getOrNull(it.index)?.id } ?: 0
            "STATEMENT" -> buildStatementAnswer(orderedAnswers, answer)
            "PAIR" -> buildPairAnswer(orderedAnswers, answer as? AkmPlayerAnswer.PairAnswer)
            else -> (answer as? AkmPlayerAnswer.Essay)?.text.orEmpty()
        }
        mapOf(
            "instruction_id" to instruction.id,
            "question_id" to q.id,
            "answerType" to q.answerType,
            "answer" to value,
        )
    }
}

private fun buildStatementAnswer(rows: List<AkmAnswerData>, answer: AkmPlayerAnswer): List<Map<String, Any?>> {
    // Tabel Pernyataan tracks each row's own Benar/Salah pick by row id (independent per row),
    // unlike BenarSalah/MultiJawaban's shared "which display index is selected" model below.
    if (answer is AkmPlayerAnswer.TableAnswer) {
        return rows.map { row ->
            mapOf("id" to row.id, "isTrue" to if (row.isTrue) 1 else 0, "answered" to if (answer.rows[row.id] == true) 1 else 0)
        }
    }
    val selectedIndices = when (answer) {
        is AkmPlayerAnswer.Choice -> setOf(answer.index)
        is AkmPlayerAnswer.MultiChoice -> answer.indices
        else -> emptySet()
    }
    return rows.mapIndexed { index, row ->
        // BenarSalah stores its "Benar"/"Salah" toggle as index 0/1 over the single real row —
        // both options point at the same row id, so `answered` is what actually carries the pick.
        val isSelected = if (rows.size == 1) selectedIndices.contains(0) else index in selectedIndices
        mapOf("id" to row.id, "isTrue" to if (row.isTrue) 1 else 0, "answered" to if (isSelected) 1 else 0)
    }
}

/** Recovers the owner id from a swapped `second_file_path` filename, exactly like the legacy
 * `AkmAnswerPayload.normalizedPairAnswers()` (`AkmAnswerPayload.kt:79-115`) — image-pairing's
 * `selected_id` can drift from what's actually displayed after repeated drag swaps, so the
 * filename (pattern `a{soal}_{idGambar}_2.jpg`) is treated as the source of truth on submit. */
private val pairImageSecondPathOwnerRegex = Regex("""a\d+_(\d+)_2\.jpg$""")

/**
 * Legacy only applies the filename-normalization for the image-pairing type
 * (`if (question.type != ANSWER_PAIRING_IMAGE) return pairAnswers`, `AkmAnswerPayload.kt:85`).
 * We skip that explicit type check: text-pairing rows never carry a `second_file_path` (it stays
 * blank), the regex simply never matches a blank string, and [selectedId] is used as-is — same
 * end result without threading an extra flag through.
 */
private fun buildPairAnswer(rows: List<AkmAnswerData>, answer: AkmPlayerAnswer.PairAnswer?): List<Map<String, Any?>> =
    rows.map { row ->
        val selectedId = answer?.arrangement?.get(row.id) ?: row.selected_id
        val currentSecondFilePath = answer?.secondFilePaths?.get(row.id) ?: row.secondFilePath
        val ownerIdFromPath = pairImageSecondPathOwnerRegex.find(currentSecondFilePath)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val finalSelectedId = if (ownerIdFromPath != null && ownerIdFromPath > 0) ownerIdFromPath else selectedId
        mapOf("id" to row.id, "answered" to finalSelectedId)
    }

/** An answer to one question — the shape depends on the question type. Kept separate from the
 * `AkmAnswer` sealed interface name already used elsewhere so payload-building stays unambiguous. */
sealed interface AkmPlayerAnswer {
    data class Choice(val index: Int) : AkmPlayerAnswer
    data class MultiChoice(val indices: Set<Int>) : AkmPlayerAnswer
    data class Essay(val text: String) : AkmPlayerAnswer
    /** Tabel Pernyataan — row id -> Benar(true)/Salah(false). Only rows the student has actually
     * toggled are present; untouched rows fall back to "Salah"/unanswered at payload time. */
    data class TableAnswer(val rows: Map<Int, Boolean>) : AkmPlayerAnswer
    /** Menjodohkan — row id -> current `selected_id` after 0+ swaps, snapshotting ALL rows every
     * time (not just the swapped pair), so payload-building never falls back to a stale default.
     * [secondFilePaths] mirrors the same row ids -> the row's current (possibly swapped)
     * `second_file_path`, needed only for the image variant's filename-based normalization. */
    data class PairAnswer(val arrangement: Map<Int, Int>, val secondFilePaths: Map<Int, String> = emptyMap()) : AkmPlayerAnswer
}
