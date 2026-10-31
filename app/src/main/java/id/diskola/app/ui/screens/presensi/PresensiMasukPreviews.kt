package id.diskola.app.ui.screens.presensi

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.PresensiCheckData
import id.diskola.app.dataclass.ResponData.PresensiSchedule
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.utils.PresensiRules.MasukStatus
import java.time.LocalDateTime

// The Masuk/Pulang screen in each state the status card can be in, with a long user name and the schedule the server sends. The map is a
// plain placeholder (GoogleMap does not render in a preview) inside the real map frame, so the frame, the distance pill and the button
// are what the screen shows. The app is light at runtime; the dark and large-font variants guard the tokens and the wrapping.

private val schedule = PresensiSchedule(start_at = "07:00:00", end_at = "15:30:00", source = "settings")

private val checkIn = PresensiCheckData(
    allow_attendance = true,
    type_attendance = "checkin",
    attendance_response_text_button = "Absen Masuk",
    schedule = schedule,
)

private val checkOut = checkIn.copy(type_attendance = "checkout", attendance_response_text_button = "Absen Pulang")

private val base = MasukViewState(
    userName = "Muhammad Rizky Ramadhan Putra Pratama",
    userAvatar = "",
    lastLabel = "Presensi terakhir 2026-10-29, 15:42",
    check = checkIn,
    status = MasukStatus.Ready("Absen Masuk"),
    restricted = true,
    radius = 100f,
    distance = 45f,
    locationOff = false,
    submitting = false,
    errorMessage = "",
)

@Composable
private fun PreviewMap(distance: Float?) {
    val scheme = MaterialTheme.colorScheme
    MapFrame(
        onMyLocation = if (distance != null) ({}) else null,
        modifier = Modifier.mapCardShadow(),
        overlay = { distance?.let { DistancePill(it) } },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Place, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MasukPreview(state: MasukViewState) {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        val clock = remember { mutableStateOf(LocalDateTime.of(2026, 10, 30, 7, 42, 17)) }
        MasukContent(
            state = state,
            clock = clock,
            onBack = {},
            onSubmit = {},
            onRetry = {},
            onEnableLocation = {},
            onDismissError = {},
        ) { PreviewMap(state.distance) }
    }
}

@Preview(name = "Masuk, inside radius", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(name = "Masuk, inside radius, dark", showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Masuk, inside radius, large font", showBackground = true, widthDp = 360, heightDp = 1100, fontScale = 1.5f)
@Composable
private fun InsideRadiusPreview() = MasukPreview(base)

@Preview(name = "Pulang, outside radius", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(name = "Pulang, outside radius, dark", showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OutsideRadiusPreview() = MasukPreview(
    base.copy(check = checkOut, status = MasukStatus.OutsideRadius, distance = 1234f),
)

@Preview(name = "Waiting for location, location off", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LocationOffPreview() = MasukPreview(
    base.copy(status = MasukStatus.WaitingLocation, locationOff = true, distance = null),
)

@Preview(name = "Waiting for location", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun WaitingLocationPreview() = MasukPreview(base.copy(status = MasukStatus.WaitingLocation, distance = null))

@Preview(name = "Loading the check", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoadingPreview() = MasukPreview(base.copy(check = null, status = MasukStatus.Loading, lastLabel = "", distance = null))

@Preview(name = "Server says no", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ServerMessagePreview() = MasukPreview(
    base.copy(status = MasukStatus.ServerMessage("School Calendar Event is not available for today"), distance = null),
)

@Preview(name = "Check failed, with error banner", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CheckFailedPreview() = MasukPreview(
    base.copy(check = null, status = MasukStatus.CheckFailed, errorMessage = "Tidak dapat terhubung ke server", lastLabel = "", distance = null),
)

@Preview(name = "No radius enforced, submitting", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun UnrestrictedSubmittingPreview() = MasukPreview(base.copy(restricted = false, submitting = true, distance = 5400f))
