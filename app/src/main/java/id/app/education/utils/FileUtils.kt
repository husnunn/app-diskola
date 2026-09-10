package id.app.education.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import id.app.education.apiservice.CommonApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import javax.inject.Inject

class FileUtils @Inject constructor(val commonApiService: CommonApiService) {

    suspend fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
        val newHeight = if (height > width) height - (height - width) else height
        var cropW = (width - height) / 2
        cropW = if (cropW < 0) 0 else cropW
        var cropH = (height - width) / 2
        cropH = if (cropH < 0) 0 else cropH
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)
    }

    suspend fun resizeBitmap(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = image.width
        val height = image.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
    }

    suspend fun bitmapToExternalFile(context: Context, bitmap: Bitmap, fileName: String): File? =
        try {
            val file =
                File(getExternalDir(context), fileName).apply {
                    if (!exists()) {
                        withContext(Dispatchers.IO) { createNewFile() }
                    }
                }
            bitmapToFile(bitmap, file)
            context.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(Uri.fromFile(file))
            )
            file
        } catch (e: IOException) {
            Timber.e(e, "BitmapFile - error: ${e.localizedMessage}")
            null
        }

    fun getExternalDir(context: Context): File = try {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "onklas"
        )
    } catch (_: IOException) {
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
    }.apply { if (!exists()) mkdirs() }

    suspend fun bitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, it)
                FileOutputStream(file).apply {
                    write(it.toByteArray())
                    flush()
                    close()
                }
            }
            true
        } catch (e: Exception) {
            Timber.e("failed to created file")
            Timber.e(e)
            false
        }
    }

    suspend fun downloadImage(uri: String, file: File): Boolean = try {
        val byteArray = commonApiService.download(uri).bytes()
        bitmapToFile(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size), file)
    } catch (e: Exception) {
        Timber.e("Failed to download image: $uri")
        Timber.e(e)
        false
    }

    fun getStringSizeLengthFile(size: Long): String {
        val df = DecimalFormat("0.00")
        val sizeKb = 1024.0f
        val sizeMb = sizeKb * sizeKb
        val sizeGb = sizeMb * sizeKb
        val sizeTerra = sizeGb * sizeKb

        return when {
            size < sizeMb -> df.format(size / sizeKb) + " Kb"
            size < sizeGb -> df.format(size / sizeMb) + " Mb"
            size < sizeTerra -> df.format(size / sizeGb) + " Gb"
            else -> "$size b"
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br></br>
     * <br></br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    fun getPath(context: Context, uri: Uri): String? {
        when {
            isExternalStorageDocument(uri) -> {
                // DocumentProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
                return null
            }
            isDownloadsDocument(uri) -> {
                // DownloadsProvider
                val id = DocumentsContract.getDocumentId(uri)
                try {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    return getDataColumn(context, contentUri, null, null)
                } catch (e: NumberFormatException) {
                    if (id.startsWith("raw:/")) {
                        val newPath = id.substring("raw:/".length)
                        val exists = File(newPath).exists()
                        if (exists)
                            return newPath
                    }
                }
            }
            isMediaDocument(uri) -> {
                // MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        }
        return when {
            "content".equals(uri.scheme!!, ignoreCase = true) -> // MediaStore (and general)
                // Return the remote address
                if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                    context,
                    uri,
                    null,
                    null
                )
            "file".equals(uri.scheme!!, ignoreCase = true) -> // File
                uri.path
            else -> null
        }
    }

    /**
     * Get the value of the postMedia column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}