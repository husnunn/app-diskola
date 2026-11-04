package id.diskola.app.ui.screens.magang

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.screens.presensi.AttendanceMap
import id.diskola.app.ui.screens.presensi.DistancePill
import id.diskola.app.ui.screens.presensi.mapCardShadow
import id.diskola.app.utils.AppDialogTone
import id.diskola.app.viewmodel.MagangAttendViewModel
import id.diskola.app.viewmodel.MagangViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/**
 * Magang "Presensi Masuk" (doc `07` §11.3). This composable owns the side effects: the precise-location permission, the "turn on location"
 * prompt, the position stream while the screen is open, the confirm dialog and the submit. What is drawn is [MagangAttendContent].
 * [shared] is the shell's [MagangViewModel], which holds the schedule row this screen reads.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MagangAttendScreen(
    scheduleId: Long,
    shared: MagangViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MagangAttendViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val schedules by shared.schedules.collectAsStateWithLifecycle()
    val item = schedules.firstOrNull { it.id == scheduleId }
    LaunchedEffect(item) { viewModel.setSchedule(item) }

    val view by viewModel.view.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var permissionDenied by remember { mutableStateOf(false) }
    var locationOff by remember { mutableStateOf(false) }
    var settingsCheckTick by remember { mutableIntStateOf(0) }
    var confirmVisible by remember { mutableStateOf(false) }

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ) { results ->
        // The radius needs a precise fix: "approximate only" counts as refused.
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] != true) permissionDenied = true
    }
    val fineGranted = permissions.permissions.any { it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        locationOff = result.resultCode != Activity.RESULT_OK
    }

    LaunchedEffect(Unit) { if (!fineGranted) permissions.launchMultiplePermissionRequest() }

    // The turn-on prompt once per grant and again on "Aktifkan lokasi"; a refusal does not re-prompt in a loop (legacy did).
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

    val current = view
    if (current == null) {
        DetailScaffold(title = "Presensi Magang", onBack = onBack, useCloseIcon = true, modifier = modifier) { padding ->
            EmptyState(
                title = "Jadwal tidak ditemukan",
                description = "Jadwal magang ini tidak ada lagi di daftar. Kembali dan muat ulang jadwal Anda.",
                icon = Icons.Rounded.EventBusy,
                actionLabel = "Kembali",
                onAction = onBack,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    } else {
        MagangAttendContent(
            view = current,
            userName = viewModel.userName,
            userAvatar = viewModel.userAvatar,
            clock = clock,
            locationOff = locationOff,
            submitting = submitting,
            errorMessage = errorMessage,
            onBack = onBack,
            onConfirm = { confirmVisible = true },
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
                schoolLat = current.lat,
                schoolLng = current.lng,
                radiusMeters = current.radius,
                showRadius = current.enforced,
                userLat = location?.latitude,
                userLng = location?.longitude,
                placeTitle = "Lokasi Magang",
                modifier = Modifier.mapCardShadow(),
                overlay = { current.distance?.let { DistancePill(it, place = "lokasi magang") } },
            )
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
    if (confirmVisible) {
        AppDialog(
            onDismiss = { confirmVisible = false },
            title = "Lakukan Absensi",
            body = "Waktu tercatat saat Anda melakukan absensi. Anda yakin ingin masuk sekarang?",
            primaryButtonText = "Ya, Absen Sekarang",
            onPrimaryClick = {
                confirmVisible = false
                viewModel.submit()
            },
            secondaryButtonText = "Nanti Saja",
            onSecondaryClick = { confirmVisible = false },
        )
    }
    if (success) {
        AppDialog(
            onDismiss = {},
            title = "Absensi Berhasil!",
            body = "Presensi Anda telah berhasil dikonfirmasi.",
            primaryButtonText = "Ok",
            onPrimaryClick = {
                shared.refresh()
                onDone()
            },
            dismissible = false,
            tone = AppDialogTone.SUCCESS,
        )
    }
}
