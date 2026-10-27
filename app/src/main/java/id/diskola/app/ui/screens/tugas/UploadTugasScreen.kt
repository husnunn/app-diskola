package id.diskola.app.ui.screens.tugas

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ErrorText
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.components.LinkPreviewCard
import id.diskola.app.ui.components.openLink
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.components.SimpleDropdown
import id.diskola.app.ui.navigation.Route
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AssignmentInput
import id.diskola.app.utils.AssignmentPayload
import id.diskola.app.viewmodel.TugasFormDialog
import id.diskola.app.viewmodel.UploadTugasViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private val END_AT_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale("id"))

/**
 * "Buat/Edit/Detail Tugas" of the teacher (`CreateHomeworkPage`, doc §6.4) — 3 modes via `route.editable`/`route.isEdit`, and two forms by the
 * teacher's data scope (see [UploadTugasViewModel]): Kelas → Hari → Mata Pelajaran, or Kelas + Mata Pelajaran. The deadline has no default (the
 * teacher must choose it), the description is required, and "Posting" does not wait for the link preview. Messages of a 422 appear under their
 * field; the draft stays as it is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTugasScreen(
    route: Route.UploadTugas,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UploadTugasViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueAtMillis by remember { mutableStateOf<Long?>(null) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    var attachSoal by remember { mutableStateOf(false) }
    var requireRead by remember { mutableStateOf(false) }
    var requireUpload by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    var prefilled by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var touchedClass by remember { mutableStateOf(false) }
    var touchedDay by remember { mutableStateOf(false) }
    var touchedSubject by remember { mutableStateOf(false) }

    val restricted by viewModel.restricted.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val initialLoading by viewModel.initialLoading.collectAsStateWithLifecycle()
    val scheduleLoading by viewModel.scheduleLoading.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val prefill by viewModel.prefill.collectAsStateWithLifecycle()
    val fieldErrors by viewModel.fieldErrors.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val linkPreview by viewModel.linkPreview.collectAsStateWithLifecycle()
    val linkPreviewLoading by viewModel.linkPreviewLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val assigned = restricted != false

    LaunchedEffect(Unit) { viewModel.start(if (route.isEdit && route.tugasId > 0) route.tugasId else 0) }
    LaunchedEffect(Unit) { viewModel.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } }
    LaunchedEffect(prefill) {
        val ready = prefill ?: return@LaunchedEffect
        if (prefilled) return@LaunchedEffect
        prefilled = true
        val item = ready.table
        title = item.title
        description = item.description
        link = item.link
        dueAtMillis = ready.dueAtMillis
        requireRead = item.downloded > 0
        requireUpload = item.uploaded > 0
        attachSoal = item.file_name.isNotBlank() || item.link.isNotBlank()
        selectedFileName = item.file_name.takeIf { it.isNotBlank() }
    }
    // Saved, or the task is gone: say so (if there is something to say), then leave; the list refreshes behind us.
    LaunchedEffect(finished) {
        val close = finished ?: return@LaunchedEffect
        close.toast?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        onDone()
    }
    LaunchedEffect(link) { viewModel.onLinkChanged(link) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val (name, size) = queryTugasFileInfo(context, uri)
        if (size != null && size > AssignmentPayload.MAX_FILE_BYTES) {
            Toast.makeText(context, "Ukuran file maksimal 12 MB", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        selectedFileUri = uri
        selectedFileName = name ?: "soal"
        selectedFileSize = size
    }

    val hasAttachment = selectedFileUri != null || selectedFileName != null || link.isNotBlank()
    val input = AssignmentInput(
        restricted = restricted,
        classId = selection.classId,
        subjectId = selection.subjectId,
        day = selection.day,
        scheduleId = selection.scheduleId,
        title = title,
        description = description,
        dueAt = dueAtMillis,
        // File, link and "must read" belong to "Upload Soal": switched off, none of them is sent.
        link = if (attachSoal) link else "",
        needDownload = attachSoal && hasAttachment && requireRead,
        needUpload = requireUpload,
        uploadFile = attachSoal,
        hasFile = attachSoal && (selectedFileUri != null || selectedFileName != null),
        fileSizeBytes = if (attachSoal) selectedFileSize else null,
    )
    val canSubmit = AssignmentPayload.validate(input).isEmpty() && !submitting && !scheduleLoading && !initialLoading

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
                            val fileUri = selectedFileUri
                            val fileName = selectedFileName ?: "soal"
                            viewModel.submit(
                                isEdit = route.isEdit,
                                tugasId = route.tugasId,
                                input = input,
                                buildFile = if (attachSoal && fileUri != null) ({ buildTugasFilePart(context, fileUri, fileName) }) else null,
                            )
                        },
                        enabled = canSubmit,
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
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })

            FormCard {
                FieldLabel("Kelas", required = true)
                SimpleDropdown(
                    options = classes.map { it.id to (if (it.grade > 0) "${it.grade} - ${it.name}" else it.name) },
                    selected = selection.classId.takeIf { it > 0 },
                    placeholder = "Pilih Kelas",
                    enabled = route.editable,
                    onSelected = {
                        touchedClass = true
                        viewModel.selectClass(it)
                    },
                )
                (fieldErrors["school_class_id"] ?: "Kelas wajib dipilih".takeIf { touchedClass && selection.classId <= 0 })?.let { ErrorText(it) }

                if (assigned) {
                    FieldLabel("Hari Mata Pelajaran", required = true)
                    SimpleDropdown(
                        options = days.mapIndexed { index, d -> index to d.lable.ifBlank { d.key } },
                        selected = days.indexOfFirst { it.key == selection.day }.takeIf { it >= 0 },
                        placeholder = "Pilih Hari Mata Pelajaran",
                        enabled = route.editable && selection.classId > 0,
                        onSelected = { index ->
                            touchedDay = true
                            viewModel.selectDay(days.getOrNull(index)?.key.orEmpty())
                        },
                    )
                    (fieldErrors["day"] ?: "Hari Mata Pelajaran wajib dipilih".takeIf { touchedDay && selection.day.isBlank() })?.let { ErrorText(it) }

                    FieldLabel("Mata Pelajaran", required = true)
                    SimpleDropdown(
                        options = schedules.map { it.id to "[${it.time_plot.start_at} - ${it.time_plot.end_at}] ${it.subject.name}" },
                        selected = selection.scheduleId.takeIf { it > 0 },
                        placeholder = "Pilih Mata Pelajaran",
                        enabled = route.editable && selection.classId > 0 && selection.day.isNotBlank(),
                        onSelected = {
                            touchedSubject = true
                            viewModel.selectSchedule(it)
                        },
                    )
                    if (scheduleLoading) {
                        Text("Memuat jadwal…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    (fieldErrors["school_subject_id"] ?: "Mata Pelajaran wajib dipilih".takeIf { touchedSubject && selection.scheduleId <= 0 })?.let { ErrorText(it) }
                } else {
                    FieldLabel("Mata Pelajaran", required = true)
                    SimpleDropdown(
                        options = subjects.map { it.id to it.name },
                        selected = selection.subjectId.takeIf { it > 0 },
                        placeholder = "Pilih Mata Pelajaran",
                        enabled = route.editable,
                        onSelected = {
                            touchedSubject = true
                            viewModel.selectSubject(it)
                        },
                    )
                    (fieldErrors["school_subject_id"] ?: "Mata Pelajaran wajib dipilih".takeIf { touchedSubject && selection.subjectId <= 0 })?.let { ErrorText(it) }
                }
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
                    val chosen = dueAtMillis
                    Text(
                        chosen?.let { END_AT_DISPLAY_FORMATTER.format(Instant.ofEpochMilli(it).atZone(zone)) } ?: "Pilih waktu berakhir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (chosen == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = Spacing.md),
                    )
                }
                fieldErrors["end_at"]?.let { ErrorText(it) }
            }

            FormCard {
                FieldLabel("Judul Tugas", required = true, counter = "${title.length}/500")
                AppTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= 500) title = it
                        viewModel.clearFieldError("title")
                    },
                    placeholder = "Ketik Judul tugas",
                    errorText = fieldErrors["title"],
                    enabled = route.editable,
                )

                FieldLabel("Deskripsi", required = true)
                AppTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        viewModel.clearFieldError("description")
                    },
                    placeholder = "Ketik deskripsi tugas",
                    errorText = fieldErrors["description"],
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
                    Text("File dalam format pdf, maksimal 12 MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TugasFileDropZone(
                        fileName = selectedFileName,
                        onPick = { if (route.editable) filePickerLauncher.launch(arrayOf("application/pdf")) },
                        onClear = {
                            selectedFileUri = null
                            selectedFileName = null
                            selectedFileSize = null
                        },
                    )
                    FieldLabel("Url Link", required = false)
                    AppTextField(
                        value = link,
                        onValueChange = { link = it },
                        placeholder = "Ketik url",
                        errorText = fieldErrors["link"],
                        enabled = route.editable,
                    )
                    // The preview only informs: "Posting" never waits for it.
                    if (link.isNotBlank()) {
                        LinkPreviewCard(url = link, loading = linkPreviewLoading, data = linkPreview, onClick = { openLink(context, link) })
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
        val today = LocalDate.now()
        val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initialDate = dueAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: today
        // The date picker speaks UTC midnight: the chosen day is converted both ways in UTC so it never slips a day near midnight.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc
                override fun isSelectableYear(year: Int): Boolean = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pendingDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                    if (pendingDate != null) showTimePicker = true
                }) { Text("Lanjut") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } },
        ) {
            DatePicker(state = state, title = { Text("Batas pengumpulan", modifier = Modifier.padding(Spacing.lg)) })
        }
    }

    if (showTimePicker) {
        val initialTime = dueAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() } ?: LocalTime.now()
        val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = true)
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
                        val date = pendingDate ?: LocalDate.now()
                        dueAtMillis = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute)).atZone(zone).toInstant().toEpochMilli()
                        viewModel.clearFieldError("end_at")
                        showTimePicker = false
                    }) { Text("Simpan") }
                }
            }
        }
    }

    if (initialLoading) AppLoadingDialog(message = "Mohon tunggu")
    if (submitting) AppLoadingDialog(message = if (route.isEdit) "Sedang menyimpan tugas" else "Sedang membuat tugas")
    when (val shown = dialog) {
        is TugasFormDialog.Blocked -> AppDialog(
            onDismiss = viewModel::dismissDialog,
            title = "Akses diblokir",
            body = shown.message + (shown.traceId?.let { "\n\nID: $it" } ?: ""),
            primaryButtonText = "Mengerti",
            onPrimaryClick = viewModel::dismissDialog,
        )
        is TugasFormDialog.Invalid -> AppDialog(
            onDismiss = viewModel::dismissDialog,
            title = "Periksa kembali isian",
            body = shown.text,
            primaryButtonText = "Mengerti",
            onPrimaryClick = viewModel::dismissDialog,
        )
        is TugasFormDialog.Failed -> AppDialog(
            onDismiss = viewModel::dismissDialog,
            title = "Gagal menyimpan tugas",
            body = shown.text,
            primaryButtonText = "Mengerti",
            onPrimaryClick = viewModel::dismissDialog,
        )
        null -> Unit
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
