package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import id.diskola.app.ui.components.ExamModeHeader
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AkmViewModel

private const val PENALTY_LABEL = "00:24"

/**
 * Exam lobby: the instruction tree the student works through. There is deliberately no back
 * affordance here — leaving mid-exam is what triggers a penalty.
 */
@Composable
fun AkmResumeScreen(
    akmId: Int,
    onOpenInstruction: (Int) -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val item = items.find { it.id == akmId } ?: return
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val penaltyActive by viewModel.penaltyActive.collectAsStateWithLifecycle()
    val timeUpEvent by viewModel.timeUpEvent.collectAsStateWithLifecycle()
    // Not read directly — collecting it forces recomposition once `observeSyncState` finishes
    // hydrating the ViewModel's schedule cache, even if nothing else on this screen changes at
    // that moment (this was the actual cause of "soal tidak muncul, cuma instruksi/perintah saja").
    @Suppress("UNUSED_VARIABLE") val rawSchedulesVersion by viewModel.rawSchedulesVersion.collectAsStateWithLifecycle()
    var expandedExam by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(akmId) {
        viewModel.observeSyncState(akmId)
        viewModel.watchDeadline(akmId)
    }

    // Recomputed on every `answers`/`rawSchedulesVersion` change since it's read straight off the
    // ViewModel's cache.
    val exams = viewModel.examsFor(akmId)
    val allDone = viewModel.allAnswered(akmId)
    val totalQuestions = exams.sumOf { it.instructions.sumOf { i -> i.total } }
    val totalAnswered = exams.sumOf { it.instructions.sumOf { i -> i.answered } }

    Column(modifier = modifier.fillMaxSize()) {
        ExamModeHeader(
            title = "Mode ujian aktif",
            subtitle = "Layar dikunci · keluar aplikasi dikenai sanksi",
            penaltyLabel = PENALTY_LABEL.takeIf { penaltyActive },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "$totalAnswered dari $totalQuestions soal terjawab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            exams.forEach { exam ->
                ExamGroupCard(
                    exam = exam,
                    expanded = expandedExam == exam.id,
                    onToggle = { expandedExam = if (expandedExam == exam.id) -1 else exam.id },
                    onOpenInstruction = { instructionId ->
                        viewModel.openInstruction(akmId, instructionId)
                        onOpenInstruction(instructionId)
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
                if (allDone) {
                    "Semua instruksi selesai. Pastikan koneksi stabil saat mengunggah."
                } else {
                    "Kumpulkan aktif setelah semua instruksi terjawab penuh."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppButton(
                text = "Kumpulkan",
                onClick = {
                    viewModel.submitExam(item.id)
                    onSubmitted()
                },
                enabled = allDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (timeUpEvent == akmId) {
        AppDialog(
            onDismiss = {},
            title = "Waktu Habis",
            body = "Waktu habis, Asesmen telah dikumpulkan.",
            primaryButtonText = "Tutup",
            onPrimaryClick = {
                viewModel.consumeTimeUpEvent()
                onSubmitted()
            },
            dismissible = false,
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
