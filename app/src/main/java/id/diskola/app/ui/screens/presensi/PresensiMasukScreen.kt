package id.diskola.app.ui.screens.presensi

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.PresensiRules.MasukStatus
import id.diskola.app.utils.location.MockLocationGuard
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.PresensiMasukViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale("id"))
private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * "Masuk Sekolah" (doc `07` §5): live map with the school radius, the server-driven action button, and a
 * confirm → submit → success flow. Needs precise location only while this screen is open; there is
 * no background location and no geofence.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PresensiMasukScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresensiMasukViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val check by viewModel.check.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val restricted by viewModel.restricted.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()
    val lastLabel by viewModel.lastLabel.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val notConfigured by viewModel.schoolNotConfigured.collectAsStateWithLifecycle()

    var permissionDenied by remember { mutableStateOf(false) }
    var locationOff by remember { mutableStateOf(false) }
    var settingsCheckTick by remember { mutableIntStateOf(0) }
    var confirmVisible by remember { mutableStateOf(false) }
    var scheduleDialogDismissed by rememberSaveable { mutableStateOf(false) }

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ) { results ->
        // Radius needs a precise fix: "approximate only" counts as refused.
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] != true) permissionDenied = true
    }
    val fineGranted = permissions.permissions.any { it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        locationOff = result.resultCode != Activity.RESULT_OK
    }

    LaunchedEffect(Unit) { if (!fineGranted) permissions.launchMultiplePermissionRequest() }

    // Settings check (turn-on prompt) — once per grant, and again on "Aktifkan lokasi". A refusal
    // does not re-prompt in a loop (legacy did); it leaves the button instead.
    LaunchedEffect(fineGranted, settingsCheckTick) {
        if (!fineGranted) return@LaunchedEffect
        val sender = viewModel.settingsResolution()
        if (sender != null) resolutionLauncher.launch(IntentSenderRequest.Builder(sender).build())
        else locationOff = !viewModel.isLocationEnabled()
    }

    LaunchedEffect(fineGranted, locationOff) {
        if (fineGranted && !locationOff) {
            viewModel.seedLocation()
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationUpdates().collect(viewModel::onLocation)
            }
        }
    }

    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(1_000)
            value = LocalDateTime.now()
        }
    }
    val isCheckIn = check?.isCheckIn == true
    val verb = if (isCheckIn) "Masuk" else "Pulang"

    DetailScaffold(
        title = "Masuk Sekolah",
        onBack = onBack,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            val ready = status as? MasukStatus.Ready
            if (ready != null || status is MasukStatus.CheckFailed) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                ) {
                    if (ready != null) {
                        AppButton(
                            text = ready.buttonLabel,
                            onClick = { confirmVisible = true },
                            enabled = !submitting,
                            loading = submitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AppButton(text = "Coba lagi", onClick = viewModel::load, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (errorMessage.isNotBlank()) {
                BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
            }
            UserRow(name = viewModel.userName, avatar = viewModel.userAvatar, lastLabel = lastLabel)

            AttendanceMap(
                schoolLat = viewModel.schoolLat,
                schoolLng = viewModel.schoolLng,
                radiusMeters = viewModel.radiusMeters,
                showRadius = restricted,
                userLat = location?.latitude,
                userLng = location?.longitude,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(DATE_FORMAT.format(now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    CLOCK_FORMAT.format(now),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            StatusLabel(status = status, locationOff = locationOff, onEnableLocation = {
                if (!viewModel.isLocationEnabled()) {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } else {
                    settingsCheckTick++
                    locationOff = false
                }
            })

            check?.schedule?.let { schedule ->
                if (schedule.start_at.isNotBlank() || schedule.end_at.isNotBlank()) {
                    val source = when (schedule.source.lowercase(Locale.ROOT)) {
                        "settings" -> "Pengaturan jam"
                        "calendar" -> "Kalender"
                        else -> schedule.source
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text("Jadwal absen dari $source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Mulai ${schedule.start_at}, Pulang ${schedule.end_at}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!restricted) {
                Text("Absensi bisa dilakukan dimanapun", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (permissionDenied) {
        AppDialog(
            onDismiss = onBack,
            title = "Perhatian",
            body = "Absensi tidak dapat dilakukan tanpa informasi lokasi Anda",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }
    if (notConfigured) {
        AppDialog(
            onDismiss = onBack,
            title = "Peringatan",
            body = "Sekolah anda belum melakukan konfigurasi titik koordinat absensi, silahkan hubungi admin sekolah anda",
            primaryButtonText = "OKE",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }
    if (status is MasukStatus.ScheduleNotSet && !scheduleDialogDismissed) {
        AppDialog(
            onDismiss = { scheduleDialogDismissed = true },
            title = "Pemberitahuan",
            body = "Presensi, Pengaturan Jam sekolah pada kelas anda belum diatur, Silahkan hubungi admin sekolah untuk informasi lebih lanjut !!!",
            primaryButtonText = "OKe",
            onPrimaryClick = { scheduleDialogDismissed = true },
        )
    }
    if (confirmVisible) {
        AppDialog(
            onDismiss = { confirmVisible = false },
            title = "Lakukan Absensi",
            body = "Waktu tercatat saat Anda melakukan absensi, Anda yakin $verb absensi sekarang",
            primaryButtonText = "Ya, Absen Sekarang",
            onPrimaryClick = {
                confirmVisible = false
                viewModel.submit()
            },
            secondaryButtonText = "Nanti Saja",
            onSecondaryClick = { confirmVisible = false },
        )
    }
    success?.let { done ->
        AppDialog(
            onDismiss = {},
            title = "Absensi Waktu Berhasil",
            body = done.message,
            primaryButtonText = "Ok",
            onPrimaryClick = onDone,
            dismissible = false,
        )
    }
}

@Composable
private fun UserRow(name: String, avatar: String, lastLabel: String) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = scheme.onSurfaceVariant)
            if (avatar.isNotBlank()) {
                AsyncImage(model = resolveAssetUrl(avatar), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            if (lastLabel.isNotBlank()) Text(lastLabel, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun AttendanceMap(
    schoolLat: Double,
    schoolLng: Double,
    radiusMeters: Float,
    showRadius: Boolean,
    userLat: Double?,
    userLng: Double?,
) {
    val scheme = MaterialTheme.colorScheme
    val school = LatLng(schoolLat, schoolLng)
    val hasSchool = schoolLat != 0.0 || schoolLng != 0.0
    val camera = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val user = if (userLat != null && userLng != null) LatLng(userLat, userLng) else null
    val userMarker = rememberMarkerState()
    val schoolMarker = rememberMarkerState()

    LaunchedEffect(user, hasSchool) {
        val target = user ?: if (hasSchool) school else null
        if (user != null) userMarker.position = user
        if (hasSchool) schoolMarker.position = school
        if (target != null) camera.animate(CameraUpdateFactory.newLatLngZoom(target, 16f))
    }

    Box(modifier = Modifier.fillMaxWidth().height(240.dp).clip(DiskolaExtraShapes.card).border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)) {
        GoogleMap(
            cameraPositionState = camera,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (hasSchool) {
                Marker(state = schoolMarker, title = "Sekolah")
                if (showRadius && radiusMeters > 0f) {
                    Circle(center = school, radius = radiusMeters.toDouble(), fillColor = Color(0x40FF0000), strokeColor = scheme.primary, strokeWidth = 4f)
                }
            }
            if (user != null) Marker(state = userMarker, title = "Lokasi Anda")
        }
        IconButton(
            onClick = { user?.let { scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(it, 16f)) } } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.sm).background(scheme.surfaceContainerLowest, CircleShape),
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = "Lokasi saya", tint = scheme.primary)
        }
    }
}

@Composable
private fun StatusLabel(status: MasukStatus, locationOff: Boolean, onEnableLocation: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (text, isError) = when (status) {
        MasukStatus.Loading -> "Memuat data presensi…" to false
        MasukStatus.CheckFailed -> "Gagal memuat data presensi" to true
        is MasukStatus.ServerMessage -> status.text to false
        MasukStatus.ScheduleNotSet -> "Jadwal presensi belum diatur" to true
        MasukStatus.WaitingLocation -> (if (locationOff) "Lokasi perangkat nonaktif" else "Mengambil lokasi…") to locationOff
        MasukStatus.Mock -> MockLocationGuard.MESSAGE to true
        MasukStatus.OutsideRadius -> "Anda berada di luar wilayah absensi" to true
        is MasukStatus.Ready -> "" to false
    }
    if (text.isNotBlank()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isError) scheme.error else scheme.onSurface,
        )
    }
    if (locationOff && status is MasukStatus.WaitingLocation) {
        AppButton(text = "Aktifkan lokasi", onClick = onEnableLocation, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
    }
}
