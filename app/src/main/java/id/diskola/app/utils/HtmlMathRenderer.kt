package id.diskola.app.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.util.LruCache
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.text.HtmlCompat
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.TeXIcon
import ru.noties.jlatexmath.awt.AndroidGraphics2D
import timber.log.Timber
import kotlin.math.roundToInt
import ru.noties.jlatexmath.awt.Color as TexColor

/**
 * Menampilkan HTML yang berisi rumus LaTeX (`\(…\)`, `\[…\]`, `$$…$$`) pada [TextView].
 *
 * HTML di-parse lebih dulu, lalu tiap rumus pada teks polos hasilnya diganti [ImageSpan] yang
 * menggambar rumus lewat JLaTeXMath. Pencarian rumus ada di [findMathRanges].
 */
object HtmlMathRenderer {

    private const val PLACEHOLDER = "\uFFFC"

    /** A WYSIWYG math plugin stores the formula source in `data-value="..."`; this is the attribute's value. */
    private val formulaDataValueRegex = Regex("(?<=data-value=\")(.*?)(?=\")")

    /**
     * Wraps any `data-value="..."` formula source in `\( ... \)` so [setTextWithMath] picks it up. Legacy does this at the six question-text
     * call sites only (`QuestionAdapter`), never for answers; [MathHtmlText] therefore asks for it only for the question text.
     */
    fun preprocessDataValueFormula(html: String): String =
        html.replace(formulaDataValueRegex) { match -> "\\(${match.value}\\)" }

    private data class IconKey(val latex: String, val style: Int, val sizePx: Int, val color: Int)

    // Hanya diakses dari main thread. Menjaga rebind (seleksi opsi, drag pasangan) tidak mem-parse
    // ulang rumus yang sama.
    private val iconCache = LruCache<IconKey, TeXIcon>(64)

    /**
     * @param textColor warna rumus. Rumus tidak ikut warna teks TextView, jadi pemanggil yang warna
     * teksnya berubah-ubah (mis. putih saat opsi terpilih) wajib mengoper warna akhirnya.
     * @param enableLinks false untuk teks di dalam baris yang bisa diklik: `LinkMovementMethod`
     * membuat TextView menelan sentuhan sehingga klik baris dan drag-drop tidak sampai.
     */
    fun setTextWithMath(
        textView: TextView,
        rawHtml: String,
        @ColorInt textColor: Int = textView.currentTextColor,
        enableLinks: Boolean = true
    ) {
        val spanned = HtmlCompat.fromHtml(
            rawHtml,
            HtmlCompat.FROM_HTML_MODE_LEGACY,
            imageGetter(),
            null
        )

        val builder = SpannableStringBuilder(spanned)
        val ranges = findMathRanges(builder.toString())

        val sizePx = textView.textSize
        val maxWidthPx = availableWidth(textView)

        // Dari belakang ke depan supaya indeks rumus di depannya tetap valid setelah replace.
        for (range in ranges.asReversed()) {
            // Rumus yang gagal di-parse dibiarkan sebagai teks sumber supaya terlihat, bukan hilang.
            val icon = obtainIcon(range, sizePx, textColor) ?: continue
            builder.replace(range.start, range.end, PLACEHOLDER)
            builder.setSpan(
                ImageSpan(TeXIconDrawable(icon, maxWidthPx), ImageSpan.ALIGN_BOTTOM),
                range.start,
                range.start + PLACEHOLDER.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = builder
        if (enableLinks) textView.movementMethod = LinkMovementMethod.getInstance()
    }

    /** Lebar yang tersedia bagi rumus; sebelum TextView ter-layout dipakai perkiraan dari layar. */
    fun availableWidth(textView: TextView): Int {
        val padding = textView.paddingLeft + textView.paddingRight
        val laidOut = textView.width - padding
        if (laidOut > 0) return laidOut

        val dm = textView.resources.displayMetrics
        val marginReserve = (32 * dm.density).toInt()
        return (dm.widthPixels - padding - marginReserve).coerceAtLeast(1)
    }

    private fun imageGetter(): Html.ImageGetter = Html.ImageGetter { _ ->
        // Placeholder kosong untuk <img> di HTML. Gambar soal ditampilkan di ImageView terpisah.
        ColorDrawable(Color.TRANSPARENT).apply {
            setBounds(0, 0, 0, 0)
        }
    }

    private fun obtainIcon(range: MathRange, sizePx: Float, @ColorInt color: Int): TeXIcon? {
        val style = if (range.display) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT
        val key = IconKey(range.latex, style, sizePx.roundToInt(), color)
        iconCache.get(key)?.let { return it }

        return try {
            TeXFormula(range.latex).createTeXIcon(style, sizePx).also { icon ->
                icon.setForeground(TexColor(color))
                iconCache.put(key, icon)
            }
        } catch (e: Exception) {
            Timber.w(e, "Rumus LaTeX tidak valid, ditampilkan sebagai teks: %s", range.latex)
            null
        }
    }

    /**
     * Menggambar [TeXIcon] langsung ke Canvas (tanpa bitmap) sehingga tetap tajam saat diperkecil
     * agar muat di [maxWidthPx].
     */
    private class TeXIconDrawable(
        private val icon: TeXIcon,
        maxWidthPx: Int
    ) : Drawable() {

        private val graphics = AndroidGraphics2D()
        private val scale: Float
        private val widthPx: Int
        private val heightPx: Int

        init {
            val w = icon.iconWidth
            val h = icon.iconHeight
            scale = if (w > maxWidthPx && w > 0) maxWidthPx.toFloat() / w else 1f
            widthPx = (w * scale).roundToInt().coerceAtLeast(1)
            heightPx = (h * scale).roundToInt().coerceAtLeast(1)
            setBounds(0, 0, widthPx, heightPx)
        }

        override fun draw(canvas: Canvas) {
            val save = canvas.save()
            try {
                canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
                if (scale != 1f) canvas.scale(scale, scale)
                // setCanvas menangkap matriks canvas saat ini, jadi harus setelah translate/scale.
                graphics.setCanvas(canvas)
                icon.paintIcon(null, graphics, 0, 0)
            } finally {
                canvas.restoreToCount(save)
            }
        }

        override fun getIntrinsicWidth(): Int = widthPx

        override fun getIntrinsicHeight(): Int = heightPx

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
