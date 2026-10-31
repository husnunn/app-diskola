package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class OffsiteWatermarkTest {

    @Test
    fun sampleSize_keepsTheLongSideAtOrAboveTheCap() {
        // The old rule gave 4 here (4000 / 4 = 1000 px, below the 1600 px cap).
        assertEquals(2, OffsiteWatermark.sampleSize(4000, 1600))
        assertEquals(2, OffsiteWatermark.sampleSize(3200, 1600))
        assertEquals(4, OffsiteWatermark.sampleSize(6400, 1600))
        assertEquals(1, OffsiteWatermark.sampleSize(3199, 1600))
        assertEquals(1, OffsiteWatermark.sampleSize(1500, 1600))
        assertEquals(1, OffsiteWatermark.sampleSize(1600, 1600))
    }

    @Test
    fun sampleSize_neverLeavesMoreThanTwiceTheCap() {
        for (long in listOf(1601, 2400, 3199, 4000, 4608, 6400, 8000)) {
            val decoded = long / OffsiteWatermark.sampleSize(long, 1600)
            assertEquals("long=$long decoded=$decoded", true, decoded >= 1600 && decoded < 3200)
        }
    }

    @Test
    fun scaledSize_fitsTheLongSideKeepingTheRatio() {
        assertEquals(1600 to 1200, OffsiteWatermark.scaledSize(2000, 1500, 1600))
        assertEquals(1200 to 1600, OffsiteWatermark.scaledSize(1500, 2000, 1600))
        assertEquals(1000 to 500, OffsiteWatermark.scaledSize(1000, 500, 1600))
        assertEquals(1600 to 900, OffsiteWatermark.scaledSize(1600, 900, 1600))
    }
}
