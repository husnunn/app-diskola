package id.diskola.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The proof photo of Presensi Dinas Luar (doc `07` §6.3–6.4): alamat, koordinat and waktu are burnt
 * into the picture so the evidence cannot be separated from where/when it was taken. The result is
 * upright (EXIF applied), not mirrored, JPEG q90 — and, unlike legacy, capped at 2560 px on the long side.
 */
object OffsiteWatermark {

    private const val MAX_SIDE = 2560
    private const val DIR = "offsite_photos"
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale("id", "ID"))

    /**
     * `inSampleSize` for decoding: the largest power of two that still leaves the long side at or above [maxSide] (the picture is then
     * scaled down to exactly [maxSide]). The old rule took the smallest power of two that got under the cap, so a 4000 px photo for a
     * 1600 px cap was decoded at 1000 px.
     */
    fun sampleSize(longSide: Int, maxSide: Int): Int {
        var sample = 1
        while (longSide / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    /** [width]×[height] scaled down, keeping the ratio, so the long side is [maxSide]; unchanged when it already fits. */
    fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longSide = maxOf(width, height)
        if (longSide <= maxSide) return width to height
        val factor = maxSide.toFloat() / longSide
        return Math.round(width * factor) to Math.round(height * factor)
    }

    /** (1) address ≤ 200 chars or `-`, (2) `%.6f, %.6f`, (3) `dd MMM yyyy HH:mm`. */
    fun lines(address: String, lat: Double, lng: Double, time: LocalDateTime = LocalDateTime.now()): List<String> = listOf(
        address.trim().take(200).ifBlank { "-" },
        String.format(Locale.US, "%.6f, %.6f", lat, lng),
        TIME_FORMAT.format(time),
    )

    /**
     * Same stamp for the Jurnal KBM scene photo, where location is best-effort: a blank address and unknown
     * coordinates simply leave their lines out (the time line is always there).
     */
    fun linesOptional(address: String?, lat: Double?, lng: Double?, time: LocalDateTime = LocalDateTime.now()): List<String> = buildList {
        address?.trim()?.take(200)?.takeIf { it.isNotBlank() }?.let(::add)
        if (lat != null && lng != null) add(String.format(Locale.US, "%.6f, %.6f", lat, lng))
        add(TIME_FORMAT.format(time))
    }

    /**
     * Decodes [source] (already a JPEG from the camera), rotates it upright, stamps [lines], writes the result.
     * [maxBytes] set = re-encode from q90 down to q40 (steps of 10) until the file fits; [maxSide] caps the long side.
     */
    suspend fun process(
        context: Context,
        source: File,
        lines: List<String>,
        maxSide: Int = MAX_SIDE,
        maxBytes: Long? = null,
        prefix: String = "OFFSITE",
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val sample = sampleSize(maxOf(bounds.outWidth, bounds.outHeight), maxSide)
            val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@runCatching null

            val degrees = when (ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val upright = if (degrees == 0f) decoded else Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true,
            )
            val (fitWidth, fitHeight) = scaledSize(upright.width, upright.height, maxSide)
            val fitted = if (fitWidth == upright.width && fitHeight == upright.height) upright else Bitmap.createScaledBitmap(upright, fitWidth, fitHeight, true)
            val stamped = stamp(fitted.copy(Bitmap.Config.ARGB_8888, true), lines)

            val dir = File(context.cacheDir, DIR).apply { mkdirs() }
            val out = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            var quality = 90
            while (true) {
                out.outputStream().use { stamped.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                if (maxBytes == null || out.length() <= maxBytes || quality <= 40) break
                quality -= 10
            }
            source.delete()
            out
        }.getOrNull()
    }

    /** White bold text on a black band (alpha 170) along the bottom edge. */
    private fun stamp(bitmap: Bitmap, lines: List<String>): Bitmap {
        val textSize = maxOf(24f, bitmap.width * 0.035f)
        val padding = textSize
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
        }
        val width = (bitmap.width - 2 * padding).toInt().coerceAtLeast(1)
        val layouts = lines.map { line ->
            StaticLayout.Builder.obtain(line, 0, line.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.35f)
                .build()
        }
        val textHeight = layouts.sumOf { it.height }
        val bandHeight = textHeight + 2 * padding
        val canvas = Canvas(bitmap)
        val top = bitmap.height - bandHeight
        canvas.drawRect(0f, top, bitmap.width.toFloat(), bitmap.height.toFloat(), Paint().apply { color = Color.argb(170, 0, 0, 0) })
        var y = top + padding
        layouts.forEach { layout ->
            canvas.save()
            canvas.translate(padding, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height
        }
        return bitmap
    }
}
