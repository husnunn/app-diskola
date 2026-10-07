package id.diskola.app.ui.screens.jurnal

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.screens.presensi.AttendanceMap
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.location.MockLocationGuard
import id.diskola.app.viewmodel.VerifikasiGate
import id.diskola.app.viewmodel.VerifikasiJurnalViewModel

/**
 * "Masuk Kelas" / Verifikasi Jurnal (doc `07` §7.3): the student confirms they are in class. When the school
 * restricts the radius the confirm button exists only inside the circle; then a session status is chosen and sent.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VerifikasiJurnalScreen(
    attendanceId: Int,
    title: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifikasiJurnalViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val restricted by viewModel.restricted.collectAsStateWithLifecycle()
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var permissionDenied by remember { mutableStateOf(false) }
    var locationOff by remember { mutableStateOf(false) }
    var settingsCheckTick by remember { mutableIntStateOf(0) }
    var pickStatus by remember { mutableStateOf(false) }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    var chooseError by remember { mutableStateOf(false) }

    val needsLocation = restricted == true
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

    LaunchedEffect(needsLocation) { if (needsLocation && !fineGranted) permissions.launchMultiplePermissionRequest() }

    LaunchedEffect(needsLocation, fineGranted, settingsCheckTick) {
        if (!needsLocation || !fineGranted) return@LaunchedEffect
        val sender = viewModel.settingsResolution()
        if (sender != null) resolutionLauncher.launch(IntentSenderRequest.Builder(sender).build())
        else locationOff = !viewModel.isLocationEnabled()
    }

    LaunchedEffect(needsLocation, fineGranted, locationOff) {
        if (needsLocation && fineGranted && !locationOff) {
            viewModel.seedLocation()
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationUpdates().collect(viewModel::onLocation)
            }
        }
    }

    DetailScaffold(
        title = "Masuk Kelas",
        onBack = onBack,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            if (gate == VerifikasiGate.READY) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                ) {
                    AppButton(text = "Konfirmasi", onClick = { pickStatus = true }, enabled = !loading, modifier = Modifier.fillMaxWidth())
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
            if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = viewModel::clearError)

            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSurface)
            }

            if (needsLocation) {
                AttendanceMap(
                    schoolLat = viewModel.schoolLat,
                    schoolLng = viewModel.schoolLng,
                    radiusMeters = viewModel.radiusMeters,
                    showRadius = true,
                    userLat = location?.latitude,
                    userLng = location?.longitude,
                )
            }

            val (text, isError) = when (gate) {
                VerifikasiGate.LOADING -> "Memuat pengaturan presensi…" to false
                VerifikasiGate.WAITING_LOCATION -> (if (locationOff) "Lokasi perangkat nonaktif" else "Mengambil lokasi…") to locationOff
                VerifikasiGate.MOCK -> MockLocationGuard.MESSAGE to true
                VerifikasiGate.OUTSIDE_RADIUS -> "Anda berada di luar wilayah absensi" to true
                VerifikasiGate.NOT_CONFIGURED -> "Titik koordinat sekolah belum dikonfigurasi" to true
                VerifikasiGate.READY -> (if (needsLocation) "Anda berada di dalam wilayah absensi" else "Konfirmasi kehadiran Anda di kelas ini") to false
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (locationOff && gate == VerifikasiGate.WAITING_LOCATION) {
                AppButton(
                    text = "Aktifkan lokasi",
                    onClick = {
                        if (!viewModel.isLocationEnabled()) {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } else {
                            settingsCheckTick++
                            locationOff = false
                        }
                    },
                    variant = ButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (permissionDenied && needsLocation) {
        AppDialog(
            onDismiss = onBack,
            title = "Perhatian",
            body = "Presensi kelas tidak dapat dilakukan tanpa informasi lokasi Anda",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }

    if (pickStatus) {
        AppDialog(
            onDismiss = { pickStatus = false },
            title = "Pilih status pembelajaran",
            body = "",
            primaryButtonText = "PROSES",
            onPrimaryClick = {
                val status = chosen
                if (status == null) {
                    chooseError = true
                } else {
                    pickStatus = false
                    viewModel.submit(attendanceId, status)
                }
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { pickStatus = false },
        ) {
            Column {
                RadioOptions(options = SESSION_STATUSES, selected = chosen, onSelect = {
                    chosen = it
                    chooseError = false
                })
                if (chooseError) Text("Pilih status pembelajaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (loading) AppLoadingDialog(message = "memulai kelas")

    if (done) {
        AppDialog(
            onDismiss = {},
            title = "Presensi Berhasil",
            body = "Kehadiran Anda di kelas ini telah tercatat.",
            primaryButtonText = "Ok",
            onPrimaryClick = onDone,
            dismissible = false,
        )
    }
}
