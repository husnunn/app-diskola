package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathParserTest {

    @Test
    fun inline_delimiter_is_found() {
        val text = "Hasil \\(\\frac{1}{2}\\) benar"
        val ranges = findMathRanges(text)

        assertEquals(1, ranges.size)
        assertEquals("\\frac{1}{2}", ranges[0].latex)
        assertFalse(ranges[0].display)
        assertEquals("\\(\\frac{1}{2}\\)", text.substring(ranges[0].start, ranges[0].end))
    }

    @Test
    fun display_and_double_dollar_are_display_style() {
        val ranges = findMathRanges("a \\[x^2\\] b \$\$y^2\$\$ c")

        assertEquals(2, ranges.size)
        assertEquals("x^2", ranges[0].latex)
        assertEquals("y^2", ranges[1].latex)
        assertTrue(ranges.all { it.display })
    }

    @Test
    fun ranges_are_ordered_by_position_regardless_of_delimiter_type() {
        val ranges = findMathRanges("\\(a\\) lalu \$\$b\$\$ lalu \\(c\\)")

        assertEquals(listOf("a", "b", "c"), ranges.map { it.latex })
        assertEquals(ranges.sortedBy { it.start }, ranges)
    }

    @Test
    fun single_dollar_is_not_a_delimiter() {
        assertTrue(findMathRanges("Harga \$5 dan \$10").isEmpty())
        assertFalse(containsMath("Harga \$5 dan \$10"))
    }

    @Test
    fun formula_can_span_multiple_lines() {
        val ranges = findMathRanges("\\[\n\\sum_{i=1}^{n} i\n\\]")

        assertEquals(1, ranges.size)
        assertEquals("\\sum_{i=1}^{n} i", ranges[0].latex)
    }

    @Test
    fun overlapping_delimiters_keep_only_the_earlier_one() {
        val ranges = findMathRanges("\\[ \\( x \\) \\]")

        assertEquals(1, ranges.size)
        assertTrue(ranges[0].display)
        assertEquals("\\( x \\)", ranges[0].latex)
    }

    @Test
    fun unterminated_delimiter_is_ignored() {
        assertTrue(findMathRanges("Rumus \\(x^2 belum ditutup").isEmpty())
    }

    @Test
    fun empty_or_blank_formula_is_ignored() {
        assertTrue(findMathRanges("kosong \\(\\) dan \\(   \\)").isEmpty())
    }

    @Test
    fun less_than_sign_decoded_from_html_is_kept_in_latex() {
        // Setelah HtmlCompat.fromHtml, "&lt;" sudah menjadi "<" sebelum teks sampai ke parser ini.
        val ranges = findMathRanges("Jika \\(a < b\\) maka")

        assertEquals(1, ranges.size)
        assertEquals("a < b", ranges[0].latex)
    }

    @Test
    fun containsMath_detects_each_delimiter() {
        assertTrue(containsMath("<p>\\(x\\)</p>"))
        assertTrue(containsMath("\\[x\\]"))
        assertTrue(containsMath("\$\$x\$\$"))
        assertFalse(containsMath("<p>teks biasa</p>"))
    }
}
