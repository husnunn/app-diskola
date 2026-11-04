package id.diskola.app.ui.screens.magang

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import id.diskola.app.dataclass.ResponData.MagangAttendanceState
import id.diskola.app.dataclass.ResponData.MagangCompany
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.dataclass.ResponData.MagangScheduleTime
import id.diskola.app.ui.theme.DiskolaTheme
import java.time.LocalDate

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
