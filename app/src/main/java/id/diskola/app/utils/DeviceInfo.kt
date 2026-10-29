package id.diskola.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.BuildConfig
import id.diskola.app.utils.session.SessionStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class DeviceInfoRow(val label: String, val value: String, val monospace: Boolean = false)

data class DeviceInfoGroup(val title: String, val rows: List<DeviceInfoRow>)

/**
 * Pure formatting for "Tentang Perangkat" (Akun): what a user reads off the screen and what "Salin informasi" puts on the clipboard,
 * so support can see which app version, Android version and device a bug report came from.
 */
object DeviceInfoRules {

    const val NOT_AVAILABLE = "Tidak tersedia"

    private const val KB = 1024L
    private const val MB = KB * KB
    private const val GB = MB * KB

    /** A row whose value the device or session did not give is spelled out instead of left blank. */
    fun row(label: String, value: String?, monospace: Boolean = false) =
        DeviceInfoRow(label, value?.trim().orEmpty().ifBlank { NOT_AVAILABLE }, monospace)

    /** "Samsung SM-A515F" — some makers already put their name in the model, which must not be said twice. */
    fun deviceName(manufacturer: String, model: String): String {
        val maker = manufacturer.trim()
        val name = model.trim()
        return when {
            maker.isEmpty() && name.isEmpty() -> NOT_AVAILABLE
            maker.isEmpty() -> name
            name.isEmpty() -> maker.replaceFirstChar { it.uppercaseChar() }
            name.startsWith(maker, ignoreCase = true) -> name
            else -> "${maker.replaceFirstChar { it.uppercaseChar() }} $name"
        }
    }

    /** "3.7 GB" / "512 MB"; a negative size means the system could not tell. */
    fun formatBytes(bytes: Long): String = when {
        bytes < 0 -> NOT_AVAILABLE
        bytes >= GB -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / GB)
        else -> String.format(Locale.US, "%d MB", bytes / MB)
    }

    /** `capturedAt` is already formatted: it ties a report to the server logs of that moment. */
    fun toPlainText(groups: List<DeviceInfoGroup>, capturedAt: String): String = buildString {
        appendLine("Diskola - Informasi Perangkat")
        appendLine("Dicatat: $capturedAt")
        groups.forEach { group ->
            appendLine()
            appendLine("[${group.title}]")
            group.rows.forEach { appendLine("${it.label}: ${it.value}") }
        }
    }.trimEnd()
}

/** Reads the facts "Tentang Perangkat" shows. Everything here is the user's own device or session; nothing is sent anywhere. */
class DeviceInfoReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val deviceUtil: DeviceUtil,
    private val dateUtil: DateUtil,
    private val sessionStore: SessionStore,
) {

    fun read(): List<DeviceInfoGroup> = listOf(aplikasi(), perangkat(), layarDanSistem(), akun())

    /** Local time with its zone, e.g. `2026-10-28 14:03:11 (Asia/Jakarta)`. */
    fun capturedAt(): String {
        val zone = TimeZone.getDefault()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = zone }.format(Date())
        return "$stamp (${zone.id})"
    }

    private fun aplikasi() = DeviceInfoGroup(
        "Aplikasi",
        listOf(
            DeviceInfoRules.row("Versi aplikasi", BuildConfig.VERSION_NAME),
            DeviceInfoRules.row("Nomor build", BuildConfig.VERSION_CODE.toString()),
            DeviceInfoRules.row("Jenis build", BuildConfig.BUILD_TYPE),
            DeviceInfoRules.row("ID paket", BuildConfig.APPLICATION_ID),
        ),
    )

    private fun perangkat() = DeviceInfoGroup(
        "Perangkat",
        listOf(
            // The id the server checks a single-device login against.
            DeviceInfoRules.row("Identitas Perangkat", deviceIdProvider.get(), monospace = true),
            DeviceInfoRules.row("Model", DeviceInfoRules.deviceName(Build.MANUFACTURER, Build.MODEL)),
            DeviceInfoRules.row("Versi Android", Build.VERSION.RELEASE),
            DeviceInfoRules.row("API Level", Build.VERSION.SDK_INT.toString()),
            DeviceInfoRules.row("Patch keamanan", Build.VERSION.SECURITY_PATCH),
            DeviceInfoRules.row("Versi sistem", Build.DISPLAY),
            DeviceInfoRules.row("Arsitektur", Build.SUPPORTED_ABIS.firstOrNull()),
        ),
    )

    private fun layarDanSistem(): DeviceInfoGroup {
        val metrics = context.resources.displayMetrics
        val zone = TimeZone.getDefault()
        return DeviceInfoGroup(
            "Layar dan sistem",
            listOf(
                DeviceInfoRules.row("Layar", "${metrics.widthPixels} × ${metrics.heightPixels} px · ${metrics.densityDpi} dpi"),
                DeviceInfoRules.row("Skala font", String.format(Locale.US, "%.2f×", context.resources.configuration.fontScale)),
                DeviceInfoRules.row("Bahasa", Locale.getDefault().toLanguageTag()),
                DeviceInfoRules.row("Zona waktu", zone.id),
                DeviceInfoRules.row("Waktu otomatis", if (dateUtil.isTimeAutomatic(context)) "Aktif" else "Nonaktif"),
                DeviceInfoRules.row("RAM", DeviceInfoRules.formatBytes(totalRam())),
                DeviceInfoRules.row("Penyimpanan kosong", DeviceInfoRules.formatBytes(freeStorage())),
            ),
        )
    }

    private fun akun(): DeviceInfoGroup {
        val rows = mutableListOf(
            DeviceInfoRules.row("ID pengguna", sessionStore.userId.takeIf { it > 0 }?.toString()),
            DeviceInfoRules.row("ID sekolah", sessionStore.school.id.takeIf { it > 0 }?.toString()),
            DeviceInfoRules.row("Peran", if (sessionStore.isTeacher) "Guru" else "Siswa"),
        )
        // The Asesmen exam password is bound to this value ("perangkat lain" errors), and Asesmen is for students only.
        if (!sessionStore.isTeacher) {
            rows += DeviceInfoRules.row("ID ujian (Asesmen)", deviceUtil.getFingerprint(), monospace = true)
        }
        return DeviceInfoGroup("Akun", rows)
    }

    private fun totalRam(): Long = try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }.totalMem
    } catch (_: Exception) {
        -1L
    }

    private fun freeStorage(): Long = try {
        StatFs(context.filesDir.path).availableBytes
    } catch (_: Exception) {
        -1L
    }
}
