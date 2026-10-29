package id.diskola.app.ui.screens.akun

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.utils.DeviceInfoGroup
import id.diskola.app.utils.DeviceInfoRules.row

// "Tentang Perangkat" with the values that make it wrap: a long system build string, a long device name and a missing field,
// at the default and a large font scale. The app is light-only, so there is no dark variant.

private val sampleGroups = listOf(
    DeviceInfoGroup(
        "Aplikasi",
        listOf(row("Versi aplikasi", "2.2.84"), row("Nomor build", "83"), row("Jenis build", "release"), row("ID paket", "id.diskola.app")),
    ),
    DeviceInfoGroup(
        "Perangkat",
        listOf(
            row("Identitas Perangkat", "9f3c2a71b04d5e68", monospace = true),
            row("Model", "Samsung SM-A515F Galaxy A51 Edisi Khusus"),
            row("Versi Android", "14"),
            row("API Level", "34"),
            row("Patch keamanan", null),
            row("Versi sistem", "UP1A.231005.007.A515FXXSBFXB1"),
            row("Arsitektur", "arm64-v8a"),
        ),
    ),
    DeviceInfoGroup(
        "Layar dan sistem",
        listOf(
            row("Layar", "1080 × 2400 px · 420 dpi"),
            row("Skala font", "1.00×"),
            row("Bahasa", "id-ID"),
            row("Zona waktu", "Asia/Jakarta"),
            row("Waktu otomatis", "Aktif"),
            row("RAM", "3.7 GB"),
            row("Penyimpanan kosong", "12.3 GB"),
        ),
    ),
    DeviceInfoGroup(
        "Akun",
        listOf(row("ID pengguna", "1042"), row("ID sekolah", "7"), row("Peran", "Siswa"), row("ID ujian (Asesmen)", "5b1e6c0e-2f6d-4d0a-9d52-8c7a1e3f4b90", monospace = true)),
    ),
)

@Preview(name = "Tentang Perangkat", showBackground = true, widthDp = 360, heightDp = 1500)
@Preview(name = "Tentang Perangkat, large font", showBackground = true, widthDp = 360, heightDp = 2200, fontScale = 1.5f)
@Composable
private fun DeviceInfoPreview() {
    DiskolaTheme { DeviceInfoContent(groups = sampleGroups, copied = false, onCopy = {}, onBack = {}) }
}

@Preview(name = "Tentang Perangkat, copied", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DeviceInfoCopiedPreview() {
    DiskolaTheme { DeviceInfoContent(groups = sampleGroups, copied = true, onCopy = {}, onBack = {}) }
}
