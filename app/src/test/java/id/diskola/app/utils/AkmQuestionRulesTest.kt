package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AkmQuestionRulesTest {

    private fun mode(
        position: Int,
        count: Int = 3,
        penalty: Boolean = false,
        explanation: Boolean = false,
        allowFinish: Boolean = false,
        breach: Boolean = false,
    ) = AkmQuestionRules.nextButtonMode(position, count, penalty, explanation, allowFinish, breach)

    @Test
    fun `a question that is not the last one is next, even when everything is answered`() {
        assertEquals(NextButtonMode.NEXT, mode(0, allowFinish = true))
        assertEquals(NextButtonMode.NEXT, mode(1, allowFinish = false))
    }

    @Test
    fun `the last question becomes finish only when every question is answered`() {
        assertEquals(NextButtonMode.FINISH, mode(2, allowFinish = true))
        assertEquals(NextButtonMode.HIDDEN, mode(2, allowFinish = false))
    }

    @Test
    fun `finish is hidden while the pin is lost`() {
        assertEquals(NextButtonMode.HIDDEN, mode(2, allowFinish = true, breach = true))
    }

    @Test
    fun `a penalty hides the button on every question`() {
        assertEquals(NextButtonMode.HIDDEN, mode(0, penalty = true))
        assertEquals(NextButtonMode.HIDDEN, mode(2, penalty = true, allowFinish = true))
    }

    @Test
    fun `the explanation mode keeps next but never shows finish`() {
        assertEquals(NextButtonMode.NEXT, mode(0, explanation = true))
        assertEquals(NextButtonMode.HIDDEN, mode(2, explanation = true, allowFinish = true))
    }

    @Test
    fun `an exam of one question shows finish once it is answered`() {
        assertEquals(NextButtonMode.FINISH, mode(0, count = 1, allowFinish = true))
        assertEquals(NextButtonMode.HIDDEN, mode(0, count = 1, allowFinish = false))
    }

    @Test
    fun `with no questions loaded yet the button is hidden`() {
        assertEquals(NextButtonMode.HIDDEN, mode(0, count = 0, allowFinish = true))
    }
}
