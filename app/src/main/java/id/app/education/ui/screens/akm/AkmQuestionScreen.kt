package id.app.education.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.dataclass.mock.MockAkm
import id.app.education.dataclass.mock.QuestionType
import id.app.education.ui.components.AnswerOption
import id.app.education.ui.components.AppBottomSheet
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppTextField
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.ExamModeHeader
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.extendedColors
import id.app.education.viewmodel.AkmAnswer
import id.app.education.viewmodel.AkmViewModel

private val OPTION_LETTERS = listOf("A", "B", "C", "D", "E", "F")

/** The question player. Handles all four question types the assessment format defines. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AkmQuestionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val index by viewModel.questionIndex.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val penaltyActive by viewModel.penaltyActive.collectAsStateWithLifecycle()
    var gridVisible by rememberSaveable { mutableStateOf(false) }

    val questions = viewModel.questions
    val question = questions[index]
    val answer = answers[question.id]
    val answeredCount = questions.count { answers.containsKey(it.id) }

    Column(modifier = modifier.fillMaxSize()) {
        ExamModeHeader(
            title = "Soal ${index + 1} dari ${questions.size}",
            subtitle = "",
            onBack = onBack,
            penaltyLabel = MockAkm.PENALTY_LABEL.takeIf { penaltyActive },
            trailing = {
                Row(
                    modifier = Modifier
                        .clickable { gridVisible = true }
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = "Pilih soal",
                        tint = MaterialTheme.extendedColors.onExamSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AkmChip(question.type.label)

            Text(
                question.text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            when (question.type) {
                QuestionType.PilihanGanda, QuestionType.BenarSalah -> {
                    val selected = (answer as? AkmAnswer.Choice)?.index
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        question.options.forEachIndexed { optionIndex, option ->
                            AnswerOption(
                                label = option,
                                selected = selected == optionIndex,
                                multiSelect = false,
                                letter = OPTION_LETTERS.getOrNull(optionIndex),
                                onClick = { viewModel.selectChoice(question.id, optionIndex) },
                            )
                        }
                    }
                }

                QuestionType.MultiJawaban -> {
                    val selected = (answer as? AkmAnswer.MultiChoice)?.indices.orEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        question.options.forEachIndexed { optionIndex, option ->
                            AnswerOption(
                                label = option,
                                selected = optionIndex in selected,
                                multiSelect = true,
                                onClick = { viewModel.toggleMultiChoice(question.id, optionIndex) },
                            )
                        }
                    }
                }

                QuestionType.EsaiSingkat -> {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        AppTextField(
                            value = (answer as? AkmAnswer.Essay)?.text.orEmpty(),
                            onValueChange = { viewModel.setEssay(question.id, it) },
                            label = "Jawaban singkat",
                            placeholder = "Satu kata, tanpa spasi",
                        )
                        Text(
                            "Spasi otomatis dihapus — esai singkat tidak boleh mengandung spasi.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PermMedia, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Slot media soal · gambar / audio / video",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppButton(
                text = "Sebelumnya",
                onClick = viewModel::previousQuestion,
                variant = ButtonVariant.Outlined,
                enabled = index > 0,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = "Berikutnya",
                onClick = viewModel::nextQuestion,
                enabled = index < questions.lastIndex,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (gridVisible) {
        AppBottomSheet(title = "Pilih Soal", onDismiss = { gridVisible = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    "$answeredCount dari ${questions.size} soal terjawab",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.height(((questions.size + 4) / 5 * 60).dp),
                ) {
                    itemsIndexed(questions) { questionIndex, item ->
                        QuestionGridCell(
                            number = questionIndex + 1,
                            current = questionIndex == index,
                            answered = answers.containsKey(item.id),
                            onClick = {
                                viewModel.goToQuestion(questionIndex)
                                gridVisible = false
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    GridLegend("Soal aktif", MaterialTheme.colorScheme.primary)
                    GridLegend("Sudah dijawab", MaterialTheme.colorScheme.secondaryContainer)
                    GridLegend("Belum dijawab", MaterialTheme.colorScheme.surfaceContainerLowest)
                }
            }
        }
    }
}

@Composable
private fun QuestionGridCell(number: Int, current: Boolean, answered: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        current -> scheme.primary
        answered -> scheme.secondaryContainer
        else -> scheme.surfaceContainerLowest
    }
    val foreground = when {
        current -> scheme.onPrimary
        answered -> scheme.onSecondaryContainer
        else -> scheme.onSurface
    }
    Box(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .background(background, DiskolaExtraShapes.card)
            .border(1.dp, if (current) scheme.primary else scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = foreground,
        )
    }
}

@Composable
private fun GridLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}
