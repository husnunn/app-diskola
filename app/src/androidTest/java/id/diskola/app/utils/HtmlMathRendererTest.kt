package id.diskola.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menjalankan JLaTeXMath sungguhan di device. Renderer lama memanggil `paintIcon` dengan Canvas
 * yang di-cast paksa ke `Graphics`, melempar ClassCastException yang ditelan sehingga semua rumus
 * tampil kosong — tes ini memastikan rumus benar-benar tergambar, bukan sekadar span terpasang.
 */
@RunWith(AndroidJUnit4::class)
class HtmlMathRendererTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun render(
        html: String,
        color: Int = Color.RED,
        enableLinks: Boolean = true
    ): TextView {
        lateinit var textView: TextView
        instrumentation.runOnMainSync {
            textView = TextView(instrumentation.targetContext).apply { textSize = 18f }
            HtmlMathRenderer.setTextWithMath(textView, html, color, enableLinks)
        }
        return textView
    }

    private fun imageSpans(textView: TextView): Array<ImageSpan> {
        val text = textView.text as Spanned
        return text.getSpans(0, text.length, ImageSpan::class.java)
    }

    @Test
    fun formula_is_drawn_in_the_requested_color() {
        val textView = render("<p>Hasil \\(\\frac{1}{2}\\)</p>", Color.RED)

        assertTrue(textView.text.toString().startsWith("Hasil "))
        val spans = imageSpans(textView)
        assertEquals(1, spans.size)

        val drawable = spans[0].drawable
        assertTrue(drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        drawable.draw(Canvas(bitmap))

        var red = 0
        var black = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val p = bitmap.getPixel(x, y)
                if (Color.alpha(p) < 200) continue
                if (Color.red(p) > 200 && Color.green(p) < 60 && Color.blue(p) < 60) red++
                if (Color.red(p) < 50 && Color.green(p) < 50 && Color.blue(p) < 50) black++
            }
        }
        assertTrue("rumus tidak tergambar dengan warna yang diminta", red > 0)
        assertEquals("rumus masih tergambar hitam default", 0, black)
    }

    @Test
    fun display_and_inline_formulas_in_one_text_become_two_spans() {
        val textView = render("<p>\\(x^2\\) lalu \\[\\sqrt{y}\\]</p>")

        assertEquals(2, imageSpans(textView).size)
    }

    @Test
    fun invalid_latex_keeps_its_source_text_instead_of_vanishing() {
        val textView = render("<p>Rusak \\(\\unknowncommandxyz{\\) selesai</p>")

        assertEquals(0, imageSpans(textView).size)
        assertTrue(textView.text.toString().contains("\\(\\unknowncommandxyz{\\)"))
    }

    @Test
    fun formula_wider_than_the_available_width_is_scaled_down() {
        val longFormula = (1..120).joinToString(" + ") { "x_{$it}" }
        val textView = render("<p>\\($longFormula\\)</p>")

        val drawable = imageSpans(textView).single().drawable
        assertTrue(drawable.intrinsicWidth <= HtmlMathRenderer.availableWidth(textView))
    }

    @Test
    fun links_are_not_enabled_when_disabled_so_rows_stay_clickable() {
        val textView = render("<p>\\(x\\)</p>", enableLinks = false)

        assertNull(textView.movementMethod)
    }
}
