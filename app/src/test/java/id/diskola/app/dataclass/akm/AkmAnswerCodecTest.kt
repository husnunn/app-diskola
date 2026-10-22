package id.diskola.app.dataclass.akm

import id.diskola.app.dataclass.ResponData.AkmAnswerData
import id.diskola.app.dataclass.ResponData.AkmInstructionData
import id.diskola.app.dataclass.ResponData.AkmQuestionData
import id.diskola.app.di.module.MoshiModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmAnswerCodecTest {

    private val codec = AkmAnswerCodec(MoshiModule.instance())

    private fun roundTrip(answer: AkmPlayerAnswer) = codec.decode(codec.encode(answer))

    // ---- codec ----

    @Test
    fun `every answer type survives a round trip`() {
        listOf(
            AkmPlayerAnswer.Choice(42),
            AkmPlayerAnswer.TrueFalse(true),
            AkmPlayerAnswer.TrueFalse(false),
            AkmPlayerAnswer.MultiChoice(setOf(3, 1, 2)),
            AkmPlayerAnswer.Essay("jawaban \"panjang\"\nbaris dua"),
            AkmPlayerAnswer.TableAnswer(mapOf(1 to true, 2 to false)),
            AkmPlayerAnswer.PairAnswer(mapOf(10 to 12, 11 to 10, 12 to 11)),
        ).forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `an empty essay is still an answer`() {
        assertEquals(AkmPlayerAnswer.Essay(""), roundTrip(AkmPlayerAnswer.Essay("")))
    }

    @Test
    fun `an empty multi choice and table stay empty`() {
        assertEquals(AkmPlayerAnswer.MultiChoice(emptySet()), roundTrip(AkmPlayerAnswer.MultiChoice(emptySet())))
        assertEquals(AkmPlayerAnswer.TableAnswer(emptyMap()), roundTrip(AkmPlayerAnswer.TableAnswer(emptyMap())))
    }

    @Test
    fun `damaged or unknown rows decode to null instead of crashing`() {
        assertNull(codec.decode(""))
        assertNull(codec.decode("not json"))
        assertNull(codec.decode("""{"kind":"SOMETHING_NEW"}"""))
        assertNull(codec.decode("""{"kind":"CHOICE"}"""))
        assertNull(codec.decode("""{"kind":"TRUE_FALSE"}"""))
    }

    @Test
    fun `very long essays survive`() {
        val text = "x".repeat(20_000)
        assertEquals(AkmPlayerAnswer.Essay(text), roundTrip(AkmPlayerAnswer.Essay(text)))
    }

    // ---- payload (doc 06 §7.2) ----

    private fun answer(id: Int, isTrue: Boolean = false, selected: Int = 0, showFalse: Boolean = false) =
        AkmAnswerData(id = id, isTrue = isTrue, selected_id = selected, showFalse = showFalse)

    private fun question(id: Int, type: String, answers: List<AkmAnswerData> = emptyList()) =
        AkmQuestionData(id = id, answerType = type, answers = answers)

    private fun instruction(id: Int, vararg questions: AkmQuestionData) = AkmInstructionData(id = id, questions = questions.toList())

    @Test
    fun `only answered questions are sent`() {
        val ins = instruction(1, question(10, "ESSAY"), question(11, "ESSAY"))
        val payload = AkmPayload.build(listOf(ins), mapOf(11 to AkmPlayerAnswer.Essay("b")))
        assertEquals(1, payload.size)
        assertEquals(11, payload[0]["question_id"])
        assertEquals(1, payload[0]["instruction_id"])
        assertEquals("ESSAY", payload[0]["answerType"])
        assertEquals("b", payload[0]["answer"])
    }

    @Test
    fun `an empty essay is sent as an empty string`() {
        val ins = instruction(1, question(10, "ESSAY"))
        assertEquals("", AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.Essay("")))[0]["answer"])
    }

    @Test
    fun `multiple choice sends the chosen answer id`() {
        val ins = instruction(1, question(10, "MULTIPLE CHOICE", listOf(answer(100), answer(101), answer(102))))
        assertEquals(101, AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.Choice(101)))[0]["answer"])
    }

    @Test
    fun `multiple choice without a choice sends zero`() {
        val ins = instruction(1, question(10, "MULTIPLE CHOICE", listOf(answer(100))))
        assertEquals(0, AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.Essay("x")))[0]["answer"])
    }

    @Test
    fun `statement rows carry the key and what was picked`() {
        val ins = instruction(1, question(10, "STATEMENT", listOf(answer(1, isTrue = true), answer(2), answer(3, isTrue = true))))
        val rows = AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.MultiChoice(setOf(1, 2))))[0]["answer"] as List<*>
        assertEquals(mapOf("id" to 1, "isTrue" to 1, "answered" to 1), rows[0])
        assertEquals(mapOf("id" to 2, "isTrue" to 0, "answered" to 1), rows[1])
        assertEquals(mapOf("id" to 3, "isTrue" to 1, "answered" to 0), rows[2])
    }

    @Test
    fun `benar salah over one statement row`() {
        val ins = instruction(1, question(10, "STATEMENT", listOf(answer(1, isTrue = true))))
        val benar = AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.TrueFalse(true)))[0]["answer"] as List<*>
        val salah = AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.TrueFalse(false)))[0]["answer"] as List<*>
        assertEquals(mapOf("id" to 1, "isTrue" to 1, "answered" to 1), benar[0])
        assertEquals(mapOf("id" to 1, "isTrue" to 1, "answered" to 0), salah[0])
    }

    @Test
    fun `table rows record each row's own pick and leave untouched rows unanswered`() {
        val ins = instruction(1, question(10, "STATEMENT", listOf(answer(1, isTrue = true, showFalse = true), answer(2), answer(3))))
        val rows = AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.TableAnswer(mapOf(1 to true, 2 to false))))[0]["answer"] as List<*>
        assertEquals(mapOf("id" to 1, "isTrue" to 1, "answered" to 1), rows[0])
        assertEquals(mapOf("id" to 2, "isTrue" to 0, "answered" to 0), rows[1])
        assertEquals(mapOf("id" to 3, "isTrue" to 0, "answered" to 0), rows[2])
    }

    @Test
    fun `pair rows send the item currently paired with each row`() {
        val ins = instruction(1, question(10, "PAIR", listOf(answer(1, selected = 2), answer(2, selected = 3), answer(3, selected = 1))))
        val swapped = AkmPlayerAnswer.PairAnswer(mapOf(1 to 3, 2 to 2, 3 to 1))
        val rows = AkmPayload.build(listOf(ins), mapOf(10 to swapped))[0]["answer"] as List<*>
        assertEquals(mapOf("id" to 1, "answered" to 3), rows[0])
        assertEquals(mapOf("id" to 2, "answered" to 2), rows[1])
        assertEquals(mapOf("id" to 3, "answered" to 1), rows[2])
    }

    @Test
    fun `a pair question that was never touched is not sent at all`() {
        val ins = instruction(1, question(10, "PAIR", listOf(answer(1, selected = 2))))
        assertTrue(AkmPayload.build(listOf(ins), emptyMap()).isEmpty())
    }

    @Test
    fun `unknown answer types are sent as essays`() {
        val ins = instruction(1, question(10, "SOMETHING_NEW"))
        assertEquals("jawab", AkmPayload.build(listOf(ins), mapOf(10 to AkmPlayerAnswer.Essay("jawab")))[0]["answer"])
    }

    // ---- pair arrangement ----

    private fun slot(id: Int, selected: Int, text: String) = AkmPairSlot(
        id = id, label = "A", firstText = "L$id", firstImageUrl = "",
        currentSecondText = text, currentSecondImageUrl = "img$selected", currentSelectedId = selected,
    )

    private val defaults = listOf(slot(1, 2, "R2"), slot(2, 3, "R3"), slot(3, 1, "R1"))

    @Test
    fun `no stored answer shows the rows as downloaded`() {
        assertEquals(defaults, defaults.arranged(null))
        assertEquals(defaults, defaults.arranged(emptyMap()))
    }

    @Test
    fun `a stored arrangement moves the right hand items`() {
        val arranged = defaults.arranged(mapOf(1 to 3, 2 to 2, 3 to 1))
        assertEquals(3, arranged.size)
        // row 1 now pairs with item 3; the default slot holding item 3 is row 2 ("R3")
        assertEquals("R3", arranged[0].currentSecondText)
        assertEquals(3, arranged[0].currentSelectedId)
        assertEquals("img3", arranged[0].currentSecondImageUrl)
        // row 2 keeps item 2... which the default slot of row 1 holds ("R2")
        assertEquals("R2", arranged[1].currentSecondText)
        assertEquals(2, arranged[1].currentSelectedId)
        // row 3 pairs with item 1, held by default row 3 ("R1")
        assertEquals("R1", arranged[2].currentSecondText)
        assertEquals(1, arranged[2].currentSelectedId)
    }

    @Test
    fun `swapping exchanges only the right hand side`() {
        val swapped = defaults.swapped(0, 2)
        assertEquals("R1", swapped[0].currentSecondText)
        assertEquals(1, swapped[0].currentSelectedId)
        assertEquals("R2", swapped[2].currentSecondText)
        assertEquals(2, swapped[2].currentSelectedId)
        assertEquals("L1", swapped[0].firstText)
        assertEquals(1, swapped[0].id)
        assertEquals(defaults[1], swapped[1])
    }

    @Test
    fun `an invalid swap changes nothing`() {
        assertEquals(defaults, defaults.swapped(1, 1))
        assertEquals(defaults, defaults.swapped(0, 9))
        assertEquals(defaults, defaults.swapped(-1, 0))
    }

    @Test
    fun `an arrangement taken from swapped rows applies back to the defaults`() {
        val swapped = defaults.swapped(0, 1)
        val arrangement = swapped.toArrangement()
        assertEquals(swapped, defaults.arranged(arrangement))
    }
}
