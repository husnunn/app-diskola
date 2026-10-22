package id.diskola.app.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.CurrentSubjectData
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.utils.HubRules
import id.diskola.app.viewmodel.HomeProfile

// Previews with the awkward content the hub must survive: very long names, server messages and event names, at the default and
// a large font scale, light and dark.

private val LONG_SCHOOL = "Sekolah Menengah Kejuruan Negeri Teknologi dan Rekayasa Informatika Terpadu Kota Surabaya Timur"
private val LONG_NAME = "Muhammad Husnun Niam Abdurrahman Al-Faruqi bin Ahmad Zainuddin"
private val LONG_MESSAGE = "Pengajuan verifikasi data Anda sedang ditinjau oleh admin sekolah. Proses ini dapat memakan waktu hingga tiga hari kerja; " +
    "pastikan NISN dan nama lengkap yang Anda kirim sesuai dengan data di sekolah, lalu hubungi admin bila belum ada kabar."

private val longProfile = HomeProfile(
    schoolName = LONG_SCHOOL,
    schoolLogo = "",
    userName = LONG_NAME,
    userAvatar = "",
    classLine = "Teknik Komputer dan Jaringan Kelas Sebelas Angkatan Dua Ribu Dua Puluh Empat Rombel Sepuluh",
    isStudent = true,
    isTeacher = false,
    isGuest = false,
    isHavingClass = true,
    klaspayActive = true,
)

private val longSubject = CurrentSubjectData(
    id = 3,
    attendance_id = 9,
    plot_start_at = "07:00",
    plot_end_at = "08:30",
    subject_name = "Dasar-Dasar Program Keahlian Pengembangan Perangkat Lunak dan Gim Tingkat Lanjut",
)

private val longBanner = HubRules.AttendanceBanner(
    state = HubRules.BannerState.CHECK_IN,
    title = "Wajib absen masuk sekarang sebelum jam pertama dimulai",
    workHours = "Jam kerja hari ini · 07:00 – 15:00",
    eventName = "Rapat Koordinasi Kurikulum Merdeka dan Persiapan Asesmen Sumatif Akhir Semester Gasal Seluruh Program Keahlian",
    source = "Dari Kalender sekolah",
    info = LONG_MESSAGE,
    showCta = true,
)

@Composable
private fun PreviewColumn(content: @Composable () -> Unit) {
    DiskolaTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

@Preview(name = "Header, long names", showBackground = true, widthDp = 360)
@Preview(name = "Header, long names, dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Header, large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun HeaderPreview() {
    DiskolaTheme { HomeHeader(profile = longProfile, bellCount = 120, loading = false, onRefresh = {}, onBell = {}) }
}

@Preview(name = "Class card, long subject", showBackground = true, widthDp = 360)
@Preview(name = "Class card, dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Class card, large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun ClassCardPreview() {
    PreviewColumn {
        StudentClassCard(longSubject, schoolLogo = "", action = HubRules.ClassAction.CHECK_IN, onCheckIn = {}, onIsiJurnal = {})
        StudentClassCard(null, schoolLogo = "", action = HubRules.ClassAction.NONE, onCheckIn = {}, onIsiJurnal = {})
    }
}

@Preview(name = "Verification notices", showBackground = true, widthDp = 360)
@Preview(name = "Verification notices, dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VerifyPreview() {
    PreviewColumn {
        VerifyDataButton(onClick = {})
        VerifyBlockedCard(LONG_MESSAGE, rejected = false)
        VerifyBlockedCard(LONG_MESSAGE, rejected = true)
        StudentNoClassCard()
    }
}

@Preview(name = "Attendance banner, long texts", showBackground = true, widthDp = 360)
@Preview(name = "Attendance banner, dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Attendance banner, large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun BannerPreview() {
    PreviewColumn {
        AttendanceBannerCard(longBanner, loading = false, onPresensi = {})
        AttendanceBannerCard(
            longBanner.copy(state = HubRules.BannerState.HOLIDAY, title = "Hari libur", eventName = "", source = "", info = LONG_MESSAGE, showCta = false),
            loading = false,
            onPresensi = {},
        )
    }
}

@Preview(name = "Menu tiles", showBackground = true, widthDp = 360)
@Preview(name = "Menu tiles, large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun MenuPreview() {
    PreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val teacher = menuTilesFor(longProfile.copy(isStudent = false, isTeacher = true))
            teacher.take(3).forEach { MenuGridTile(it, badge = 0, onClick = {}, modifier = Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val locked = menuTilesFor(longProfile.copy(isStudent = false, isTeacher = true, isHavingClass = false))
            locked.drop(3).take(3).forEach { MenuGridTile(it, badge = 12, onClick = {}, modifier = Modifier.weight(1f)) }
        }
    }
}
