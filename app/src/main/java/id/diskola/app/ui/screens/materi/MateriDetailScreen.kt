package id.diskola.app.ui.screens.materi

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.FileViewerHost
import id.diskola.app.ui.components.displayNameOf
import id.diskola.app.ui.components.openLink
import id.diskola.app.ui.components.LinkPreviewCard
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.screens.tugas.EmptyNote
import id.diskola.app.ui.screens.tugas.TextCard
import id.diskola.app.ui.screens.tugas.TugasFileCard
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
 * below the header card is conditional. id-based: [MateriDetailViewModel] resolves from Room, falling
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

    FileViewerHost(intent = openFileIntent, fileName = materi?.file_name, onConsumed = viewModel::consumeOpenFileIntent)
    LaunchedEffect(pdfPath) {
        pdfPath?.let { path ->
            onOpenPdf(path, materi?.name.orEmpty())
            viewModel.consumePdfPath()
        }
    }

    DetailScaffold(title = "Detail Materi", onBack = onBack, modifier = modifier) { padding ->
        val item = materi
        when {
            item != null -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding)
                    .padding(top = Spacing.md, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                if (errorMessage.isNotBlank()) {
                    BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
                }
                infoMessage?.let {
                    BannerError(message = it, onDismiss = { viewModel.consumeInfoMessage() })
                }

                MateriHeaderCard(item)

                val hasFile = item.file_name.isNotBlank() && item.file_path.isNotBlank()
                val hasExplanation = item.explanation_file_path.isNotBlank()
                // Doc §2.4: every block below the header is shown only when it has content.
                if (item.description.isNotBlank()) {
                    MateriSection("Deskripsi", Icons.Rounded.Description) { TextCard(item.description) }
                }

                if (hasFile) {
                    MateriSection("File Materi", Icons.Rounded.AttachFile) {
                        TugasFileCard(
                            fileName = item.file_name,
                            fileSize = formatFileSize(item.file_size),
                            loading = fileLoading,
                            readLabel = "Lihat",
                            downloadLabel = "Unduh",
                            onOpen = { permissionDialogFor = item.file_path to item.file_name },
                            onDownload = { viewModel.downloadFile(item.file_path, item.file_name) },
                        )
                    }
                }

                if (item.link.isNotBlank()) {
                    MateriSection("Tautan Terkait", Icons.Rounded.Link) {
                        LinkPreviewCard(
                            url = item.link,
                            loading = linkPreviewLoading,
                            data = linkPreview,
                            onClick = { openLink(context, item.link) },
                        )
                    }
                }

                if (hasExplanation) {
                    MateriSection("Pembahasan", Icons.Rounded.Lightbulb) {
                        // The card shows the file's name, not the raw path/URL the legacy card showed (doc §2.4);
                        // the same name is what "Lihat" and "Unduh" use, as in Detail Tugas.
                        val explanationName = displayNameOf(item.explanation_file_path)
                        TugasFileCard(
                            fileName = explanationName,
                            fileSize = "",
                            loading = fileLoading,
                            readLabel = "Lihat",
                            downloadLabel = "Unduh",
                            onOpen = { permissionDialogFor = item.explanation_file_path to explanationName },
                            onDownload = { viewModel.downloadFile(item.explanation_file_path, explanationName) },
                        )
                    }
                }

                if (item.description.isBlank() && !hasFile && item.link.isBlank() && !hasExplanation) {
                    EmptyNote("Materi ini belum memiliki isi", Icons.Rounded.Description)
                }
            }
            // The invalid-id dialog below covers this state.
            invalid -> Box(modifier = Modifier.padding(padding).fillMaxSize())
            else -> LoadingState(modifier = Modifier.padding(padding).padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md))
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
