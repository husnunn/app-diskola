package id.app.education.ui.screens.materi

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
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppTextField
import id.app.education.ui.navigation.Route
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.MateriViewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (route.isEdit) "Edit Materi" else "Upload Materi") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) }
                },
            )
        },
        bottomBar = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(Spacing.xl)) {
                AppButton(
                    text = if (route.isEdit) "Simpan Perubahan" else "Posting Materi",
                    onClick = {
                        val data = buildRequestData(judul, deskripsi, urlLink, target, selectedGrade, selectedMajorId, selectedClassId, selectedSubjectId)
                        val filePart = selectedFileUri?.let { buildFilePart(context, it, selectedFileName ?: "upload.pdf") }
                        if (route.isEdit) {
                            viewModel.updateMateri(route.materiId, data, filePart)
                        } else {
                            viewModel.uploadMateri(data, filePart)
                        }
                    },
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AppTextField(value = judul, onValueChange = { if (it.length <= 500) judul = it }, label = "Judul Materi", placeholder = "Ketik judul di sini")
            AppTextField(value = deskripsi, onValueChange = { if (it.length <= 5000) deskripsi = it }, label = "Deskripsi", placeholder = "Ketik deskripsi")

            SimpleDropdown(
                label = "Mata Pelajaran",
                options = teacherSubjects.map { it.name },
                selected = selectedSubjectName,
                onSelected = { name ->
                    selectedSubjectName = name
                    selectedSubjectId = teacherSubjects.find { it.name == name }?.id
                },
            )

            Text("Ditampilkan ke", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

            TargetRow(
                label = "Jenjang",
                selected = target == UploadTarget.JENJANG,
                onSelect = { target = UploadTarget.JENJANG },
            ) {
                SimpleDropdown(label = "", options = listOf("10", "11", "12"), selected = selectedGrade, onSelected = { selectedGrade = it }, enabled = target == UploadTarget.JENJANG)
            }
            TargetRow(
                label = "Jurusan",
                selected = target == UploadTarget.JURUSAN,
                onSelect = { target = UploadTarget.JURUSAN },
            ) {
                SimpleDropdown(
                    label = "",
                    options = majors.map { it.name },
                    selected = majors.find { it.id == selectedMajorId }?.name.orEmpty(),
                    onSelected = { name -> selectedMajorId = majors.find { it.name == name }?.id },
                    enabled = target == UploadTarget.JURUSAN,
                )
            }
            TargetRow(
                label = "Kelas",
                selected = target == UploadTarget.KELAS,
                onSelect = { target = UploadTarget.KELAS },
            ) {
                SimpleDropdown(
                    label = "",
                    options = classes.map { it.name },
                    selected = classes.find { it.id == selectedClassId }?.name.orEmpty(),
                    onSelected = { name -> selectedClassId = classes.find { it.name == name }?.id },
                    enabled = target == UploadTarget.KELAS,
                )
            }

            androidx.compose.material3.HorizontalDivider()

            Text("File Materi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Upload file · format PDF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    AppButton(
                        text = "Lampirkan File",
                        onClick = { filePickerLauncher.launch("application/pdf") },
                        variant = id.app.education.ui.components.ButtonVariant.Outlined,
                        modifier = Modifier.width(180.dp),
                    )
                }
            }
            selectedFileName?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            AppTextField(value = urlLink, onValueChange = { urlLink = it }, label = "Url Link", placeholder = "Ketik url")
            Spacer(modifier = Modifier.size(Spacing.huge))
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
