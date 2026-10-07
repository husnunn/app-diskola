package id.diskola.app.ui.screens.poin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import id.diskola.app.dataclass.ResponData.PoinFormMode
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
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
import id.diskola.app.utils.PhotoCapture
import id.diskola.app.viewmodel.PoinFormViewModel
import id.diskola.app.viewmodel.PoinGuruViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
private val TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm")

/** Pelanggaran / Prestasi / Pemanggilan form (doc `05` §9.4). Success is only announced after the
 * API call actually succeeded; a failure shows the banner and the form stays editable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoinFormScreen(
    mode: PoinFormMode,
    guruViewModel: PoinGuruViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PoinFormViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val target by guruViewModel.selected.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val time by viewModel.time.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val pickerItems by viewModel.pickerItems.collectAsStateWithLifecycle()
    val pickerQuery by viewModel.pickerQuery.collectAsStateWithLifecycle()
    val pickerLoading by viewModel.pickerLoading.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var showPhotoSource by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(mode) { viewModel.setMode(mode) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) viewModel.onPhotoPicked(uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onPhotoPicked(uri)
    }

    val typeLabel = when (mode) {
        PoinFormMode.VIOLATION -> "Pilih Jenis Pelanggaran"
        PoinFormMode.ACHIEVEMENT -> "Pilih Jenis Prestasi"
        PoinFormMode.HANDLING -> "Pilih Jenis Pemanggilan"
    }
    val title = when (mode) {
        PoinFormMode.VIOLATION -> "Detail Pelanggaran Siswa"
        PoinFormMode.ACHIEVEMENT -> "Detail Prestasi Siswa"
        PoinFormMode.HANDLING -> "Pemanggilan siswa"
    }
    val successMessage = when (mode) {
        PoinFormMode.VIOLATION -> "Poin pelanggaran berhasil ditambahkan"
        PoinFormMode.ACHIEVEMENT -> "Poin prestasi berhasil ditambahkan"
        PoinFormMode.HANDLING -> "Pemanggilan Siswa Berhasil Ditambahkan"
    }

    DetailScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(
                    text = "Kirim",
                    onClick = { target?.let(viewModel::submit) },
                    enabled = target != null && !loading,
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

            FormCard {
                FieldLabel(if (mode == PoinFormMode.HANDLING) "Jenis Pemanggilan" else "Jenis", required = true)
                AppTextField(
                    value = selectedType?.name.orEmpty(),
                    onValueChange = {},
                    placeholder = typeLabel,
                    readOnly = true,
                    onClick = {
                        viewModel.openPicker()
                        showPicker = true
                    },
                    trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                )

                FieldLabel("Keterangan :", required = true)
                AppTextField(value = message, onValueChange = viewModel::onMessageChange, placeholder = "Keterangan")
            }

            if (mode == PoinFormMode.HANDLING) {
                FormCard {
                    FieldLabel("Jadwal Pemanggilan :", required = true)
                    AppTextField(
                        value = date?.format(DATE_LABEL).orEmpty(),
                        onValueChange = {},
                        placeholder = "Tanggal",
                        readOnly = true,
                        onClick = { showDatePicker = true },
                    )
                    AppTextField(
                        value = time?.format(TIME_LABEL).orEmpty(),
                        onValueChange = {},
                        placeholder = "Waktu",
                        readOnly = true,
                        onClick = { showTimePicker = true },
                    )
                    AppButton(
                        text = "pilih jadwal",
                        onClick = { showDatePicker = true },
                        variant = ButtonVariant.Outlined,
                        leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                FormCard {
                    FieldLabel("Lampiran Foto (*opsional) :", required = false)
                    photo?.let { file ->
                        Box {
                            AsyncImage(
                                model = file,
                                contentDescription = "Foto lampiran",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(DiskolaExtraShapes.card)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card),
                            )
                            IconButton(
                                onClick = viewModel::removePhoto,
                                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus foto", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    AppButton(
                        text = "ambil gambar",
                        onClick = { showPhotoSource = true },
                        variant = ButtonVariant.Outlined,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showPicker) {
        PagedPickerSheet(
            title = typeLabel,
            query = pickerQuery,
            onQueryChange = viewModel::onPickerQuery,
            options = pickerItems.map { PickerOption(it.id, it.name, if (it.score.isNotBlank()) "Poin ${it.score}" else "") },
            selectedId = selectedType?.id,
            loading = pickerLoading,
            onSelect = { option ->
                pickerItems.firstOrNull { it.id == option.id }?.let(viewModel::onTypeSelected)
                showPicker = false
            },
            onLoadMore = viewModel::loadMorePicker,
            onDismiss = { showPicker = false },
        )
    }

    if (showPhotoSource) {
        AppDialog(
            onDismiss = { showPhotoSource = false },
            title = "Ambil gambar dari",
            body = "",
            primaryButtonText = "Kamera",
            onPrimaryClick = {
                showPhotoSource = false
                runCatching {
                    val uri = PhotoCapture.newCameraUri(context)
                    cameraUri = uri
                    takePicture.launch(uri)
                }.onFailure {
                    viewModel.reportError("Gagal membuka kamera, ijin untuk menyimpan file tidak diberikan")
                }
            },
            secondaryButtonText = "Galeri",
            onSecondaryClick = {
                showPhotoSource = false
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }

    if (showDatePicker) {
        val todayUtcMillis = remember { LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) {
            DatePicker(state = state, title = { Text("Jadwal Pemanggilan", modifier = Modifier.padding(Spacing.lg)) })
        }
    }

    if (showTimePicker) {
        val now = remember { LocalTime.now() }
        val timeState = rememberTimePickerState(
            initialHour = (time ?: now).hour,
            initialMinute = (time ?: now).minute,
            is24Hour = true,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Jadwal Pemanggilan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.lg))
                TimePicker(state = timeState)
                Row(modifier = Modifier.padding(top = Spacing.lg)) {
                    TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
                    TextButton(onClick = {
                        viewModel.onTimeSelected(LocalTime.of(timeState.hour, timeState.minute))
                        showTimePicker = false
                    }) { Text("Simpan") }
                }
            }
        }
    }

    if (loading) AppLoadingDialog(message = "mengunggah data")

    if (done) {
        AppDialog(
            onDismiss = {},
            title = "Pemberitahuan",
            body = successMessage,
            primaryButtonText = "Baik",
            onPrimaryClick = {
                guruViewModel.refreshSelectedScores()
                onDone()
            },
            dismissible = false,
        )
    }
}
