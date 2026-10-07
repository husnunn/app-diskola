package id.diskola.app.ui.screens.materi

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
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
import id.diskola.app.viewmodel.MateriDetailViewModel
import java.text.DecimalFormat

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
 * A single material (`MateriDetailPage`, doc `05-pembelajaran-materi-tugas.md` §2.4) — every block
 * below the hero is conditional. id-based: [MateriDetailViewModel] resolves from Room, falling
 * back to the detail API, and an invalid/missing id shows "Materi Tidak tersedia" instead of the
 * legacy NPE crash.
 */
@Composable
fun MateriDetailScreen(
    materiId: Int,
    subjectId: Int,
    isTeacher: Boolean,
    onBack: () -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val materi by viewModel.materi.collectAsStateWithLifecycle()
    val invalid by viewModel.invalid.collectAsStateWithLifecycle()
    val fileLoading by viewModel.fileLoading.collectAsStateWithLifecycle()
    val linkPreview by viewModel.linkPreview.collectAsStateWithLifecycle()
    val linkPreviewLoading by viewModel.linkPreviewLoading.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()
    val openFileIntent by viewModel.openFileIntent.collectAsStateWithLifecycle()
    val pdfPath by viewModel.pdfPath.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var permissionDialogFor by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(materiId, subjectId, isTeacher) { viewModel.load(materiId, subjectId, isTeacher) }

    LaunchedEffect(openFileIntent) {
        openFileIntent?.let {
            try {
                context.startActivity(it)
            } catch (_: Exception) {
            }
            viewModel.consumeOpenFileIntent()
        }
    }
    LaunchedEffect(pdfPath) {
        pdfPath?.let { path ->
            onOpenPdf(path, materi?.name.orEmpty())
            viewModel.consumePdfPath()
        }
    }

    DetailScaffold(title = "Detail Materi", onBack = onBack, modifier = modifier) { padding ->
        val item = materi
        if (item == null) {
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
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        item.name.ifBlank { "Tanpa judul" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).background(scheme.surfaceContainerLow, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                initialsOf(item.teacher_name),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = scheme.primary,
                            )
                        }
                        Column(modifier = Modifier.padding(start = Spacing.md)) {
                            Text(
                                item.teacher_name.ifBlank { "-" },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface,
                            )
                            val meta = listOf(item.created_at_label, item.targetLabel().orEmpty()).filter { it.isNotBlank() }
                            if (meta.isNotEmpty()) {
                                Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {
                    BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
                }
                infoMessage?.let {
                    BannerError(message = it, onDismiss = { viewModel.consumeInfoMessage() })
                }

                if (item.description.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("DESKRIPSI")
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }

                if (item.file_name.isNotBlank() && item.file_path.isNotBlank()) {
                    FileCard(
                        title = "FILE MATERI",
                        fileName = item.file_name,
                        fileSize = formatFileSize(item.file_size),
                        loading = fileLoading,
                        onOpen = { permissionDialogFor = item.file_path to item.file_name },
                        onDownload = { viewModel.downloadFile(item.file_path, item.file_name) },
                    )
                }

                if (item.link.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("TAUTAN TERKAIT")
                        LinkPreviewCard(
                            url = item.link,
                            loading = linkPreviewLoading,
                            data = linkPreview,
                            onClick = { openUrlInBrowser(context, item.link) },
                        )
                    }
                }

                if (item.explanation_file_path.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("PEMBAHASAN")
                        // Doc §2.4: the pembahasan card's name is the raw path/URL, not a
                        // formatted file name — that's the legacy behaviour, not a bug we
                        // introduced, so it's kept as-is rather than "improved".
                        FileCard(
                            title = null,
                            fileName = item.explanation_file_path,
                            fileSize = null,
                            loading = fileLoading,
                            onOpen = { permissionDialogFor = item.explanation_file_path to item.file_name },
                            onDownload = { viewModel.downloadFile(item.explanation_file_path, item.file_name) },
                        )
                    }
                }
            }
        }
    }

    if (invalid) {
        AppDialog(
            onDismiss = onBack,
            dismissible = false,
            title = "Materi Tidak tersedia",
            body = "Halaman yang kamu cari sudah tidak tersedia",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
        )
    }

    permissionDialogFor?.let { (url, fileName) ->
        AppDialog(
            onDismiss = { permissionDialogFor = null },
            title = "Akses File Diperlukan",
            body = "Aplikasi ini membutuhkan akses ke penyimpanan Anda untuk memilih atau membuka file dalam proses unggah dokumen tugas dan materi. Data ini hanya digunakan dalam aplikasi dan tidak akan dibagikan tanpa izin Anda.",
            primaryButtonText = "Setuju",
            onPrimaryClick = {
                permissionDialogFor = null
                viewModel.openFile(url, fileName)
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { permissionDialogFor = null },
        )
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    } catch (_: Exception) {
    }
}

@Composable
private fun FileCard(
    title: String?,
    fileName: String,
    fileSize: String?,
    loading: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        title?.let { SectionLabel(it) }
        Column(
            modifier = Modifier
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
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = scheme.onErrorContainer)
                }
                Column(modifier = Modifier.padding(start = Spacing.md)) {
                    Text(
                        if (fileSize.isNullOrBlank()) fileName else "$fileName | $fileSize",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(
                    text = "Lihat",
                    onClick = onOpen,
                    loading = loading,
                    leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "Unduh",
                    onClick = onDownload,
                    variant = ButtonVariant.Outlined,
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
