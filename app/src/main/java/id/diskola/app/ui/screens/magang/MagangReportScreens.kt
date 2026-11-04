package id.diskola.app.ui.screens.magang

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AppDialogTone
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.PhotoCapture
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.MagangReportViewModel
import id.diskola.app.viewmodel.MagangViewModel

/**
 * "Laporan Harian" (doc `07` §11.4): the picture and the notes that also check the student out. The button waits for both, then a confirm
 * dialog ("Presensi Keluar") precedes the send. [shared] is the shell's ViewModel, read for the company shown above the form.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MagangReportScreen(
    scheduleId: Long,
    shared: MagangViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MagangReportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val schedules by shared.schedules.collectAsStateWithLifecycle()
    val item = schedules.firstOrNull { it.id == scheduleId }

    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val processing by viewModel.processing.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showSource by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) viewModel.onPhotoPicked(uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> viewModel.onPhotoPicked(uri) }

    fun startCamera() {
        runCatching {
            val uri = PhotoCapture.newCameraUri(context)
            cameraUri = uri
            takePicture.launch(uri)
        }.onFailure { viewModel.reportError("Gagal menyiapkan kamera") }
    }

    // The manifest declares CAMERA, so the system camera app refuses to open for us until it has been granted at runtime.
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) startCamera() else viewModel.reportError("Izin kamera dibutuhkan")
    }

    DetailScaffold(
        title = "Laporan Harian",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            // The reason of a failed send sits above the button: at the top of the form it was out of sight whenever the form had scrolled.
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                if (errorMessage.isNotBlank()) {
                    BannerError(
                        message = errorMessage,
                        onDismiss = viewModel::clearError,
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md)) {
                    AppButton(
                        text = "Submit Laporan",
                        onClick = { confirmVisible = true },
                        enabled = MagangRules.canSubmitReport(photo != null, notes) && !submitting && !processing,
                        loading = submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
            item?.let {
                Column {
                    Text(
                        MagangRules.companyName(it),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${MagangRules.dateLabel(it.schedule?.date)} • ${MagangRules.timeRange(it.schedule?.start_time.orEmpty(), it.schedule?.end_time.orEmpty())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FormCard {
                FieldLabel("Lampiran", required = true)
                AttachmentBox(
                    photo = photo,
                    processing = processing,
                    onPick = { showSource = true },
                    onRemove = viewModel::removePhoto,
                    onPreview = { previewVisible = true },
                )
            }
            FormCard {
                FieldLabel("Catatan", required = true)
                AppTextField(
                    value = notes,
                    onValueChange = viewModel::onNotesChange,
                    placeholder = "Tulis catatan kegiatan hari ini (min. ${MagangRules.REPORT_MIN_CHARS} karakter)...",
                    minLines = 6,
                    followCursor = true,
                )
                Row {
                    Spacer(Modifier.weight(1f))
                    Text(
                        MagangRules.reportCounter(notes),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (MagangRules.reportCounterReached(notes)) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showSource) {
        AppDialog(
            onDismiss = { showSource = false },
            title = "Ambil gambar dari",
            body = "",
            primaryButtonText = "Kamera",
            onPrimaryClick = {
                showSource = false
                if (cameraPermission.status.isGranted) startCamera() else cameraPermission.launchPermissionRequest()
            },
            secondaryButtonText = "Galeri",
            onSecondaryClick = {
                showSource = false
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
    if (confirmVisible) {
        AppDialog(
            onDismiss = { confirmVisible = false },
            title = "Presensi Keluar",
            body = "Simpan laporan dan presensi keluar masuk sekarang?",
            primaryButtonText = "Konfirmasi",
            onPrimaryClick = {
                confirmVisible = false
                viewModel.submit(scheduleId)
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { confirmVisible = false },
        )
    }
    if (previewVisible) photo?.let { ImagePreviewDialog(model = it, onDismiss = { previewVisible = false }) }
    if (success) {
        AppDialog(
            onDismiss = {},
            title = "Laporan tersimpan",
            body = "Laporan harian dan presensi keluar Anda telah tersimpan.",
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

/** A report sent earlier, read-only (legacy: the same dialog with the input disabled): company, times, the attachment and the whole text. */
@Composable
fun MagangReportDetailScreen(
    reportId: Long,
    shared: MagangViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val attendance by shared.attendance.collectAsStateWithLifecycle()
    val report = MagangRules.reportsOf(attendance).firstOrNull { it.id == reportId }
    var preview by remember { mutableStateOf<String?>(null) }

    DetailScaffold(title = "Laporan Harian", onBack = onBack, modifier = modifier) { padding ->
        if (report == null) {
            EmptyState(
                title = "Laporan tidak ditemukan",
                description = "Laporan ini tidak ada lagi di daftar. Kembali dan muat ulang.",
                icon = Icons.Rounded.EventBusy,
                actionLabel = "Kembali",
                onAction = onBack,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            val attachment = MagangRules.attachmentOf(report)
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        report.company?.name?.trim().orEmpty().ifEmpty { "Tempat Magang" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LabeledRow("Masuk", MagangRules.dateTimeLabel(report.check_in_at))
                    LabeledRow("Keluar", MagangRules.dateTimeLabel(report.check_out_at))
                }
                if (attachment != null) {
                    FormCard {
                        FieldLabel("Lampiran", required = false)
                        AsyncImage(
                            model = resolveAssetUrl(attachment),
                            contentDescription = "Lampiran laporan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { preview = attachment },
                        )
                    }
                }
                FormCard {
                    FieldLabel("Laporan", required = false)
                    Text(MagangRules.reportText(report), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    preview?.let { ImagePreviewDialog(model = resolveAssetUrl(it), onDismiss = { preview = null }) }
}
