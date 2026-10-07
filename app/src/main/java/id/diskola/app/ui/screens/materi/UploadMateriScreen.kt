package id.diskola.app.ui.screens.materi

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.LinkPreviewCard
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.components.SimpleDropdown
import id.diskola.app.ui.navigation.Route
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.UploadMateriViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

private enum class UploadTarget { JENJANG, JURUSAN, KELAS }

private val ALLOWED_MIME_TYPES = arrayOf(
    "application/pdf",
    "image/jpeg",
    "image/png",
    "image/*",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
)

private const val MAX_FILE_BYTES = 12 * 1024 * 1024L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadMateriScreen(
    route: Route.UploadMateri,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UploadMateriViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var judul by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var urlLink by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(if (route.subjectId >= 0) route.subjectId else null) }
    var selectedMajorId by remember { mutableStateOf<Int?>(null) }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf(UploadTarget.JENJANG) }
    var selectedGrade by remember { mutableStateOf<Int?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }
    var fileSizeError by remember { mutableStateOf<String?>(null) }

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val majors by viewModel.majors.collectAsStateWithLifecycle()
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val uploadSuccess by viewModel.uploadSuccess.collectAsStateWithLifecycle()
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val linkPreview by viewModel.linkPreview.collectAsStateWithLifecycle()
    val linkPreviewLoading by viewModel.linkPreviewLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadReferenceData()
        if (route.isEdit && route.materiId > 0) viewModel.loadExisting(route.materiId)
    }
    LaunchedEffect(existing) {
        val item = existing
        if (item != null && !prefilled) {
            prefilled = true
            judul = item.name
            deskripsi = item.description
            urlLink = item.link
            selectedSubjectId = item.subject_id
            selectedFileName = item.file_name.takeIf { it.isNotBlank() }
            target = when {
                item.major_id > 0 -> UploadTarget.JURUSAN
                item.class_id > 0 -> UploadTarget.KELAS
                else -> UploadTarget.JENJANG
            }
            selectedMajorId = item.major_id.takeIf { it > 0 }
            selectedClassId = item.class_id.takeIf { it > 0 }
            selectedGrade = item.grade.takeIf { it > 0 }
        }
    }
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) onDone()
    }
    LaunchedEffect(urlLink) { viewModel.onLinkChanged(urlLink) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val (name, size) = queryFileInfo(context, uri)
        if (size != null && size > MAX_FILE_BYTES) {
            fileSizeError = "ukuran file melebihi batas maksimal (12Mb)"
            return@rememberLauncherForActivityResult
        }
        fileSizeError = null
        selectedFileUri = uri
        selectedFileName = name ?: "upload"
    }

    val hasTarget = when (target) {
        UploadTarget.JENJANG -> selectedGrade != null
        UploadTarget.JURUSAN -> selectedMajorId != null
        UploadTarget.KELAS -> selectedClassId != null
    }
    val canSubmit = judul.isNotBlank() &&
        selectedSubjectId != null &&
        hasTarget &&
        (selectedFileUri != null || selectedFileName != null || urlLink.isNotBlank())

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
                        viewModel.submit(route.isEdit, route.materiId, data, filePart)
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
            if (errorMessage.isNotBlank()) {
                BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
            }

            FormCard {
                FieldLabel(text = "Judul Materi", required = true, counter = "${judul.length}/500")
                id.diskola.app.ui.components.AppTextField(
                    value = judul,
                    onValueChange = { if (it.length <= 500) judul = it },
                    placeholder = "mis. Struktur Data: Array dan ArrayList",
                )

                FieldLabel(text = "Deskripsi", required = false, counter = "${deskripsi.length}/5000")
                id.diskola.app.ui.components.AppTextField(
                    value = deskripsi,
                    onValueChange = { if (it.length <= 5000) deskripsi = it },
                    placeholder = "Ringkasan isi materi (opsional)",
                )

                FieldLabel(text = "Mata Pelajaran", required = true)
                SimpleDropdown(
                    options = subjects.map { it.id.toInt() to it.name },
                    selected = selectedSubjectId,
                    placeholder = "Pilih mata pelajaran",
                    onSelected = { selectedSubjectId = it },
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
                    selectedGrade = null
                    selectedClassId = null
                }
                TargetRadio("Kelas", target == UploadTarget.KELAS) {
                    target = UploadTarget.KELAS
                    selectedGrade = null
                    selectedMajorId = null
                }

                when (target) {
                    UploadTarget.JENJANG -> SimpleDropdown(
                        options = grades.map { it.id to it.name },
                        selected = selectedGrade,
                        placeholder = "Pilih jenjang",
                        onSelected = { selectedGrade = it },
                    )

                    UploadTarget.JURUSAN -> SimpleDropdown(
                        options = majors.map { it.id to it.name },
                        selected = selectedMajorId,
                        placeholder = "Pilih jurusan",
                        onSelected = { selectedMajorId = it },
                    )

                    UploadTarget.KELAS -> SimpleDropdown(
                        options = classes.map { it.id to (if (it.grade > 0) "${it.grade} - ${it.name}" else it.name) },
                        selected = selectedClassId,
                        placeholder = "Pilih kelas",
                        onSelected = { selectedClassId = it },
                    )
                }

                if (!hasTarget) {
                    Text(
                        "Pilih salah satu target di atas sebelum mengunggah materi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            FormCard {
                FieldLabel(text = "File Materi", required = false)
                FileDropZone(
                    fileName = selectedFileName,
                    onPick = { filePickerLauncher.launch(ALLOWED_MIME_TYPES) },
                    onClear = {
                        selectedFileUri = null
                        selectedFileName = null
                        fileSizeError = null
                    },
                )
                fileSizeError?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }

                FieldLabel(text = "Url Link", required = false)
                id.diskola.app.ui.components.AppTextField(
                    value = urlLink,
                    onValueChange = { urlLink = it },
                    placeholder = "https://",
                )
                if (urlLink.isNotBlank()) {
                    LinkPreviewCard(
                        url = urlLink,
                        loading = linkPreviewLoading,
                        data = linkPreview,
                        onClick = {},
                    )
                }

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
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
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
                "PDF, gambar, Word, Excel, atau PowerPoint · maksimal 12 MB",
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

private fun queryFileInfo(context: android.content.Context, uri: Uri): Pair<String?, Long?> {
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

private fun buildRequestData(
    name: String,
    desc: String,
    link: String,
    target: UploadTarget,
    grade: Int?,
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
        UploadTarget.JENJANG -> grade?.let { map["grade"] = it.toString().toRequestBody(plain) }
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
    if (!file.exists()) return null
    val mimeType = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
    return MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
}
