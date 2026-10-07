package id.diskola.app.ui.screens.tugas

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ErrorText
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.components.LinkPreviewCard
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.components.SimpleDropdown
import id.diskola.app.ui.navigation.Route
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.UploadTugasViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private val END_AT_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale("id"))

/** `CreateHomeworkPage` (doc §6.4) — 3 modes (create/edit/detail) via `route.editable`. Kelas→Hari→
 * Mapel cascade; edit mode's deadline always defaults to "now" rather than attempting to parse the
 * server's display-label string (decision, see `UploadTugasViewModel`'s doc comment). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTugasScreen(
    route: Route.UploadTugas,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UploadTugasViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var selectedScheduleId by remember { mutableStateOf<Int?>(null) }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    var endAt by remember { mutableStateOf(LocalDateTime.now()) }
    var attachSoal by remember { mutableStateOf(false) }
    var requireRead by remember { mutableStateOf(false) }
    var requireUpload by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var touchedClass by remember { mutableStateOf(false) }
    var touchedDay by remember { mutableStateOf(false) }
    var touchedSchedule by remember { mutableStateOf(false) }

    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val uploadSuccess by viewModel.uploadSuccess.collectAsStateWithLifecycle()
    val linkPreview by viewModel.linkPreview.collectAsStateWithLifecycle()
    val linkPreviewLoading by viewModel.linkPreviewLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadReferenceData()
        if (route.isEdit && route.tugasId > 0) viewModel.loadExisting(route.tugasId)
    }
    LaunchedEffect(existing) {
        val item = existing
        if (item != null && !prefilled) {
            prefilled = true
            title = item.title
            description = item.description
            link = item.link
            selectedClassId = item.class_id.takeIf { it > 0 }
            selectedSubjectId = item.subject_id.takeIf { it > 0 }
            selectedScheduleId = item.schedule_id.takeIf { it > 0 }
            requireRead = item.downloded > 0
            requireUpload = item.uploaded > 0
            attachSoal = item.file_name.isNotBlank() || item.link.isNotBlank()
            selectedFileName = item.file_name.takeIf { it.isNotBlank() }
            // Decision: deadline always defaults to "now" in edit mode — the server's `end_at_label`
            // is a display string, not guaranteed parseable back into a real date.
        }
    }
    LaunchedEffect(uploadSuccess) { if (uploadSuccess) onDone() }
    LaunchedEffect(link) { viewModel.onLinkChanged(link) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val (name, _) = queryTugasFileInfo(context, uri)
        selectedFileUri = uri
        selectedFileName = name ?: "soal"
    }

    val selectedClass = classes.find { it.id == selectedClassId }
    val hasAttachment = selectedFileUri != null || selectedFileName != null || link.isNotBlank()
    val canSubmit = title.isNotBlank() &&
        (selectedSubjectId ?: 0) > 0 &&
        (selectedClassId ?: 0) > 0 &&
        !selectedDay.isNullOrBlank() &&
        (selectedScheduleId ?: 0) > 0 &&
        (!attachSoal || hasAttachment)

    DetailScaffold(
        title = if (!route.editable) "Detail Tugas" else if (route.isEdit) "Edit Tugas" else "Buat Tugas",
        onBack = onDone,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            if (route.editable) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md)) {
                    AppButton(
                        text = "Posting Tugas",
                        onClick = {
                            val parts = selectedFileUri?.let { uri -> buildTugasFilePart(context, uri, selectedFileName ?: "soal") }
                            viewModel.submit(
                                isEdit = route.isEdit,
                                tugasId = route.tugasId,
                                title = title,
                                description = description,
                                classId = selectedClassId ?: 0,
                                subjectId = selectedSubjectId ?: 0,
                                scheduleId = selectedScheduleId ?: 0,
                                grade = selectedClass?.grade ?: 0,
                                endAt = endAt,
                                requireRead = requireRead,
                                requireUpload = requireUpload,
                                link = link,
                                filePart = parts,
                            )
                        },
                        enabled = canSubmit,
                        loading = loading,
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
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })

            FormCard {
                FieldLabel("Kelas", required = true)
                SimpleDropdown(
                    options = classes.map { it.id to (if (it.grade > 0) "${it.grade} - ${it.name}" else it.name) },
                    selected = selectedClassId,
                    placeholder = "Pilih Kelas",
                    onSelected = {
                        selectedClassId = it
                        selectedDay = null
                        selectedScheduleId = null
                        selectedSubjectId = null
                        touchedClass = true
                        viewModel.onClassSelected()
                    },
                )
                if (touchedClass && selectedClassId == null) ErrorText("Kelas wajib dipilih")

                FieldLabel("Hari Mata Pelajaran", required = true)
                SimpleDropdown(
                    options = days.mapIndexed { index, d -> index to d.lable.ifBlank { d.key } },
                    selected = days.indexOfFirst { it.key == selectedDay }.takeIf { it >= 0 },
                    placeholder = "Pilih Hari Mata Pelajaran",
                    onSelected = { index ->
                        val day = days.getOrNull(index)?.key.orEmpty()
                        selectedDay = day
                        selectedScheduleId = null
                        selectedSubjectId = null
                        touchedDay = true
                        selectedClassId?.let { viewModel.onDaySelected(it, day) }
                    },
                    modifier = Modifier,
                )
                if (touchedDay && selectedDay.isNullOrBlank()) ErrorText("Hari Mata Pelajaran wajib dipilih")

                FieldLabel("Mata Pelajaran", required = true)
                SimpleDropdown(
                    options = schedules.map { it.id to "[${it.time_plot.start_at} - ${it.time_plot.end_at}] ${it.subject.name}" },
                    selected = selectedScheduleId,
                    placeholder = "Pilih Mata Pelajaran",
                    onSelected = { id ->
                        selectedScheduleId = id
                        selectedSubjectId = schedules.find { it.id == id }?.subject?.id
                        touchedSchedule = true
                    },
                )
                if (touchedSchedule && (selectedScheduleId ?: 0) <= 0) ErrorText("Mata Pelajaran wajib dipilih")
            }

            FormCard {
                FieldLabel("Waktu berakhir", required = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
                        .clickable(enabled = route.editable) { showDatePicker = true }
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(endAt.format(END_AT_DISPLAY_FORMATTER), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = Spacing.md))
                }
            }

            FormCard {
                FieldLabel("Judul Tugas", required = true, counter = "${title.length}/500")
                AppTextField(
                    value = title,
                    onValueChange = { if (it.length <= 500) title = it },
                    placeholder = "Ketik Judul tugas",
                    enabled = route.editable,
                )

                FieldLabel("Deskripsi", required = false)
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Ketik deskripsi tugas",
                    enabled = route.editable,
                )
            }

            FormCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = route.editable) { attachSoal = !attachSoal },
                ) {
                    Checkbox(checked = attachSoal, onCheckedChange = { attachSoal = it }, enabled = route.editable)
                    Text("Upload Soal", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                if (attachSoal) {
                    Text("File dalam format pdf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TugasFileDropZone(
                        fileName = selectedFileName,
                        onPick = { if (route.editable) filePickerLauncher.launch(arrayOf("application/pdf")) },
                        onClear = { selectedFileUri = null; selectedFileName = null },
                    )
                    FieldLabel("Url Link", required = false)
                    AppTextField(value = link, onValueChange = { link = it }, placeholder = "Ketik url", enabled = route.editable)
                    if (link.isNotBlank()) {
                        LinkPreviewCard(url = link, loading = linkPreviewLoading, data = linkPreview, onClick = {})
                    }
                    if (selectedFileName != null || link.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = route.editable) { requireRead = !requireRead },
                        ) {
                            Checkbox(checked = requireRead, onCheckedChange = { requireRead = it }, enabled = route.editable)
                            Text("Siswa wajib membaca materi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = route.editable) { requireUpload = !requireUpload },
                ) {
                    Checkbox(checked = requireUpload, onCheckedChange = { requireUpload = it }, enabled = route.editable)
                    Text("Siswa wajib mengumpulkan jawaban", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (date.isAfter(LocalDate.now()) || date.isEqual(LocalDate.now())) {
                            endAt = LocalDateTime.of(date, endAt.toLocalTime())
                        }
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Batas pengumpulan") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) {
            DatePicker(state = state, title = { Text("Batas pengumpulan", modifier = Modifier.padding(Spacing.lg)) })
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = endAt.hour, initialMinute = endAt.minute, is24Hour = true)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card).padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Batas pengumpulan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.lg))
                TimePicker(state = timeState)
                Row(modifier = Modifier.padding(top = Spacing.lg)) {
                    TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
                    TextButton(onClick = {
                        endAt = LocalDateTime.of(endAt.toLocalDate(), LocalTime.of(timeState.hour, timeState.minute))
                        showTimePicker = false
                    }) { Text("Simpan") }
                }
            }
        }
    }
}

@Composable
private fun TugasFileDropZone(fileName: String?, onPick: () -> Unit, onClear: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.5.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onPick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = scheme.primary)
        Text(
            fileName ?: "Lampirkan File",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = Spacing.md),
        )
        if (fileName != null) {
            IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, contentDescription = "Hapus berkas", tint = scheme.error) }
        }
    }
}

private fun queryTugasFileInfo(context: android.content.Context, uri: Uri): Pair<String?, Long?> {
    var name: String? = null
    var size: Long? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) name = cursor.getString(nameIndex)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }
    return name to size
}

private fun buildTugasFilePart(context: android.content.Context, uri: Uri, fileName: String): MultipartBody.Part {
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    val mimeType = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
    return MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
}
