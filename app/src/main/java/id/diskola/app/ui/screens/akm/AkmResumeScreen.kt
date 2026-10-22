package id.diskola.app.ui.screens.akm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.akm.AkmExam
import id.diskola.app.dataclass.akm.AkmInstruction
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.ExamModeHeader
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AkmPenaltyRules
import id.diskola.app.utils.AkmSessionRules
import id.diskola.app.viewmodel.AkmResumeViewModel
import id.diskola.app.viewmodel.SubmitUi

/**
 * Exam lobby: the instruction tree the student works through, and "Kumpulkan". Leaving never loses an answer (every one is already in
 * Room), but while the session runs it is not free: in strict mode it is refused, otherwise the student is warned and the exit is
 * recorded as leaving without handing in (doc 06 §12.1).
 *
 * Texts are legacy's (doc 06 §5.5): loading "Sedang memproses jawaban", alert "Berhasil" / "Jawaban sedang dikumpulkan, silahkan
 * tunggu sampai jawabanmu dinilai" / "Baik", alert "Gagal" / "Terdapat kesalahan dalam pengunggahan jawaban" / "Coba lagi", and at the
 * end of the exam "Waktu ujian telah berakhir, jawaban akan dikumpulkan" with a single "OK".
 */
@Composable
fun AkmResumeScreen(
    examId: Int,
    onBack: () -> Unit,
    onOpenInstruction: (instructionId: Int) -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmResumeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val submit by viewModel.submit.collectAsStateWithLifecycle()
    val timeUp by viewModel.timeUp.collectAsStateWithLifecycle()
    val watermark by viewModel.watermark.collectAsStateWithLifecycle()
    var expandedExam by rememberSaveable { mutableIntStateOf(-1) }

    val session = LocalExamSession.current
    val sessionState by session.state.collectAsStateWithLifecycle()
    val sessionRunning = sessionState.active && sessionState.examId == examId && !state.locked
    var leaveAlert by remember { mutableStateOf(false) }
    // A session that is not running (handed in, found closed) leaves freely; a running one asks first.
    val requestLeave: () -> Unit = { if (sessionRunning) leaveAlert = true else onBack() }

    EndSessionWhenHandedIn(examId = examId, handedIn = state.locked)
    val penalty = sessionState.penalty?.takeIf { sessionRunning }
    BackHandler(enabled = submit != SubmitUi.Processing) { requestLeave() }

    Column(modifier = modifier.fillMaxSize()) {
        // While the screen is pinned there is no way out to offer, so the arrow is not drawn (legacy `applyLockdownChrome`).
        ExamModeHeader(
            title = if (penalty != null) AkmPenaltyRules.RESUME_TITLE else "Kerjakan Asesmen",
            subtitle = "",
            onBack = if (sessionRunning && (sessionState.strict || penalty != null)) null else requestLeave,
            penaltyLabel = penalty?.let { AkmPenaltyRules.pillLabel(it.remainingMillis) },
        )

        when {
            // Legacy hides the list and "Kumpulkan" while the penalty runs; the dialog above says why.
            penalty != null -> Spacer(modifier = Modifier.weight(1f))
            !state.loaded -> LoadingState(modifier = Modifier.weight(1f))
            state.exams.isEmpty() -> EmptyState(
                title = "Soal belum diunduh",
                description = "Paket soal asesmen ini belum ada di perangkat. Kembali ke detail lalu sinkronkan soal.",
                icon = Icons.Rounded.ReportProblem,
                modifier = Modifier.weight(1f),
                actionLabel = "Kembali",
                onAction = onBack,
            )
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    // Not in the legacy app (doc 06 §4.4 lists it as a gap); shown on purpose, decided 2026-10-16. It only informs.
                    if (watermark.enabled) {
                        InfoBanner(
                            icon = Icons.Rounded.ReportProblem,
                            text = "Jadwal ini ditandai \"${watermark.label}\" oleh sekolah. Kamu tetap dapat mengerjakan dan mengumpulkan jawaban.",
                        )
                    }
                    Text(
                        state.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${state.answered} dari ${state.total} soal terjawab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    state.exams.forEach { exam ->
                        ExamGroupCard(
                            exam = exam,
                            expanded = expandedExam == exam.id,
                            onToggle = { expandedExam = if (expandedExam == exam.id) -1 else exam.id },
                            onOpenInstruction = { instructionId ->
                                if (session.checkpoint("buka_instruksi")) onOpenInstruction(instructionId)
                            },
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        when {
                            state.locked -> "Jawaban sudah dikumpulkan dan tidak dapat diubah lagi."
                            state.allAnswered -> "Semua soal terjawab. Pastikan koneksi stabil saat mengunggah."
                            else -> "Kumpulkan aktif setelah semua soal terjawab."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppButton(
                        text = "Kumpulkan",
                        onClick = { if (session.checkpoint("kumpulkan")) viewModel.submit() },
                        enabled = state.allAnswered && !state.locked && submit != SubmitUi.Processing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (leaveAlert) {
        ExamLeaveDialog(
            action = AkmSessionRules.backAction(strict = sessionState.strict, penaltyOn = viewModel.penaltyApplied),
            onDismiss = { leaveAlert = false },
            onLeave = {
                leaveAlert = false
                session.abort(examId)
                onBack()
            },
        )
    }

    when (val result = submit) {
        SubmitUi.Idle -> Unit
        SubmitUi.Processing -> AppLoadingDialog(message = "Sedang memproses jawaban")
        SubmitUi.Done -> AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Berhasil",
            body = "Jawaban sedang dikumpulkan, silahkan tunggu sampai jawabanmu dinilai",
            primaryButtonText = "Baik",
            onPrimaryClick = {
                viewModel.dismissSubmitResult()
                onSubmitted()
            },
        )
        is SubmitUi.Failed -> AppDialog(
            onDismiss = viewModel::dismissSubmitResult,
            title = "Gagal",
            body = result.message,
            primaryButtonText = "Coba lagi",
            onPrimaryClick = {
                viewModel.dismissSubmitResult()
                viewModel.submit()
            },
            secondaryButtonText = "Tutup",
            onSecondaryClick = viewModel::dismissSubmitResult,
        )
    }

    if (timeUp) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Perhatian",
            body = "Waktu ujian telah berakhir, jawaban akan dikumpulkan",
            primaryButtonText = "OK",
            onPrimaryClick = {
                viewModel.acknowledgeTimeUp()
                onSubmitted()
            },
        )
    }
}

@Composable
private fun ExamGroupCard(
    exam: AkmExam,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenInstruction: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val allInstructionsDone = exam.instructions.isNotEmpty() && exam.instructions.all { it.answered >= it.total }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onToggle)
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (allInstructionsDone) Icons.Rounded.TaskAlt else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (allInstructionsDone) MaterialTheme.colorScheme.primary else scheme.outline,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(
                    exam.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                )
                Text(
                    "${exam.questionCount} soal",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            exam.instructions.forEach { instruction ->
                HorizontalDivider()
                InstructionRow(
                    instruction = instruction,
                    onClick = { onOpenInstruction(instruction.id) },
                )
            }
        }
    }
}

@Composable
private fun InstructionRow(instruction: AkmInstruction, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = when {
        instruction.answered == 0 -> "Belum"
        instruction.answered >= instruction.total -> "Selesai"
        else -> "Proses"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                instruction.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
            Text(
                "${instruction.answered}/${instruction.total} soal terjawab",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        AkmChip(label, accent = label == "Selesai")
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
