package id.diskola.app.ui.screens.materi

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.navigation.Route
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.MateriViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

private enum class UploadTarget { JENJANG, JURUSAN, KELAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMateriScreen(
    route: Route.UploadMateri,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var judul by remember { mutableStateOf(route.materiName) }
    var deskripsi by remember { mutableStateOf(route.materiDesc) }
    var urlLink by remember { mutableStateOf(route.link) }
    var selectedSubjectId by remember { mutableStateOf(if (route.subjectId >= 0) route.subjectId else null) }
    var selectedSubjectName by remember { mutableStateOf("") }
    var selectedMajorId by remember { mutableStateOf<Int?>(null) }
    var selectedClassId by remember { mutableStateOf(if (route.classId >= 0) route.classId else null) }
    var target by remember { mutableStateOf(if (route.classId >= 0) UploadTarget.KELAS else UploadTarget.JENJANG) }
    var selectedGrade by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val teacherSubjects by viewModel.teacherSubjects.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val majors by viewModel.majors.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val uploadResult by viewModel.uploadResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.fetchTeacherRequirements() }
    LaunchedEffect(teacherSubjects) {
        if (route.subjectId >= 0) teacherSubjects.find { it.id == route.subjectId }?.let { selectedSubjectName = it.name }
    }
    LaunchedEffect(classes) {
        if (route.classId >= 0) classes.find { it.id == route.classId }?.let { }
    }
    LaunchedEffect(uploadResult) {
        if (uploadResult != null) onDone()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedFileUri = uri
        selectedFileName = queryFileName(context, uri) ?: "upload.pdf"
    }
    val canSubmit = judul.isNotBlank() &&
        selectedSubjectId != null &&
        (selectedFileUri != null || urlLink.isNotBlank())

    DetailScaffold(
        title = if (route.isEdit) "Edit Materi" else "Unggah Materi",
        onBack = onDone,
        useCloseIcon = true,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(
                    text = if (route.isEdit) "Simpan Perubahan" else "Posting materi",
                    onClick = {
                        val data = buildRequestData(judul, deskripsi, urlLink, target, selectedGrade, selectedMajorId, selectedClassId, selectedSubjectId)
                        val filePart = selectedFileUri?.let { buildFilePart(context, it, selectedFileName ?: "upload.pdf") }
                        if (route.isEdit) {
                            viewModel.updateMateri(route.materiId, data, filePart)
                        } else {
                            viewModel.uploadMateri(data, filePart)
                        }
                    },
                    enabled = canSubmit,
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
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FormCard {
                FieldLabel(text = "Judul Materi", required = true, counter = "${judul.length}/500")
                AppTextField(
                    value = judul,
                    onValueChange = { if (it.length <= 500) judul = it },
                    placeholder = "mis. Struktur Data: Array dan ArrayList",
                )

                FieldLabel(text = "Deskripsi", required = false, counter = "${deskripsi.length}/5000")
                AppTextField(
                    value = deskripsi,
                    onValueChange = { if (it.length <= 5000) deskripsi = it },
                    placeholder = "Ringkasan isi materi (opsional)",
                )

                FieldLabel(text = "Mata Pelajaran", required = true)
                SimpleDropdown(
                    label = "",
                    options = teacherSubjects.map { it.name },
                    selected = selectedSubjectName.ifBlank { "Pilih mata pelajaran" },
                    onSelected = { name ->
                        selectedSubjectName = name
                        selectedSubjectId = teacherSubjects.find { it.name == name }?.id
                    },
                )
            }

            FormCard {
                SectionLabel("DITAMPILKAN KE")
                TargetRadio("Jenjang", target == UploadTarget.JENJANG) {
                    target = UploadTarget.JENJANG
                    selectedMajorId = null
                    selectedClassId = null
                }
                TargetRadio("Jurusan", target == UploadTarget.JURUSAN) {
                    target = UploadTarget.JURUSAN
                    selectedGrade = ""
                    selectedClassId = null
                }
                TargetRadio("Kelas", target == UploadTarget.KELAS) {
                    target = UploadTarget.KELAS
                    selectedGrade = ""
                    selectedMajorId = null
                }

                when (target) {
                    UploadTarget.JENJANG -> SimpleDropdown(
                        label = "",
                        options = listOf("10", "11", "12"),
                        selected = selectedGrade.ifBlank { "Pilih jenjang" },
                        onSelected = { selectedGrade = it },
                    )

                    UploadTarget.JURUSAN -> SimpleDropdown(
                        label = "",
                        options = majors.map { it.name },
                        selected = majors.find { it.id == selectedMajorId }?.name ?: "Pilih jurusan",
                        onSelected = { name -> selectedMajorId = majors.find { it.name == name }?.id },
                    )

                    UploadTarget.KELAS -> SimpleDropdown(
                        label = "",
                        options = classes.map { it.name },
                        selected = classes.find { it.id == selectedClassId }?.name ?: "Pilih kelas",
                        onSelected = { name -> selectedClassId = classes.find { it.name == name }?.id },
                    )
                }

                Text(
                    "Hanya satu target yang dikirim — mengganti mode akan mengosongkan pilihan sebelumnya.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FormCard {
                FieldLabel(text = "File Materi", required = false)
                FileDropZone(
                    fileName = selectedFileName,
                    onPick = { filePickerLauncher.launch("*/*") },
                    onClear = {
                        selectedFileUri = null
                        selectedFileName = null
                    },
                )

                FieldLabel(text = "Url Link", required = false)
                AppTextField(
                    value = urlLink,
                    onValueChange = { urlLink = it },
                    placeholder = "https://",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card)
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.extendedColors.warning,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "Isi minimal satu: berkas materi atau tautan. Deskripsi tidak wajib.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.md),
                    )
                }
            }
        }
    }
}

