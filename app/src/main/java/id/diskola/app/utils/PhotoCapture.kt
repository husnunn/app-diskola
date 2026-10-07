package id.diskola.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Camera target + upload preparation for Poin's optional photo. The Compose side only needs a
 * `Uri` to hand to `TakePicture`/`PickVisualMedia`; this turns whatever comes back into an
 * EXIF-rotated JPEG of at most [MAX_BYTES], the same contract as legacy `IntentUtil.compressBitmap`
 * (quality 80, stepping down by 5) — except a photo that can never fit is reported (null) instead of
 * silently sent without the picture.
 */
object PhotoCapture {

    const val MAX_BYTES = 1_000_000
    private const val MAX_SIDE = 2560
    private const val DIR = "poin_photos"

    /** A fresh empty file in the shared cache, exposed through the app's FileProvider. */
    fun newCameraUri(context: Context): Uri {
        val file = File.createTempFile("JPEG_${stamp()}_", ".jpg", photoDir(context))
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    /** Returns the compressed copy, or null if [uri] is unreadable or cannot get under [maxBytes]. */
    suspend fun compress(context: Context, uri: Uri, maxBytes: Int = MAX_BYTES): File? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val degrees = resolver.openInputStream(uri)?.use { rotationDegrees(ExifInterface(it)) } ?: 0
            val bitmap = if (degrees == 0) decoded else Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees.toFloat()) }, true,
            )

            var quality = 80
            while (quality >= 5) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (out.size() <= maxBytes) {
                    val file = File(photoDir(context), "JPEG_${stamp()}_${System.nanoTime()}.jpg")
                    file.writeBytes(out.toByteArray())
                    return@runCatching file
                }
                quality -= 5
            }
            null
        }.getOrNull()
    }

    private fun rotationDegrees(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun photoDir(context: Context): File = File(context.cacheDir, DIR).apply { mkdirs() }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
