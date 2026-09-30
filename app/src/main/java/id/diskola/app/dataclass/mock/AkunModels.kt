package id.diskola.app.dataclass.mock

/** UI-only mock model for Akun > Perangkat (device/session management) — no backend yet. */
data class DeviceSession(
    val id: String,
    val name: String,
    val isCurrent: Boolean,
    val lastUsedLabel: String,
)

object MockAkun {
    val devices = listOf(
        DeviceSession("d1", "Xiaomi Redmi Note 12", isCurrent = true, lastUsedLabel = "aktif sekarang"),
        DeviceSession("d2", "Samsung Galaxy A15", isCurrent = false, lastUsedLabel = "3 hari lalu"),
        DeviceSession("d3", "Tablet Lab Sekolah", isCurrent = false, lastUsedLabel = "2 minggu lalu"),
    )

    const val CHANGELOG_1 = "Alur masuk dirapikan: pilih sekolah kini lewat bottom sheet dengan pencarian."
    const val CHANGELOG_2 = "Mode gelap penuh di seluruh layar akun."
    const val CHANGELOG_3 = "Halaman Perangkat baru untuk mengeluarkan sesi yang tidak dikenali."
}
