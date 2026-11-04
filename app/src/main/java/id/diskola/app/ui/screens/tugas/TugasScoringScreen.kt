package id.diskola.app.ui.screens.tugas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.FileViewerHost
import id.diskola.app.ui.components.LinkPreviewCard
import id.diskola.app.ui.components.openLink
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AnswerFileRules
import id.diskola.app.viewmodel.TugasScoringViewModel
import id.diskola.app.viewmodel.TugasTerkumpulViewModel

/** `HomeworkScoringPage` (doc §6.4) — integer 0-100 score input, submission info, and the
 * student's answer files/link, read-only. */
@Composable
fun TugasScoringScreen(
    collectedId: Int,
    assignmentId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    terkumpulViewModel: TugasTerkumpulViewModel,
    viewModel: TugasScoringViewModel = hiltViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val assignment by viewModel.assignment.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val fileLoading by viewModel.fileLoading.collectAsStateWithLifecycle()
    val pdfPath by viewModel.pdfPath.collectAsStateWithLifecycle()
    val openFileIntent by viewModel.openFileIntent.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()

    FileViewerHost(intent = openFileIntent, fileName = null, onConsumed = viewModel::consumeOpenFileIntent)
    LaunchedEffect(pdfPath) {
        pdfPath?.let { path ->
            onOpenPdf(path, assignment?.student?.name?.let { "Jawaban $it" } ?: "Jawaban")
            viewModel.consumePdfPath()
        }
    }
    LaunchedEffect(assignmentId) {
        terkumpulViewModel.findAssignment(assignmentId)?.let { viewModel.load(it) }
    }
    LaunchedEffect(saveSuccess) { if (saveSuccess) onSaved() }

    DetailScaffold(title = "Nilai Tugas", onBack = onBack, modifier = modifier) { padding ->
        val current = assignment
        if (current != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding)
                    .padding(bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                if (errorMessage.isNotBlank()) {
                    BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
                }
                infoMessage?.let { BannerError(message = it, onDismiss = viewModel::consumeInfoMessage) }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionLabel("SKOR")
                    AppTextField(
                        value = score,
                        onValueChange = { viewModel.onScoreChanged(it) },
                        placeholder = "--",
                        keyboardType = KeyboardType.Number,
                    )
                }
                if (current.upload_at.isNotBlank()) {
                    Text("Dikumpulkan pada ${current.upload_at}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    InfoRow("Peserta", current.student.name)
                    InfoRow("NIS", current.student.nisn)
                    InfoRow("Kelas", current.student.`class`)
                }
                val answerFiles = remember(current) { AnswerFileRules.filesOf(current) }
                val answerLinks = remember(current) { AnswerFileRules.linksOf(current) }
                if (AnswerFileRules.hasAnswer(answerFiles, answerLinks)) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionLabel("JAWABAN TERKUMPUL")
                        answerFiles.forEach { file ->
                            TugasFileCard(
                                fileName = AnswerFileRules.name(file),
                                fileSize = formatFileSize(AnswerFileRules.size(file)),
                                loading = fileLoading,
                                readLabel = "Baca",
                                onOpen = { viewModel.openAnswerFile(file) },
                                onDownload = { viewModel.downloadAnswerFile(file) },
                            )
                        }
                        answerLinks.firstOrNull()?.let { link ->
                            LinkPreviewCard(url = link, loading = false, data = null, onClick = { openLink(context, link) })
                        }
                    }
                }
                AppButton(
                    text = "simpan",
                    onClick = { viewModel.submit(collectedId) },
                    enabled = score.isNotBlank(),
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value.ifBlank { "-" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
