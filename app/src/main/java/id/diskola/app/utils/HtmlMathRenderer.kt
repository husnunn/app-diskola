package id.diskola.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.core.text.HtmlCompat
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.TeXIcon
import ru.noties.jlatexmath.awt.Graphics
import kotlin.text.Regex

/** Verbatim port of `android-portal/.../utils/HtmlMathRenderer.kt` from the legacy app. */
object HtmlMathRenderer {

    /** Matches a WYSIWYG math-plugin's `data-value="..."` attribute — the raw formula source. */
    private val formulaDataValueRegex = Regex("(?<=data-value=\")(.*?)(?=\")")

    // Patterns: $$...$$, \(...\), \[...\]
    private val patternBlock = Regex("""\$\$(.+?)\$\$""", RegexOption.DOT_MATCHES_ALL)
    private val patternInline = Regex("""\\\((.+?)\\\)""", RegexOption.DOT_MATCHES_ALL)
    private val patternDisplay = Regex("""\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)

    /**
     * Wraps any `data-value="..."` formula source in `\( ... \)` inline-math delimiters so
     * [setTextWithMath] picks it up. Mirrors the `.replace(formulaRegex) { "\\(${it.value}\\)" }`
     * call duplicated at every legacy ViewHolder call site (e.g. `QuestionTableVh.kt:105-106`).
     */
    fun preprocessDataValueFormula(html: String): String =
        html.replace(formulaDataValueRegex) { match -> "\\(${match.value}\\)" }

    fun setTextWithMath(textView: TextView, rawHtml: String) {
        // Parse HTML to Spanned
        val spanned = HtmlCompat.fromHtml(
            rawHtml,
            HtmlCompat.FROM_HTML_MODE_LEGACY,
            imageGetter(textView.context),
            null
        )

        val builder = SpannableStringBuilder(spanned)
        val text = builder.toString()

        val matches = mutableListOf<Match>()
        findAll(patternBlock, text) { matches.add(it.copy(style = TeXConstants.STYLE_DISPLAY)) }
        findAll(patternDisplay, text) { matches.add(it.copy(style = TeXConstants.STYLE_DISPLAY)) }
        findAll(patternInline, text) { matches.add(it.copy(style = TeXConstants.STYLE_TEXT)) }

        // Replace from end to start to keep indices valid
        matches.sortByDescending { it.start }

        val maxWidthPx = estimateMaxWidth(textView)
        matches.forEach { m ->
            val drawable = renderLatexDrawable(textView.context, m.latex, textView, m.style, maxWidthPx)
            val span = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
            val placeholder = "￼"
            builder.replace(m.start, m.end, placeholder)
            builder.setSpan(span, m.start, m.start + placeholder.length, 0)
        }

        textView.text = builder
        // No `movementMethod` here (legacy sets `LinkMovementMethod` unconditionally, but this app
        // never renders real clickable spans in question/answer text) — installing it anyway made
        // the embedded TextView claim touch at the native-view layer before Compose's own gesture
        // detectors on ancestor composables ever saw the event, breaking the Menjodohkan long-press
        // drag (confirmed live: long-press produced zero reaction whatsoever).
    }

    private fun imageGetter(context: Context): Html.ImageGetter = Html.ImageGetter { _ ->
        // Minimal placeholder for <img> in HTML. Question image shown in separate ImageView.
        ColorDrawable(Color.TRANSPARENT).apply {
            setBounds(0, 0, 0, 0)
        }
    }

    private fun estimateMaxWidth(textView: TextView): Int {
        val dm = textView.resources.displayMetrics
        val screen = dm.widthPixels
        val horizontalPadding = textView.paddingLeft + textView.paddingRight
        val marginReserve = (16 * dm.density).toInt() * 2 // ~32dp
        return (screen - horizontalPadding - marginReserve).coerceAtLeast(1)
    }

    private fun renderLatexDrawable(
        context: Context,
        latex: String,
        textView: TextView,
        style: Int,
        maxWidthPx: Int
    ): Drawable {
        return try {
            val fontSizeSp = textView.textSize / context.resources.displayMetrics.scaledDensity
            val formula = TeXFormula(latex)
            val icon: TeXIcon = formula.createTeXIcon(style, fontSizeSp)

            var bmp = Bitmap.createBitmap(icon.iconWidth, icon.iconHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.TRANSPARENT)
            icon.paintIcon(null, canvas as Graphics?, 0, 0)

            // Scale down if wider than max width
            if (bmp.width > maxWidthPx && bmp.width > 0) {
                val ratio = maxWidthPx.toFloat() / bmp.width.toFloat()
                val newW = maxWidthPx
                val newH = (bmp.height * ratio).toInt().coerceAtLeast(1)
                bmp = Bitmap.createScaledBitmap(bmp, newW, newH, true)
            }

            BitmapDrawable(context.resources, bmp).apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        } catch (e: Exception) {
            // Fallback: show nothing if formula invalid
            ColorDrawable(Color.TRANSPARENT).apply { setBounds(0, 0, 0, 0) }
        }
    }

    private data class Match(val start: Int, val end: Int, val latex: String, val style: Int)

    private fun findAll(pattern: Regex, text: String, emit: (Match) -> Unit) {
        pattern.findAll(text).forEach { mr ->
            val latex = mr.groupValues.getOrNull(1)
            if (latex != null) emit(Match(mr.range.first, mr.range.last + 1, latex, TeXConstants.STYLE_TEXT))
        }
    }
}
