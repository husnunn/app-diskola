package id.diskola.app.ui.screens.presensi

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.accompanist.permissions.rememberPermissionState
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ErrorText
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.PresensiRules
import id.diskola.app.viewmodel.PresensiOffsiteViewModel
import java.io.File

/**
 * "Presensi Dinas Luar" (doc `07` §6.2, teachers). The selfie comes back from the camera screen as a
 * file path ([capturedPhotoPath]); [onTakePhoto] opens it with the current address and position.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PresensiOffsiteScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onTakePhoto: (address: String, lat: Double, lng: Double) -> Unit,
    capturedPhotoPath: String?,
    onPhotoHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresensiOffsiteViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val statusLabel by viewModel.statusLabel.collectAsStateWithLifecycle()
    val address by viewModel.address.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val errors by viewModel.fieldErrors.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var locationDenied by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ) { results -> if (results[Manifest.permission.ACCESS_FINE_LOCATION] != true) locationDenied = true }
    val fineGranted = locationPermissions.permissions.any { it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) onTakePhoto(viewModel.address.value, viewModel.location.value?.latitude ?: 0.0, viewModel.location.value?.longitude ?: 0.0)
        else Toast.makeText(context, "Izin kamera dibutuhkan untuk foto bukti", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(viewModel.isStudent) {
        if (viewModel.isStudent) {
            Toast.makeText(context, "Presensi dinas luar hanya untuk guru dan staff", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(Unit) { if (!fineGranted) locationPermissions.launchMultiplePermissionRequest() }
    LaunchedEffect(fineGranted) {
        if (fineGranted) {
            viewModel.seedLocation()
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationUpdates().collect(viewModel::onLocation)
            }
        }
    }
    LaunchedEffect(capturedPhotoPath) {
        if (capturedPhotoPath != null) {
            viewModel.onPhotoCaptured(capturedPhotoPath)
            onPhotoHandled()
        }
    }

    val trimmedLength = note.trim().length
    val canSubmit = PresensiRules.canSubmitOffsite(
        allowAttendance = availability.allowAttendance,
        loading = submitting,
        note = note,
        hasPhoto = photo != null,
        requirePhoto = availability.requirePhoto,
    )

    DetailScaffold(
        title = "Presensi Dinas Luar",
        onBack = onBack,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(
                    text = availability.buttonLabel.ifBlank { "Presensi" },
                    onClick = { confirmVisible = true },
                    enabled = canSubmit,
                    loading = submitting,
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
            if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })

            AttendanceMap(
                schoolLat = 0.0,
                schoolLng = 0.0,
                radiusMeters = 0f,
                showRadius = false,
                userLat = location?.latitude,
                userLng = location?.longitude,
            )

            FormCard {
                FieldLabel("Alamat lokasi", required = true)
                AppTextField(
                    value = address,
                    onValueChange = viewModel::onAddressChange,
                    placeholder = "Alamat lokasi",
                    errorText = errors.address,
                )

                FieldLabel("Keterangan dinas luar", required = true)
                AppTextField(
                    value = note,
                    onValueChange = { if (it.length <= PresensiRules.NOTE_MAX) viewModel.onNoteChange(it) },
                    placeholder = "Keterangan dinas luar",
                    errorText = errors.note,
                )
                Text(
                    "$trimmedLength / min. ${PresensiRules.NOTE_MIN} karakter",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (trimmedLength >= PresensiRules.NOTE_MIN) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.error,
                )
            }

            FormCard {
                FieldLabel("Foto bukti", required = availability.requirePhoto)
                photo?.let { file -> PhotoPreview(file) }
                AppButton(
                    text = if (photo == null) "Ambil Foto" else "Ambil Ulang",
                    onClick = {
                        if (!viewModel.canTakePhoto()) return@AppButton
                        if (cameraPermission.status.isGranted) {
                            onTakePhoto(address, location?.latitude ?: 0.0, location?.longitude ?: 0.0)
                        } else {
                            cameraPermission.launchPermissionRequest()
                        }
                    },
                    variant = ButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
                errors.photo?.let { ErrorText(it) }
            }

            if (statusLabel.isNotBlank()) {
                Text(statusLabel, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (locationDenied) {
        AppDialog(
            onDismiss = onBack,
            title = "Perhatian",
            body = "Presensi dinas luar membutuhkan izin lokasi",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }
    if (confirmVisible) {
        AppDialog(
            onDismiss = { confirmVisible = false },
            title = "Lakukan Absensi",
            body = "Waktu tercatat saat Anda melakukan absensi dinas luar. Anda yakin ${availability.buttonLabel.ifBlank { "presensi" }} sekarang?",
            primaryButtonText = "Ya, Absen Sekarang",
            onPrimaryClick = {
                confirmVisible = false
                viewModel.submit()
            },
            secondaryButtonText = "Nanti Saja",
            onSecondaryClick = { confirmVisible = false },
        )
    }
    success?.let { message ->
        AppDialog(
            onDismiss = {},
            title = "Pemberitahuan",
            body = message,
            primaryButtonText = "Ok",
            onPrimaryClick = onDone,
            dismissible = false,
        )
    }
}

@Composable
private fun PhotoPreview(file: File) {
    AsyncImage(
        model = file,
        contentDescription = "Foto bukti",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(DiskolaExtraShapes.card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card),
    )
}
