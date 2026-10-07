package id.diskola.app.ui.screens.jurnal

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppFilterChip
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.components.PagedPickerSheet
import id.diskola.app.ui.components.PickerOption
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.JurnalFormResult
import id.diskola.app.viewmodel.JurnalFormViewModel
import id.diskola.app.viewmodel.PagedPicker

private enum class Picker { CLASS, SUBJECT, TEACHER }

/**
 * Journal form (doc `07` §8) — teacher: consecutive hour chips + kelas + mapel + tujuan; student: the hour is fixed
 * ([plotLabel]) + mapel + guru + status. Both take an optional scene photo that the scope can make mandatory.
 * [onFinished] gets the attendance id to open when the server said the journal already exists, else null.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun JurnalFormScreen(
    plotId: Int,
    plotLabel: String,
    capturedPhotoPath: String?,
    onPhotoHandled: () -> Unit,
    onBack: () -> Unit,
    onTakePhoto: (address: String, lat: Double?, lng: Double?) -> Unit,
    onFinished: (openAttendanceId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JurnalFormViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isTeacher = viewModel.isTeacher

    val assigned by viewModel.assigned.collectAsStateWithLifecycle()
    val captureRequired by viewModel.captureRequired.collectAsStateWithLifecycle()
    val plots by viewModel.plots.collectAsStateWithLifecycle()
    val selectedPlots by viewModel.selectedPlots.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubject.collectAsStateWithLifecycle()
    val selectedTeacher by viewModel.selectedTeacher.collectAsStateWithLifecycle()
    val objective by viewModel.objective.collectAsStateWithLifecycle()
    val studentStatus by viewModel.studentStatus.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val preparing by viewModel.preparing.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var picker by remember { mutableStateOf<Picker?>(null) }
    var showFullPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(plotId) { viewModel.start(plotId) }
    LaunchedEffect(capturedPhotoPath) {
        if (capturedPhotoPath != null) {
            viewModel.onPhotoCaptured(capturedPhotoPath)
            onPhotoHandled()
        }
    }

    // Camera is required to take the photo; location only improves the watermark, so refusing it is fine.
    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ) {
        val cameraGranted = it[Manifest.permission.CAMERA] == true
        if (cameraGranted) viewModel.prepareCapture { spot -> onTakePhoto(spot.address.orEmpty(), spot.lat, spot.lng) }
        else Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_LONG).show()
    }
    val cameraGranted = permissions.permissions.any { it.permission == Manifest.permission.CAMERA && it.status.isGranted }
    fun takePhoto() {
        if (cameraGranted) viewModel.prepareCapture { spot -> onTakePhoto(spot.address.orEmpty(), spot.lat, spot.lng) }
        else permissions.launchMultiplePermissionRequest()
    }

    DetailScaffold(
        title = "Jurnal Mengajar",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(
                    text = "Proses",
                    onClick = viewModel::submit,
                    enabled = !submitting,
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
            if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = viewModel::clearError)
            if (isTeacher && assigned) {
                Text(
                    "Hanya menampilkan kelas/mapel/jam dari jadwal Anda",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FormCard {
                FieldLabel("Plot Waktu :", required = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (isTeacher) {
                        plots.forEachIndexed { index, plot ->
                            AppFilterChip(label = plot.label, selected = index in selectedPlots, onClick = { viewModel.onChipTapped(index) })
                        }
                    } else {
                        // students fill one fixed hour: shown highlighted, not tappable
                        AppFilterChip(label = plotLabel.ifBlank { "Jam terpilih" }, selected = true, onClick = {})
                    }
                }
            }

            FormCard {
                if (isTeacher) {
                    FieldLabel("Kelas :", required = true)
                    SelectField(selectedClass?.label.orEmpty(), "Pilih Kelas") {
                        viewModel.classPicker.open()
                        picker = Picker.CLASS
                    }
                }
                FieldLabel("Mata Pelajaran :", required = true)
                SelectField(selectedSubject?.name.orEmpty(), "Pilih Mata Pelajaran") {
                    viewModel.subjectPicker.open()
                    picker = Picker.SUBJECT
                }
                if (isTeacher) {
                    FieldLabel("Tujuan Pembelajaran :", required = true)
                    AppTextField(value = objective, onValueChange = viewModel::onObjectiveChange, placeholder = "Isikan disini ...", minLines = 3)
                } else {
                    FieldLabel("Guru :", required = true)
                    SelectField(selectedTeacher?.name.orEmpty(), "Pilih Guru") {
                        viewModel.teacherPicker.open()
                        picker = Picker.TEACHER
                    }
                    FieldLabel("Status Pembelajaran :", required = true)
                    RadioOptions(options = SESSION_STATUSES, selected = studentStatus, onSelect = viewModel::onStatusSelected)
                }
            }

            FormCard {
                FieldLabel("Foto Suasana KBM", required = captureRequired)
                photo?.let { file ->
                    Box {
                        AsyncImage(
                            model = file,
                            contentDescription = "Foto suasana KBM",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(DiskolaExtraShapes.card)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
                                .clickable { showFullPhoto = true },
                        )
                        IconButton(onClick = viewModel::removePhoto, modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus foto", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text("Ketuk foto untuk melihat penuh", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppButton(
                    text = if (photo == null) "Ambil Foto" else "Ambil Ulang Foto",
                    onClick = ::takePhoto,
                    variant = ButtonVariant.Outlined,
                    enabled = !preparing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Ambil foto suasana kelas. Maks 2 MB.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    when (picker) {
        Picker.CLASS -> PickerSheet(
            title = "Pilih Kelas",
            picker = viewModel.classPicker,
            toOption = { PickerOption(it.id, it.label) },
            selectedId = selectedClass?.id,
            onSelect = { viewModel.onClassSelected(it) },
            onDismiss = { picker = null },
        )
        Picker.SUBJECT -> PickerSheet(
            title = "Pilih Mata Pelajaran",
            picker = viewModel.subjectPicker,
            toOption = { PickerOption(it.id, it.name) },
            selectedId = selectedSubject?.id,
            onSelect = { viewModel.onSubjectSelected(it) },
            onDismiss = { picker = null },
        )
        Picker.TEACHER -> PickerSheet(
            title = "Pilih Guru",
            picker = viewModel.teacherPicker,
            toOption = { PickerOption(it.id, it.name, it.nip) },
            selectedId = selectedTeacher?.id,
            onSelect = { viewModel.onTeacherSelected(it) },
            onDismiss = { picker = null },
        )
        null -> Unit
    }

    if (showFullPhoto) {
        photo?.let { file ->
            Dialog(onDismissRequest = { showFullPhoto = false }) {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog).padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text("Foto Suasana KBM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    AsyncImage(model = file, contentDescription = "Foto suasana KBM", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                    AppButton(text = "Tutup", onClick = { showFullPhoto = false }, variant = ButtonVariant.Text, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (submitting) AppLoadingDialog(message = "mengunggah data")
    if (preparing) AppLoadingDialog(message = "menyiapkan kamera")

    when (val current = result) {
        is JurnalFormResult.Created -> AppDialog(
            onDismiss = {},
            title = "Pemberitahuan",
            body = current.message,
            primaryButtonText = "Baik",
            onPrimaryClick = {
                viewModel.dismissResult()
                onFinished(null)
            },
            dismissible = false,
        )
        is JurnalFormResult.Exists -> AppDialog(
            onDismiss = {},
            title = "Jurnal sudah ada",
            body = "Jurnal untuk jadwal ini sudah dibuat. Membuka detail jurnal.",
            primaryButtonText = "Buka",
            onPrimaryClick = {
                viewModel.dismissResult()
                onFinished(current.attendanceIds.firstOrNull())
            },
            dismissible = false,
        )
        null -> Unit
    }
}

@Composable
private fun SelectField(value: String, placeholder: String, onClick: () -> Unit) {
    AppTextField(
        value = value,
        onValueChange = {},
        placeholder = placeholder,
        readOnly = true,
        onClick = onClick,
        trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

@Composable
private fun <T> PickerSheet(
    title: String,
    picker: PagedPicker<T>,
    toOption: (T) -> PickerOption,
    selectedId: Int?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val items by picker.items.collectAsStateWithLifecycle()
    val query by picker.query.collectAsStateWithLifecycle()
    val loading by picker.loading.collectAsStateWithLifecycle()
    val options = items.map(toOption)
    PagedPickerSheet(
        title = title,
        query = query,
        onQueryChange = picker::onQuery,
        options = options,
        selectedId = selectedId,
        loading = loading,
        onSelect = { option ->
            items.getOrNull(options.indexOfFirst { it.id == option.id })?.let(onSelect)
            onDismiss()
        },
        onLoadMore = picker::loadMore,
        onDismiss = onDismiss,
    )
}
