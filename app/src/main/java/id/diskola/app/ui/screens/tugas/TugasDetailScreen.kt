package id.diskola.app.ui.screens.tugas

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.LinkPreviewCard
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.viewmodel.SelectedAnswerFile
import id.diskola.app.viewmodel.TugasDetailViewModel
import java.text.DecimalFormat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private val ANSWER_MIME_TYPES = arrayOf(
    "application/pdf", "image/jpeg", "image/png", "image/*",
    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
)

private fun formatFileSize(raw: String): String {
    val bytes = raw.toLongOrNull() ?: return raw
    val df = DecimalFormat("0.00")
    val kb = 1024.0
    val mb = kb * kb
    val gb = mb * kb
    return when {
        bytes < mb -> "${df.format(bytes / kb)} Kb"
        bytes < gb -> "${df.format(bytes / mb)} Mb"
        else -> "${df.format(bytes / gb)} Gb"
    }
}

/**
 * `HomeworkDetailPage` (doc `05-pembelajaran-materi-tugas.md` §5.5) — also the teacher's read-only
 * "Lihat Tugas" view (§6.4) via `isTeacher`, which hides the requirement card and submit bar.
 */
@Composable
fun TugasDetailScreen(
    tugasId: Int,
    type: Int,
    isTeacher: Boolean,
    onBack: () -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TugasDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val item by viewModel.tugas.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    val answerFiles by viewModel.answerFiles.collectAsStateWithLifecycle()
    val readDone by viewModel.readDone.collectAsStateWithLifecycle()
    val uploadDone by viewModel.uploadDone.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    val studentLink by viewModel.studentLink.collectAsStateWithLifecycle()
    val linkPreview by viewModel.linkPreview.collectAsStateWithLifecycle()
    val linkPreviewLoading by viewModel.linkPreviewLoading.collectAsStateWithLifecycle()
    val teacherLinkPreview by viewModel.teacherLinkPreview.collectAsStateWithLifecycle()
    val teacherLinkPreviewLoading by viewModel.teacherLinkPreviewLoading.collectAsStateWithLifecycle()
    val fileLoading by viewModel.fileLoading.collectAsStateWithLifecycle()
    val pdfPath by viewModel.pdfPath.collectAsStateWithLifecycle()
    val openFileIntent by viewModel.openFileIntent.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val submitSuccess by viewModel.submitSuccess.collectAsStateWithLifecycle()

    var permissionDialogFor by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    var fileSizeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tugasId, type, isTeacher) { viewModel.load(tugasId, type, isTeacher) }
    LaunchedEffect(submitSuccess) { if (submitSuccess) onBack() }
    LaunchedEffect(openFileIntent) {
        openFileIntent?.let {
            try { context.startActivity(it) } catch (_: Exception) {}
            viewModel.consumeOpenFileIntent()
        }
    }
    LaunchedEffect(pdfPath) {
        pdfPath?.let { path ->
            onOpenPdf(path, item?.title.orEmpty())
            viewModel.consumePdfPath()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            val (name, size) = queryFileInfo(context, uri)
            val fileName = name ?: "jawaban"
            val error = viewModel.addSelectedFile(SelectedAnswerFile(uri, fileName, size ?: 0L))
            if (error != null) fileSizeError = error
        }
    }

    val isFinished = type != HomeworkTable.TYPE_BACKLOG
    val needRead = (item?.downloded ?: 0) > 0
    val needUpload = (item?.uploaded ?: 0) > 0
    val showRequirementsAndSubmit = !isTeacher && !isFinished

    DetailScaffold(
        title = "Detail Tugas",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            if (showRequirementsAndSubmit && item != null) {
                val allMet = (!needRead || readDone) && (!needUpload || uploadDone)
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                ) {
                    Text(
                        if (allMet) {
                            "Semua syarat terpenuhi ✓"
                        } else {
                            val missing = buildList {
                                if (needRead && !readDone) add("baca soal")
                                if (needUpload && !uploadDone) add("upload jawaban")
                            }.joinToString(" & ")
                            "Lengkapi $missing untuk mengirim"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (allMet) MaterialTheme.extendedColors.success else MaterialTheme.extendedColors.warning,
                    )
                    AppButton(
                        text = "Kirim Tugas",
                        onClick = {
                            val plain = "text/plain".toMediaTypeOrNull()
                            val parts = selectedFiles.map { f -> buildAnswerFilePart(context, f) }
                            viewModel.submit(parts)
                        },
                        enabled = allMet,
                        loading = false,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    )
                }
            }
        },
    ) { padding ->
        val current = item
        if (notFound) {
            // handled by the dialog below
            Box(modifier = Modifier.padding(padding).fillMaxSize())
        } else if (current == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding)
                    .padding(bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        current.title.ifBlank { "Tanpa judul" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                    )
                    val meta = listOf(current.subject_name, current.teacher_name.takeIf { it.isNotBlank() }?.let { "Guru · $it" })
                        .filterNotNull().filter { it.isNotBlank() }
                    if (meta.isNotEmpty()) {
                        Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }

                if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
                infoMessage?.let { BannerError(message = it, onDismiss = { viewModel.consumeInfoMessage() }) }
                fileSizeError?.let {
                    BannerError(message = it, onDismiss = { fileSizeError = null })
                }

                if (!isTeacher) {
                    when {
                        isFinished -> Text("Kamu sudah mengerjakan tugas", style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
                        current.is_overdue -> Text("Waktu pengumpulan tugas terlambat", style = MaterialTheme.typography.bodyMedium, color = scheme.error)
                    }
                }

                if (showRequirementsAndSubmit && (needRead || needUpload)) {
                    RequirementCard(needRead = needRead, needUpload = needUpload, readDone = readDone, uploadDone = uploadDone)
                }

                if (current.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("DESKRIPSI")
                        Text(current.description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }

                if (current.file_name.isNotBlank() || current.link.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel(if (needRead) "SOAL · WAJIB DIBACA" else "SOAL")
                        if (current.file_name.isNotBlank()) {
                            TugasFileCard(
                                fileName = current.file_name,
                                fileSize = formatFileSize(current.file_size),
                                loading = fileLoading,
                                readLabel = if (readDone) "Sudah dibaca ✓" else "Baca tugas",
                                onOpen = { permissionDialogFor = Triple(current.file_path, current.file_name, true) },
                                onDownload = { viewModel.downloadFile(current.file_path, current.file_name) },
                            )
                        }
                        if (current.link.isNotBlank()) {
                            LinkPreviewCard(
                                url = current.link,
                                loading = teacherLinkPreviewLoading,
                                data = teacherLinkPreview,
                                onClick = { if (!isTeacher) viewModel.openFile(current.link, "soal", true) },
                            )
                        }
                    }
                }

                if (answerFiles.isNotEmpty() || current.link_student.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("JAWABAN TERKUMPUL")
                        if (answerFiles.isNotEmpty()) {
                            Text("${answerFiles.size} file terlampir", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                        answerFiles.forEach { file ->
                            TugasFileCard(
                                fileName = file.file_name,
                                fileSize = formatFileSize(file.file_size),
                                loading = fileLoading,
                                readLabel = "Baca",
                                onOpen = { permissionDialogFor = Triple(file.file_path, file.file_name, false) },
                                onDownload = { viewModel.downloadFile(file.file_path, file.file_name) },
                            )
                        }
                        if (current.link_student.isNotBlank()) {
                            LinkPreviewCard(url = current.link_student, loading = false, data = null, onClick = {})
                        }
                    }
                }

                if (showRequirementsAndSubmit && needUpload) {
                    KumpulkanJawabanSection(
                        selectedFiles = selectedFiles,
                        studentLink = studentLink,
                        linkPreview = linkPreview,
                        linkPreviewLoading = linkPreviewLoading,
                        onPickFiles = { filePickerLauncher.launch(ANSWER_MIME_TYPES) },
                        onRemoveFile = { viewModel.removeSelectedFile(it) },
                        onLinkChanged = { viewModel.onStudentLinkChanged(it) },
                    )
                }

                if (current.explanation_file_path.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("PEMBAHASAN")
                        TugasFileCard(
                            fileName = current.explanation_file_path,
                            fileSize = "",
                            loading = fileLoading,
                            readLabel = "baca materi",
                            onOpen = { permissionDialogFor = Triple(current.explanation_file_path, current.title, false) },
                            onDownload = { viewModel.downloadFile(current.explanation_file_path, current.title) },
                        )
                    }
                }
            }
        }
    }

    if (notFound) {
        AppDialog(
            onDismiss = onBack,
            dismissible = false,
            title = "Tugas Tidak Tersedia",
            body = "Data tugas tidak ditemukan. Coba muat ulang daftar Tugas.",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
        )
    }

    permissionDialogFor?.let { (url, fileName, markRead) ->
        AppDialog(
            onDismiss = { permissionDialogFor = null },
            title = "Akses File Diperlukan",
            body = "Aplikasi ini membutuhkan akses ke penyimpanan Anda untuk memilih atau membuka file dalam proses unggah dokumen tugas dan materi. Data ini hanya digunakan dalam aplikasi dan tidak akan dibagikan tanpa izin Anda.",
            primaryButtonText = "Setuju",
            onPrimaryClick = {
                permissionDialogFor = null
                viewModel.openFile(url, fileName, markRead)
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { permissionDialogFor = null },
        )
    }
}

