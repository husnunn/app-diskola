package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachMarkRulesTest {

    // A 1080 x 2000 area, 24 px margin, 24 px frame, 48 px arrow.
    private fun place(left: Float, top: Float, right: Float, bottom: Float, bw: Float = 800f, bh: Float = 420f) =
        CoachMarkRules.place(left, top, right, bottom, bw, bh, areaWidth = 1080f, areaHeight = 2000f, margin = 24f, pad = 24f, arrowWidth = 48f)

    @Test
    fun place_aTargetNearTheTopPutsTheBubbleBelowIt() {
        val p = place(40f, 200f, 1040f, 300f)
        assertFalse(p.above)
        assertEquals(300f + 24f + 24f, p.top, 0f)
    }

    @Test
    fun place_aTargetNearTheBottomPutsTheBubbleAboveIt() {
        val p = place(40f, 1700f, 1040f, 1800f)
        assertTrue(p.above)
        assertEquals(1700f - 420f - 24f - 24f, p.top, 0f)
    }

    @Test
    fun place_aboveOnlyWhenItFits() {
        // Room above (380) is larger than below (10) but smaller than the bubble needs (468), so it falls back to below, as legacy did.
        val p = place(40f, 380f, 1040f, 1990f)
        assertFalse(p.above)
    }

    @Test
    fun place_theBubbleIsCentredOnTheTargetAndKeptInsideTheScreen() {
        val centred = place(440f, 200f, 640f, 300f)
        assertEquals(540f - 400f, centred.left, 0f)
        val leftEdge = place(0f, 200f, 100f, 300f)
        assertEquals(24f, leftEdge.left, 0f)
        val rightEdge = place(980f, 200f, 1080f, 300f)
        assertEquals(1080f - 800f - 24f, rightEdge.left, 0f)
    }

    @Test
    fun place_theArrowPointsAtTheTargetCentreButStaysOffTheBubbleCorners() {
        val centred = place(440f, 200f, 640f, 300f)
        assertEquals(540f, centred.arrowCenterX, 0f)
        val leftEdge = place(0f, 200f, 100f, 300f)
        assertEquals(24f + 48f, leftEdge.arrowCenterX, 0f)
        val rightEdge = place(980f, 200f, 1080f, 300f)
        assertEquals(1080f - 24f - 48f, rightEdge.arrowCenterX, 0f)
    }

    @Test
    fun steps_labelCounterAndAdvance() {
        assertEquals("Lanjut", CoachMarkRules.buttonLabel(0, 4))
        assertEquals("Lanjut", CoachMarkRules.buttonLabel(2, 4))
        assertEquals("Selesai", CoachMarkRules.buttonLabel(3, 4))
        assertEquals("Selesai", CoachMarkRules.buttonLabel(0, 1))
        assertEquals("1 dari 4", CoachMarkRules.counter(0, 4))
        assertEquals("4 dari 4", CoachMarkRules.counter(3, 4))
        assertEquals(1, CoachMarkRules.advance(0, 4))
        assertEquals(3, CoachMarkRules.advance(2, 4))
        assertNull(CoachMarkRules.advance(3, 4))
        assertNull(CoachMarkRules.advance(0, 1))
    }
}
