package id.diskola.app.ui.screens.presensi

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.utils.AppDialogTone
import id.diskola.app.utils.PresensiRules.MasukStatus
import id.diskola.app.viewmodel.PresensiMasukViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Masuk Sekolah" (doc `07` §5): live map with the school radius, the server-driven action button, and a
 * confirm → submit → success flow. Needs precise location only while this screen is open; there is
 * no background location and no geofence. This composable owns the side effects (permission, location, dialogs);
 * what is drawn is [MasukContent].
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val check by viewModel.check.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val restricted by viewModel.restricted.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val distance by viewModel.distance.collectAsStateWithLifecycle()
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

    // A State, not a value: only the hero reads it, so a new second does not recompose the rest of the screen.
    val clock = produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(1_000)
            value = LocalDateTime.now()
        }
    }
    val verb = if (check?.isCheckIn == true) "Masuk" else "Pulang"

    MasukContent(
        state = MasukViewState(
            userName = viewModel.userName,
            userAvatar = viewModel.userAvatar,
            lastLabel = lastLabel,
            check = check,
            status = status,
            restricted = restricted,
            radius = viewModel.radiusMeters,
            distance = distance,
            locationOff = locationOff,
            submitting = submitting,
            errorMessage = errorMessage,
        ),
        clock = clock,
        onBack = onBack,
        onSubmit = { confirmVisible = true },
        onRetry = viewModel::load,
        onEnableLocation = {
            if (!viewModel.isLocationEnabled()) {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                settingsCheckTick++
                locationOff = false
            }
        },
        onDismissError = viewModel::clearError,
        modifier = modifier,
    ) {
        AttendanceMap(
            schoolLat = viewModel.schoolLat,
            schoolLng = viewModel.schoolLng,
            radiusMeters = viewModel.radiusMeters,
            showRadius = restricted,
            userLat = location?.latitude,
            userLng = location?.longitude,
            modifier = Modifier.mapCardShadow(),
            overlay = { distance?.let { DistancePill(it) } },
        )
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
            tone = AppDialogTone.SUCCESS,
        )
    }
}

/** Shared by Masuk and Dinas Luar; the radius circle and the markers take their colours from the theme. */
@Composable
internal fun AttendanceMap(
    schoolLat: Double,
    schoolLng: Double,
    radiusMeters: Float,
    showRadius: Boolean,
    userLat: Double?,
    userLng: Double?,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val school = LatLng(schoolLat, schoolLng)
    val hasSchool = schoolLat != 0.0 || schoolLng != 0.0
    val camera = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val user = if (userLat != null && userLng != null) LatLng(userLat, userLng) else null
    val userMarker = rememberMarkerState()
    val schoolMarker = rememberMarkerState()

    // Like the legacy `myLocAttached`: the camera goes to the user once, on the first fix, and after that only the marker follows. A new fix
    // every few seconds used to pull the camera back, undoing whatever the user had just panned to ("Lokasi saya" still brings it back).
    var cameraOnUser by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(user, hasSchool) {
        if (user != null) userMarker.position = user
        if (hasSchool) schoolMarker.position = school
        val target = when {
            user != null && !cameraOnUser -> user.also { cameraOnUser = true }
            user == null && hasSchool -> school
            else -> null
        }
        // Launched on the screen's scope, not in this effect: the next fix restarts the effect and would cut the animation off halfway.
        if (target != null) scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(target, 16f)) }
    }

    MapFrame(
        onMyLocation = user?.let { target -> { scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(target, 16f)) } } },
        modifier = modifier,
        overlay = overlay,
    ) {
        GoogleMap(
            cameraPositionState = camera,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (hasSchool) {
                Marker(state = schoolMarker, title = "Sekolah")
                if (showRadius && radiusMeters > 0f) {
                    Circle(center = school, radius = radiusMeters.toDouble(), fillColor = scheme.primary.copy(alpha = 0.12f), strokeColor = scheme.primary, strokeWidth = 4f)
                }
            }
            // Blue, so the user is not mistaken for the (red) school pin.
            if (user != null) Marker(state = userMarker, title = "Lokasi Anda", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        }
    }
}
