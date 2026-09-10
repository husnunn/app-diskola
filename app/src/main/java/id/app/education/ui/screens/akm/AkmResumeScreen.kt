package id.app.education.ui.screens.akm

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
import id.app.education.dataclass.mock.AkmExam
import id.app.education.dataclass.mock.AkmInstruction
import id.app.education.dataclass.mock.MockAkm
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.ExamModeHeader
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.AkmViewModel

/**
 * Exam lobby: the instruction tree the student works through. There is deliberately no back
 * affordance here — leaving mid-exam is what triggers a penalty.
 */
@Composable
fun AkmResumeScreen(
    akmId: Int,
    onOpenQuestions: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val item = viewModel.items.find { it.id == akmId } ?: viewModel.items.first()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val penaltyActive by viewModel.penaltyActive.collectAsStateWithLifecycle()
    var expandedExam by rememberSaveable { mutableIntStateOf(1) }

    val liveAnswered = viewModel.questions.count { answers.containsKey(it.id) }
    val liveTotal = viewModel.questions.size
    val allDone = liveAnswered == liveTotal

    Column(modifier = modifier.fillMaxSize()) {
        ExamModeHeader(
            title = "Mode ujian aktif",
            subtitle = "Layar dikunci · keluar aplikasi dikenai sanksi",
            penaltyLabel = MockAkm.PENALTY_LABEL.takeIf { penaltyActive },
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
                "$liveAnswered dari $liveTotal soal terjawab pada instruksi aktif",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            viewModel.exams.forEach { exam ->
                ExamGroupCard(
                    exam = exam,
                    expanded = expandedExam == exam.id,
                    onToggle = { expandedExam = if (expandedExam == exam.id) -1 else exam.id },
                    liveAnswered = liveAnswered,
                    onOpenInstruction = onOpenQuestions,
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
}

@Composable
private fun ExamGroupCard(
    exam: AkmExam,
    expanded: Boolean,
    onToggle: () -> Unit,
    liveAnswered: Int,
    onOpenInstruction: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val allInstructionsDone = exam.instructions.all { instruction ->
        (instruction.answered ?: liveAnswered) >= instruction.total
    }

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
                    answered = instruction.answered ?: liveAnswered,
                    onClick = onOpenInstruction,
                )
            }
        }
    }
}

@Composable
private fun InstructionRow(instruction: AkmInstruction, answered: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = when {
        answered == 0 -> "Belum"
        answered >= instruction.total -> "Selesai"
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
                "$answered/${instruction.total} soal terjawab",
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
