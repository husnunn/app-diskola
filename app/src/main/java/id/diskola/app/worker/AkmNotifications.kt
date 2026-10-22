package id.diskola.app.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.diskola.app.R

/** The silent "Sinkronisasi Soal" channel and the notifications of the exam workers (download progress, upload result). */
object AkmNotifications {

    fun ensureChannel(context: Context): String {
        val channelId = "${context.getString(R.string.app_name)}.silent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                manager.createNotificationChannel(NotificationChannel(channelId, "Sinkronisasi Soal", NotificationManager.IMPORTANCE_MIN))
            }
        }
        return channelId
    }

    @SuppressLint("MissingPermission")
    fun notifyIfAllowed(context: Context, id: Int, notification: Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun downloadId(akmId: Int): Int = "akm_download_$akmId".hashCode()

    fun uploadId(akmId: Int): Int = "akm_upload_$akmId".hashCode()

    /** "Jawaban berhasil terkirim" / "Jawaban gagal terkirim" (legacy `AkmUploader.kt:154-186`, without its literal quote marks). */
    fun showUploadResult(context: Context, akmId: Int, examName: String, success: Boolean) {
        val name = examName.ifBlank { "asesmen" }
        val notification = NotificationCompat.Builder(context, ensureChannel(context))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (success) "Jawaban berhasil terkirim" else "Jawaban gagal terkirim")
            .setContentText(if (success) "Jawaban ujian $name telah berhasil dikirim" else "Jawaban ujian $name gagal dikirim")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifyIfAllowed(context, uploadId(akmId), notification)
    }
}
