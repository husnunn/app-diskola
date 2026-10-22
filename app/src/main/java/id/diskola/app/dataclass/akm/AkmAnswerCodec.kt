package id.diskola.app.dataclass.akm

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import id.diskola.app.dataclass.ResponData.AkmAnswerData
import id.diskola.app.dataclass.ResponData.AkmInstructionData

/** The row stored in `akm_answer.answerJson` — one flat shape for every [AkmPlayerAnswer], so a new answer type never needs a migration. */
@JsonClass(generateAdapter = true)
data class StoredAnswer(
    val kind: String,
    val ids: List<Int> = emptyList(),
    val flag: Boolean? = null,
    val text: String = "",
    val pairs: List<StoredPair> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class StoredPair(val id: Int, val value: Int = 0, val flag: Boolean = false)

/** [AkmPlayerAnswer] <-> the JSON kept in Room. Unknown or damaged rows decode to null (the question then simply counts as unanswered). */
class AkmAnswerCodec(moshi: Moshi) {
    private val adapter = moshi.adapter(StoredAnswer::class.java)

    fun encode(answer: AkmPlayerAnswer): String = adapter.toJson(toStored(answer))

    fun decode(json: String): AkmPlayerAnswer? = try {
        adapter.fromJson(json)?.let(::fromStored)
    } catch (e: Exception) {
        null
    }

    private fun toStored(answer: AkmPlayerAnswer): StoredAnswer = when (answer) {
        is AkmPlayerAnswer.Choice -> StoredAnswer(KIND_CHOICE, ids = listOf(answer.answerId))
        is AkmPlayerAnswer.TrueFalse -> StoredAnswer(KIND_TRUE_FALSE, flag = answer.isTrue)
        is AkmPlayerAnswer.MultiChoice -> StoredAnswer(KIND_MULTI, ids = answer.answerIds.sorted())
        is AkmPlayerAnswer.Essay -> StoredAnswer(KIND_ESSAY, text = answer.text)
        is AkmPlayerAnswer.TableAnswer -> StoredAnswer(KIND_TABLE, pairs = answer.rows.map { (id, value) -> StoredPair(id, flag = value) })
        is AkmPlayerAnswer.PairAnswer -> StoredAnswer(KIND_PAIR, pairs = answer.arrangement.map { (id, selected) -> StoredPair(id, value = selected) })
    }

    private fun fromStored(stored: StoredAnswer): AkmPlayerAnswer? = when (stored.kind) {
        KIND_CHOICE -> stored.ids.firstOrNull()?.let { AkmPlayerAnswer.Choice(it) }
        KIND_TRUE_FALSE -> stored.flag?.let { AkmPlayerAnswer.TrueFalse(it) }
        KIND_MULTI -> AkmPlayerAnswer.MultiChoice(stored.ids.toSet())
        KIND_ESSAY -> AkmPlayerAnswer.Essay(stored.text)
        KIND_TABLE -> AkmPlayerAnswer.TableAnswer(stored.pairs.associate { it.id to it.flag })
        KIND_PAIR -> AkmPlayerAnswer.PairAnswer(stored.pairs.associate { it.id to it.value })
        else -> null
    }

    private companion object {
        const val KIND_CHOICE = "CHOICE"
        const val KIND_TRUE_FALSE = "TRUE_FALSE"
        const val KIND_MULTI = "MULTI"
        const val KIND_ESSAY = "ESSAY"
        const val KIND_TABLE = "TABLE"
        const val KIND_PAIR = "PAIR"
    }
}

/**
 * The submit body, per doc `06` §7.2 (legacy `AkmAnswerPayload.kt`): one item for every **answered** question —
 * `{instruction_id, question_id, answerType, answer}` — where `answerType` is the API's own label and `answer` depends on it.
 * Built from the stored answers at send time (never kept as a JSON map: Moshi would read the ids back as doubles).
 */
object AkmPayload {

    fun build(instructions: List<AkmInstructionData>, answers: Map<Int, AkmPlayerAnswer>): List<Map<String, Any?>> =
        instructions.flatMap { instruction ->
            instruction.questions.mapNotNull { question ->
                val answer = answers[question.id] ?: return@mapNotNull null
                mapOf(
                    "instruction_id" to instruction.id,
                    "question_id" to question.id,
                    "answerType" to question.answerType,
                    "answer" to answerValue(question.answerType, question.answers, answer),
                )
            }
        }

    private fun answerValue(answerType: String, rows: List<AkmAnswerData>, answer: AkmPlayerAnswer): Any? = when (answerType) {
        // The chosen option id, or 0 when the stored answer is not a single choice.
        "MULTIPLE CHOICE" -> (answer as? AkmPlayerAnswer.Choice)?.answerId ?: 0
        "STATEMENT" -> statementRows(rows, answer)
        "PAIR" -> {
            val arrangement = (answer as? AkmPlayerAnswer.PairAnswer)?.arrangement.orEmpty()
            rows.map { row -> mapOf("id" to row.id, "answered" to (arrangement[row.id] ?: row.selected_id)) }
        }
        else -> (answer as? AkmPlayerAnswer.Essay)?.text.orEmpty()
    }

    /** `isTrue` is the answer key of the row, `answered` is whether "Benar" was picked / ticked for it. */
    private fun statementRows(rows: List<AkmAnswerData>, answer: AkmPlayerAnswer): List<Map<String, Any?>> =
        rows.map { row ->
            val picked = when (answer) {
                is AkmPlayerAnswer.TableAnswer -> answer.rows[row.id] == true
                is AkmPlayerAnswer.TrueFalse -> answer.isTrue
                is AkmPlayerAnswer.MultiChoice -> row.id in answer.answerIds
                else -> false
            }
            mapOf("id" to row.id, "isTrue" to if (row.isTrue) 1 else 0, "answered" to if (picked) 1 else 0)
        }
}

/**
 * The right-hand column of a Menjodohkan question as the student left it: every row shows the item [arrangement] pairs it with.
 * [defaults] are the rows as downloaded (each carries its own right-hand item and `selected_id`); with no stored answer they are
 * returned unchanged.
 */
fun List<AkmPairSlot>.arranged(arrangement: Map<Int, Int>?): List<AkmPairSlot> {
    if (arrangement.isNullOrEmpty()) return this
    val bySelected = associateBy { it.currentSelectedId }
    return map { slot ->
        val selected = arrangement[slot.id] ?: return@map slot
        val source = bySelected[selected] ?: return@map slot
        slot.copy(
            currentSecondText = source.currentSecondText,
            currentSecondImageUrl = source.currentSecondImageUrl,
            currentSelectedId = selected,
        )
    }
}

/** Swapping two right-hand items — the only thing a drag does (the left anchors and row ids never move). */
fun List<AkmPairSlot>.swapped(positionA: Int, positionB: Int): List<AkmPairSlot> {
    if (positionA == positionB || positionA !in indices || positionB !in indices) return this
    val a = this[positionA]
    val b = this[positionB]
    return toMutableList().apply {
        this[positionA] = a.copy(currentSecondText = b.currentSecondText, currentSecondImageUrl = b.currentSecondImageUrl, currentSelectedId = b.currentSelectedId)
        this[positionB] = b.copy(currentSecondText = a.currentSecondText, currentSecondImageUrl = a.currentSecondImageUrl, currentSelectedId = a.currentSelectedId)
    }
}

fun List<AkmPairSlot>.toArrangement(): Map<Int, Int> = associate { it.id to it.currentSelectedId }
