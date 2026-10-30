package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkeletonRulesTest {

    @Test
    fun `the band starts off the left edge and ends off the right edge`() {
        assertEquals(-120f, SkeletonRules.bandStart(progress = 0f, width = 300f, bandWidth = 120f), 0.001f)
        assertEquals(300f, SkeletonRules.bandStart(progress = 1f, width = 300f, bandWidth = 120f), 0.001f)
    }

    @Test
    fun `the band moves steadily to the right`() {
        val positions = (0..10).map { SkeletonRules.bandStart(it / 10f, width = 300f, bandWidth = 120f) }
        assertEquals(positions.sorted(), positions)
        assertEquals(positions.distinct().size, positions.size)
        // Halfway the band's start is halfway between the two extremes.
        assertEquals(90f, SkeletonRules.bandStart(0.5f, 300f, 120f), 0.001f)
    }

    @Test
    fun `progress outside 0 to 1 is clamped`() {
        assertEquals(SkeletonRules.bandStart(0f, 300f, 120f), SkeletonRules.bandStart(-0.5f, 300f, 120f), 0.001f)
        assertEquals(SkeletonRules.bandStart(1f, 300f, 120f), SkeletonRules.bandStart(1.7f, 300f, 120f), 0.001f)
    }

    @Test
    fun `only an animation scale of exactly zero is reduce-motion`() {
        assertTrue(SkeletonRules.isReduceMotion(0f))
        assertFalse(SkeletonRules.isReduceMotion(1f))
        assertFalse(SkeletonRules.isReduceMotion(0.5f))
        assertFalse(SkeletonRules.isReduceMotion(10f))
    }

    @Test
    fun `the show delay is long enough to skip a fast answer but short enough to feel immediate`() {
        assertTrue(SkeletonRules.SHOW_DELAY_MILLIS in 100L..300L)
    }
}
