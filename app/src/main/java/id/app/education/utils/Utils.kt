package id.app.education.utils

import android.content.Context
import android.graphics.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class Utils @Inject constructor(@ApplicationContext private val context: Context, private val pref: PreferenceClass) {

    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun logFile(msg: String = "") {
        try {
            val currentDate: String =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime: String =
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val fileName = "${pref.getInt("user_id")}_$currentDate.txt"

            val file = File(context.filesDir, fileName)
                .also { if (!it.exists()) it.createNewFile() }

            if (file.exists()) {
                val fos = FileOutputStream(file, true)
                fos.write("[$currentTime]: $msg\n".toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.close()
            }
        } catch (e: IOException) {
            Timber.tag("FileLog").e(e)
        }
    }

    fun circleCropBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = -0xbdbdbe
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(),
            (bitmap.width / 2).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}
