package id.diskola.app.dataclass.akm

import id.diskola.app.dataclass.ResponData.AkmAnswerData
import id.diskola.app.dataclass.ResponData.AkmExamData
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmQuestionData
import id.diskola.app.dataclass.ResponData.AkmScheduleData
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AkmShuffleTest {

    private fun question(id: Int, optionIds: List<Int>) =
        AkmQuestionData(id = id, answerType = "MULTIPLE CHOICE", answers = optionIds.map { AkmAnswerData(id = it) })

    private fun schedule(random: Boolean) = AkmScheduleData(
        id = 1,
        exams = listOf(
            AkmExamData(
                id = 5,
                instructions = listOf(
                    AkmInstructionData(id = 7, isRandom = random, questions = (1..20).map { question(it, (1..6).map { o -> it * 10 + o }) }),
                ),
            ),
        ),
    )

    private fun AkmScheduleData.questions() = exams!!.single().instructions.single().questions

    @Test
    fun `a random instruction reorders its questions and options but loses none`() {
        val original = schedule(random = true)
        val shuffled = AkmShuffle.shuffleSchedule(original, Random(1)).questions()
        assertEquals(original.questions().map { it.id }.toSet(), shuffled.map { it.id }.toSet())
        assertNotEquals(original.questions().map { it.id }, shuffled.map { it.id })
        shuffled.forEach { q ->
            val before = original.questions().first { it.id == q.id }.answers.map { it.id }
            assertEquals(before.toSet(), q.answers.map { it.id }.toSet())
        }
        assertNotEquals(original.questions().map { q -> q.answers.map { it.id } }, shuffled.map { q -> q.answers.map { it.id } })
    }

    @Test
    fun `a fixed instruction keeps its order`() {
        val original = schedule(random = false)
        assertEquals(original.questions(), AkmShuffle.shuffleSchedule(original, Random(1)).questions())
    }

    @Test
    fun `the same seed gives the same order`() {
        val a = AkmShuffle.shuffleSchedule(schedule(true), Random(42)).questions().map { it.id }
        val b = AkmShuffle.shuffleSchedule(schedule(true), Random(42)).questions().map { it.id }
        assertEquals(a, b)
    }

    @Test
    fun `a single exam object is folded into the exam list`() {
        val single = AkmScheduleData(id = 1, exam = AkmExamData(id = 5))
        val result = AkmShuffle.shuffleSchedule(single, Random(0))
        assertEquals(1, result.exams!!.size)
        assertNull(result.exam)
    }

    @Test
    fun `pair rows keep ids and left sides while the right side is permuted`() {
        val rows = (1..6).map { AkmAnswerData(id = it, firstStatement = "L$it", secondStatement = "R$it", secondFilePath = "p$it", selected_id = it) }
        val shuffled = AkmShuffle.shufflePairRows(rows, Random(3))
        assertEquals(rows.map { it.id }, shuffled.map { it.id })
        assertEquals(rows.map { it.firstStatement }, shuffled.map { it.firstStatement })
        assertEquals((1..6).toSet(), shuffled.map { it.selected_id }.toSet())
        shuffled.forEach { row ->
            // the item shown on a row is the one that belongs to selected_id
            assertEquals("R${row.selected_id}", row.secondStatement)
            assertEquals("p${row.selected_id}", row.secondFilePath)
        }
    }
}
