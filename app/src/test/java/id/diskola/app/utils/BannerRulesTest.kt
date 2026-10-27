package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerRulesTest {

    @Test
    fun `a short message stays the minimum time`() {
        assertEquals(BannerRules.MIN_MILLIS, BannerRules.autoDismissMillis("Tugas telah dikerjakan"))
        assertEquals(BannerRules.MIN_MILLIS, BannerRules.autoDismissMillis(""))
    }

    @Test
    fun `a longer message stays longer`() {
        val medium = BannerRules.autoDismissMillis("x".repeat(80))
        val long = BannerRules.autoDismissMillis("x".repeat(120))
        assertEquals(7_800L, medium)
        assertTrue(long > medium)
    }

    @Test
    fun `a very long message is capped`() {
        assertEquals(BannerRules.MAX_MILLIS, BannerRules.autoDismissMillis("x".repeat(500)))
    }

    @Test
    fun `the time never leaves the bounds`() {
        listOf(0, 1, 30, 33, 34, 100, 149, 150, 151, 1_000).forEach { length ->
            val millis = BannerRules.autoDismissMillis("x".repeat(length))
            assertTrue("length $length -> $millis", millis in BannerRules.MIN_MILLIS..BannerRules.MAX_MILLIS)
        }
    }
}
