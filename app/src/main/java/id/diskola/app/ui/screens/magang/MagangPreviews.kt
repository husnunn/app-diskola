package id.diskola.app.ui.screens.magang

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.MagangAttendanceState
import id.diskola.app.dataclass.ResponData.MagangCompany
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.dataclass.ResponData.MagangScheduleTime
import id.diskola.app.ui.screens.presensi.DistancePill
import id.diskola.app.ui.screens.presensi.MapFrame
import id.diskola.app.ui.screens.presensi.mapCardShadow
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.utils.PresensiRules.MasukStatus
import id.diskola.app.viewmodel.MagangAttendView
import java.time.LocalDate
import java.time.LocalDateTime

// The Jadwal tab in the states a student sees it in: a day with both buttons and a long company name, a day that is already done (status
// text, no button), an empty day, the first load and a failed first load. The app is light at runtime; the dark and large-font variants guard
// the tokens and the wrapping.

private val day = LocalDate.of(2026, 6, 4)

private fun schedule(id: Long, name: String, state: MagangAttendanceState) = MagangScheduleItem(
    id = id,
    company = MagangCompany(id = 1, name = name),
    schedule = MagangScheduleTime(date = "2026-06-04", start_time = "15:00:00", end_time = "19:15:00"),
    attendance = state,
)

private val withButtons = schedule(
    1,
    "PT. Teknologi Pendidikan Nusantara Berkembang Bersama",
    MagangAttendanceState(status = "Belum presensi", can_attend = true, can_leave = true),
)

private val done = schedule(2, "PT. TESTING MAGANG", MagangAttendanceState(status = "Hadir"))

@Composable
private fun JadwalPreview(
    schedules: List<MagangScheduleItem>,
    loaded: Boolean = true,
    loading: Boolean = false,
    errorMessage: String = "",
    selected: LocalDate = day,
) {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            JadwalTab(
                selected = selected,
                schedules = schedules,
                loaded = loaded,
                loading = loading,
                errorMessage = errorMessage,
                onSelectDate = {},
                onPickMonth = {},
                onRefresh = {},
                onRetry = {},
                onDismissError = {},
                onMasuk = {},
                onLaporan = {},
                modifier = Modifier,
            )
        }
    }
}

@Preview(name = "Jadwal, buttons", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Jadwal, buttons, dark", showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Jadwal, buttons, large font", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.5f)
@Composable
private fun ButtonsPreview() = JadwalPreview(listOf(withButtons, done))

@Preview(name = "Jadwal, done", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun DonePreview() = JadwalPreview(listOf(done))

@Preview(name = "Jadwal, empty day", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Jadwal, empty day, dark", showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() = JadwalPreview(listOf(withButtons), selected = LocalDate.of(2026, 6, 5))

@Preview(name = "Jadwal, loading", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoadingPreview() = JadwalPreview(emptyList(), loaded = false, loading = true)

@Preview(name = "Jadwal, first load failed", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ErrorPreview() = JadwalPreview(emptyList(), loaded = false, errorMessage = "Tidak dapat terhubung ke server.")

// ---- Presensi Masuk ----
// The check-in screen in each state its status card can be in. The map is a plain placeholder (GoogleMap does not render in a preview)
// inside the real map frame; the long company name and address guard the wrapping at the large font.

private val attendView = MagangAttendView(
    companyName = "PT. Teknologi Pendidikan Nusantara Berkembang Bersama",
    address = "Jl. Raya Bratang Binangun No.85 B, Baratajaya, Kec. Gubeng, Surabaya, Jawa Timur 60284, Indonesia",
    dateLabel = "04 Nov 2026",
    timeLabel = "08:00 – 16:00",
    lat = -7.2942595,
    lng = 112.7597888,
    radius = 100f,
    enforced = true,
    distance = 45f,
    status = MasukStatus.Ready("Konfirmasi"),
)

@Composable
private fun AttendPreview(view: MagangAttendView, locationOff: Boolean = false, submitting: Boolean = false, errorMessage: String = "") {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        val clock = remember { mutableStateOf(LocalDateTime.of(2026, 11, 4, 8, 15, 30)) }
        MagangAttendContent(
            view = view,
            userName = "Muhammad Rizky Ramadhan Putra Pratama",
            userAvatar = "",
            clock = clock,
            locationOff = locationOff,
            submitting = submitting,
            errorMessage = errorMessage,
            onBack = {},
            onConfirm = {},
            onEnableLocation = {},
            onDismissError = {},
        ) {
            val scheme = MaterialTheme.colorScheme
            MapFrame(
                onMyLocation = if (view.distance != null) ({}) else null,
                modifier = Modifier.mapCardShadow(),
                overlay = { view.distance?.let { DistancePill(it, place = "lokasi magang") } },
            ) {
                Box(modifier = Modifier.fillMaxSize().background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Place, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Preview(name = "Masuk, inside radius", showBackground = true, widthDp = 360, heightDp = 860)
@Preview(name = "Masuk, inside radius, dark", showBackground = true, widthDp = 360, heightDp = 860, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Masuk, inside radius, large font", showBackground = true, widthDp = 360, heightDp = 1200, fontScale = 1.5f)
@Composable
private fun AttendInsidePreview() = AttendPreview(attendView)

@Preview(name = "Masuk, outside radius", showBackground = true, widthDp = 360, heightDp = 860)
@Composable
private fun AttendOutsidePreview() = AttendPreview(attendView.copy(distance = 1234f, status = MasukStatus.OutsideRadius))

@Preview(name = "Masuk, radius off", showBackground = true, widthDp = 360, heightDp = 860)
@Composable
private fun AttendRadiusOffPreview() = AttendPreview(attendView.copy(enforced = false, distance = null))

@Preview(name = "Masuk, waiting for location", showBackground = true, widthDp = 360, heightDp = 860)
@Composable
private fun AttendWaitingPreview() = AttendPreview(attendView.copy(distance = null, status = MasukStatus.WaitingLocation))

@Preview(name = "Masuk, location off", showBackground = true, widthDp = 360, heightDp = 860)
@Composable
private fun AttendLocationOffPreview() = AttendPreview(attendView.copy(distance = null, status = MasukStatus.WaitingLocation), locationOff = true)

@Preview(name = "Masuk, server says no", showBackground = true, widthDp = 360, heightDp = 860)
@Composable
private fun AttendServerPreview() = AttendPreview(attendView.copy(distance = null, status = MasukStatus.ServerMessage("Waktu presensi masuk sudah lewat")))

@Preview(name = "Masuk, submitting with error", showBackground = true, widthDp = 360, heightDp = 860)
@Composable
private fun AttendErrorPreview() = AttendPreview(attendView, errorMessage = "Anda berada di luar radius yang diizinkan.", submitting = true)
