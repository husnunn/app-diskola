package id.diskola.app.ui.screens.agenda

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import id.diskola.app.dataclass.ResponData.AgendaCheckMode
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AgendaCheckViewModel
import id.diskola.app.viewmodel.AgendaLocationState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val NOW_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy · HH.mm", Locale("id"))

/** "Lapor Masuk/Pulang Agenda". Submit stays disabled until a real GPS fix exists. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AgendaCheckScreen(
    date: String,
    agendaId: Int,
    mode: AgendaCheckMode,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenPresensi: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgendaCheckViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val item by viewModel.item.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val gateBlocked by viewModel.gateBlocked.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val isIn = mode == AgendaCheckMode.CHECK_IN
    val verb = if (isIn) "masuk" else "pulang"

    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ) { results ->
        val granted = results.values.any { it }
        if (!granted) Toast.makeText(context, "Aktifkan lokasi untuk lapor $verb", Toast.LENGTH_SHORT).show()
        viewModel.onPermissionResult(granted)
    }
    val anyGranted = permissions.permissions.any { it.status.isGranted }

    LaunchedEffect(Unit) { viewModel.start(date, agendaId, mode) }
    LaunchedEffect(Unit) {
        if (anyGranted) viewModel.fetchLocation() else {
            permissionAsked = true
            permissions.launchMultiplePermissionRequest()
        }
    }
    LaunchedEffect(done) {
        if (done) {
            Toast.makeText(context, "Lapor $verb agenda berhasil", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    val nowLabel by produceState(initialValue = NOW_FORMAT.format(LocalDateTime.now())) {
        while (true) {
            delay(15_000)
            value = NOW_FORMAT.format(LocalDateTime.now())
        }
    }

    DetailScaffold(
        title = if (isIn) "Lapor Masuk Agenda" else "Lapor Pulang Agenda",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(
                    text = if (isIn) "Lapor masuk" else "Lapor pulang",
                    onClick = viewModel::submit,
                    enabled = location is AgendaLocationState.Ready && item != null,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )
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

            item?.let { agenda ->
                InfoCard(title = "Ringkasan Agenda") {
                    Text(agenda.kind, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(agenda.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSurface)
                    Text(agenda.windowLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AgendaPolicySection(policy = agenda.policy)
            }

            InfoCard(title = if (isIn) "Waktu lapor masuk" else "Waktu lapor pulang") {
                Text(nowLabel, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }

            SectionLabel("Lokasi Saat Ini")
            LocationCard(
                state = location,
                onRetry = {
                    when {
                        anyGranted -> viewModel.fetchLocation()
                        // Asked before and the system won't show the dialog again → only Settings can help.
                        permissionAsked && !permissions.shouldShowRationale -> context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                        else -> {
                            permissionAsked = true
                            permissions.launchMultiplePermissionRequest()
                        }
                    }
                },
            )
            Text(
                "Waktu dan lokasi tercatat otomatis saat lapor $verb.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (gateBlocked) {
        AppDialog(
            onDismiss = onBack,
            title = "Pemberitahuan",
            body = "Silakan presensi masuk terlebih dahulu sebelum check-in agenda",
            primaryButtonText = "Presensi",
            onPrimaryClick = onOpenPresensi,
            secondaryButtonText = "Tutup",
            onSecondaryClick = onBack,
            dismissible = false,
        )
    }
    if (notFound) {
        AppDialog(
            onDismiss = onBack,
            title = "Agenda Tidak Tersedia",
            body = "Data agenda ini tidak ditemukan. Buka kembali daftar agenda lalu coba lagi.",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun LocationCard(state: AgendaLocationState, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        when (state) {
            AgendaLocationState.Idle, AgendaLocationState.Fetching -> {
                Text("Mengambil lokasi…", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            AgendaLocationState.Unavailable -> {
                Text("Lokasi belum tersedia", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                AppButton(text = "Coba lagi", onClick = onRetry, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            }
            is AgendaLocationState.Ready -> {
                val latLng = LatLng(state.latitude, state.longitude)
                val camera = rememberCameraPositionState()
                val marker = rememberMarkerState(position = latLng)
                LaunchedEffect(latLng) {
                    marker.position = latLng
                    camera.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                }
                GoogleMap(
                    cameraPositionState = camera,
                    uiSettings = MapUiSettings(
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = true,
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(DiskolaExtraShapes.card),
                ) {
                    Marker(state = marker, title = "Lokasi Anda")
                }
                Text(
                    when {
                        state.searchingAddress -> "Mencari alamat…"
                        state.address.isNullOrBlank() -> "Alamat tidak ditemukan"
                        else -> state.address
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
                Text(
                    String.format(Locale.US, "Koordinat: %.6f, %.6f", state.latitude, state.longitude),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
