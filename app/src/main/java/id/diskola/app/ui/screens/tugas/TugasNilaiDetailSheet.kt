package id.diskola.app.ui.screens.tugas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.TugasNilaiDetailViewModel

/** `HomeworkNilaiDetailPage` (doc §5.5) — subject/teacher/score/information_label + optional
 * pembahasan card, the "Nilai" tab's tap target. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasNilaiDetailSheet(
    tugasId: Int,
    onDismiss: () -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TugasNilaiDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val item by viewModel.tugas.collectAsStateWithLifecycle()
    val fileLoading by viewModel.fileLoading.collectAsStateWithLifecycle()
    val pdfPath by viewModel.pdfPath.collectAsStateWithLifecycle()
    val openFileIntent by viewModel.openFileIntent.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()

    var permissionDialogFor by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(tugasId) { viewModel.load(tugasId) }
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

    AppBottomSheet(title = item?.title?.ifBlank { "Detail Tugas" } ?: "Detail Tugas", onDismiss = onDismiss, modifier = modifier) {
        val current = item
        if (current != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                if (current.subject_name.isNotBlank()) {
                    Text(current.subject_name, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                }
                if (current.teacher_name.isNotBlank()) {
                    Text("Guru · ${current.teacher_name}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                infoMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = scheme.error) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).background(scheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(current.score.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = scheme.onPrimaryContainer)
                    }
                    if (current.information_label.isNotBlank()) {
                        Text(
                            current.information_label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Spacing.md),
                        )
                    }
                }
                if (current.explanation_file_path.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("PEMBAHASAN")
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            AppButton(
                                text = "baca materi",
                                onClick = { permissionDialogFor = current.explanation_file_path to current.title },
                                loading = fileLoading,
                                modifier = Modifier.weight(1f),
                            )
                            AppButton(
                                text = "Download",
                                onClick = { viewModel.downloadFile(current.explanation_file_path, current.title) },
                                variant = ButtonVariant.Outlined,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
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