/** One bordered block of the upload form. */
@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        content()
    }
}

@Composable
private fun FieldLabel(text: String, required: Boolean, counter: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (required) {
            Text(" *", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.weight(1f))
        counter?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TargetRadio(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FileDropZone(fileName: String?, onPick: () -> Unit, onClear: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.5.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onPick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = scheme.primary)
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
            Text(
                fileName ?: "Belum ada berkas dipilih",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface,
            )
            Text(
                "Format PDF atau gambar · maksimal 12 MB",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        if (fileName != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus berkas", tint = scheme.error)
            }
        }
    }
}

@Composable
private fun TargetRow(label: String, selected: Boolean, onSelect: () -> Unit, dropdown: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(110.dp)) {
            RadioButton(selected = selected, onClick = onSelect)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.weight(1f)) { dropdown() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = if (label.isNotBlank()) ({ Text(label) }) else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

private fun queryFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
    }
    return name
}

private fun buildRequestData(
    name: String,
    desc: String,
    link: String,
    target: UploadTarget,
    grade: String,
    majorId: Int?,
    classId: Int?,
    subjectId: Int?,
): Map<String, RequestBody> {
    val plain = "text/plain".toMediaTypeOrNull()
    val map = mutableMapOf<String, RequestBody>()
    map["name"] = name.toRequestBody(plain)
    map["description"] = desc.toRequestBody(plain)
    map["school_subject_id"] = subjectId.toString().toRequestBody(plain)
    if (link.isNotBlank()) map["link"] = link.toRequestBody(plain)

    when (target) {
        UploadTarget.JENJANG -> if (grade.isNotEmpty()) map["grade"] = grade.toRequestBody(plain)
        UploadTarget.JURUSAN -> majorId?.let { map["school_major_id"] = it.toString().toRequestBody(plain) }
        UploadTarget.KELAS -> classId?.let { map["school_classes_id"] = it.toString().toRequestBody(plain) }
    }
    return map
}

private fun buildFilePart(context: android.content.Context, uri: Uri, fileName: String): MultipartBody.Part? {
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    if (file.length() > 12 * 1024 * 1024L) return null
    return MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("application/pdf".toMediaTypeOrNull()))
}
