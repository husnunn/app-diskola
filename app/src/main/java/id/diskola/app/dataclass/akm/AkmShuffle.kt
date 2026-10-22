package id.diskola.app.dataclass.akm

import id.diskola.app.dataclass.ResponData.AkmAnswerData
import id.diskola.app.dataclass.ResponData.AkmExamData
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
import kotlin.random.Random

/**
 * Randomisation done **once, when the package is downloaded**, and stored with it (legacy `AkmDownloader.kt:81-85,157-161`): the order
 * saved is the order shown, so reopening an instruction — or the app restarting — never reshuffles what a student already answered.
 */
object AkmShuffle {

    /** Questions of an `isRandom` instruction, and the options of each of those questions, in a new order. */
    fun shuffleSchedule(schedule: AkmScheduleData, random: Random): AkmScheduleData {
        val exams = schedule.exams ?: listOfNotNull(schedule.exam)
        return schedule.copy(exams = exams.map { it.shuffled(random) }, exam = null)
    }

    private fun AkmExamData.shuffled(random: Random): AkmExamData = copy(instructions = instructions.map { it.shuffled(random) })

    private fun AkmInstructionData.shuffled(random: Random): AkmInstructionData {
        if (!isRandom) return this
        return copy(questions = questions.shuffled(random).map { it.copy(answers = it.answers.shuffled(random)) })
    }

    /**
     * Menjodohkan: the right-hand column is always shuffled (random instruction or not, `AkmDownloader.kt:138-155`). Row `i` ends up
     * showing the right-hand item that belongs to row `selected_id`; the student's job is to put every item next to its own row.
     * Rows keep their ids and left-hand sides; only `secondStatement`/`secondFilePath`/`selected_id` move.
     */
    fun shufflePairRows(rows: List<AkmAnswerData>, random: Random): List<AkmAnswerData> {
        val secondTextById = rows.associate { it.id to it.secondStatement }
        val secondPathById = rows.associate { it.id to it.secondFilePath }
        val order = rows.map { it.id }.shuffled(random)
        return rows.mapIndexed { index, row ->
            val owner = order[index]
            row.copy(
                secondStatement = secondTextById[owner].orEmpty(),
                secondFilePath = secondPathById[owner].orEmpty(),
                selected_id = owner,
            )
        }
    }
}