@Composable
private fun RequirementCard(needRead: Boolean, needUpload: Boolean, readDone: Boolean, uploadDone: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionLabel("SYARAT PENGUMPULAN")
        if (needRead) RequirementRow(number = 1, label = "Baca soal", done = readDone)
        if (needUpload) RequirementRow(number = 2, label = "Upload jawaban", done = uploadDone)
    }
}

@Composable
private fun RequirementRow(number: Int, label: String, done: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (done) scheme.primary else scheme.surfaceContainerLow, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (done) "✓" else number.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = if (done) scheme.onPrimary else scheme.onSurfaceVariant,
            )
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f).padding(start = Spacing.md))
        Text(
            if (done) "Selesai" else "Belum",
            style = MaterialTheme.typography.labelSmall,
            color = if (done) MaterialTheme.extendedColors.success else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TugasFileCard(
    fileName: String,
    fileSize: String,
    loading: Boolean,
    readLabel: String,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(scheme.errorContainer, DiskolaExtraShapes.iconBox),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = scheme.onErrorContainer)
            }
            Column(modifier = Modifier.padding(start = Spacing.md)) {
                Text(
                    if (fileSize.isBlank()) fileName else "$fileName | $fileSize",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppButton(
                text = readLabel,
                onClick = onOpen,
                loading = loading,
                leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = "Download",
                onClick = onDownload,
                variant = ButtonVariant.Outlined,
                leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KumpulkanJawabanSection(
    selectedFiles: List<SelectedAnswerFile>,
    studentLink: String,
    linkPreview: id.diskola.app.repository.LinkPreviewData?,
    linkPreviewLoading: Boolean,
    onPickFiles: () -> Unit,
    onRemoveFile: (Uri) -> Unit,
    onLinkChanged: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel("KUMPULKAN JAWABAN · WAJIB")
        Text(
            "jpeg, png, pdf, doc, xls, ppt · maks. 12 MB/file",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                .border(1.5.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                .clickable(onClick = onPickFiles)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = scheme.primary)
            Text(
                if (selectedFiles.isEmpty()) "Ketuk untuk lampirkan file" else "${selectedFiles.size} file dipilih",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
            Text("Bisa lebih dari 1 file", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        selectedFiles.forEach { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerLow, DiskolaExtraShapes.card)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${file.name} | ${formatFileSize(file.sizeBytes.toString())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemoveFile(file.uri) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Hapus berkas", tint = scheme.error)
                }
            }
        }

        Text("Atau tempel link · OPSIONAL", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        id.diskola.app.ui.components.AppTextField(
            value = studentLink,
            onValueChange = onLinkChanged,
            placeholder = "https://",
        )
        if (studentLink.isNotBlank()) {
            LinkPreviewCard(url = studentLink, loading = linkPreviewLoading, data = linkPreview, onClick = {})
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

private fun buildAnswerFilePart(context: android.content.Context, file: SelectedAnswerFile): MultipartBody.Part {
    val localFile = File(context.cacheDir, file.name)
    context.contentResolver.openInputStream(file.uri)?.use { input ->
        localFile.outputStream().use { output -> input.copyTo(output) }
    }
    val mimeType = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(localFile.extension.lowercase()) ?: "application/octet-stream"
    return MultipartBody.Part.createFormData("file[]", localFile.name, localFile.asRequestBody(mimeType.toMediaTypeOrNull()))
}
