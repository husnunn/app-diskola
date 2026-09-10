package id.app.education.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.app.education.dataclass.mock.AkmExplanation
import id.app.education.dataclass.mock.MockAkm
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.DetailScaffold
import id.app.education.ui.components.ProgressTrack
import id.app.education.ui.components.ScoreRing
import id.app.education.ui.components.SectionLabel
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.AkmViewModel

/** Scored result for one assessment, broken down per subtest. */
@Composable
fun AkmScoreScreen(
    scoreId: Int,
    onBack: () -> Unit,
    onOpenExplanation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val score = viewModel.scores.find { it.id == scoreId } ?: viewModel.scores.first()
    val scheme = MaterialTheme.colorScheme

    DetailScaffold(title = "Hasil Asesmen", onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ScoreRing(score = score.score)
                Text(
                    score.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                )
                Text(
                    "Dikerjakan ${score.dateLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionLabel("SKOR PER SUBTES")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                        .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    MockAkm.subtestScores.forEach { subtest ->
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    subtest.label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    subtest.score.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                    color = scheme.onSurface,
                                )
                            }
                            ProgressTrack(fraction = subtest.score / 100f)
                        }
                    }
                }
            }

            if (score.hasExplanation) {
                AppButton(
                    text = "Unduh & Lihat Pembahasan",
                    onClick = onOpenExplanation,
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Per-question review, showing the student's answer against the key. */
@Composable
fun AkmExplainScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailScaffold(title = "Pembahasan", onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MockAkm.explanations.forEach { explanation ->
                ExplanationCard(explanation)
            }
        }
    }
}

@Composable
private fun ExplanationCard(explanation: AkmExplanation) {
    val scheme = MaterialTheme.colorScheme
    val container = if (explanation.correct) scheme.secondaryContainer else scheme.errorContainer
    val onContainer = if (explanation.correct) scheme.onSecondaryContainer else scheme.onErrorContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(
                modifier = Modifier.size(26.dp).background(container, DiskolaExtraShapes.chipBadge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    explanation.number.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = onContainer,
                )
            }
            AkmChip(
                label = if (explanation.correct) "Benar" else "Salah",
                icon = if (explanation.correct) Icons.Rounded.Check else Icons.Rounded.Close,
                accent = explanation.correct,
                error = !explanation.correct,
            )
        }

        Text(
            explanation.question,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )

        AnswerRow("Jawaban kamu", explanation.yourAnswer, highlightError = !explanation.correct)
        AnswerRow("Kunci", explanation.key, highlightError = false)

        HorizontalDivider()

        Text(
            explanation.rationale,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )

        if (explanation.hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(scheme.surfaceContainerLow, DiskolaExtraShapes.card),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = scheme.onSurfaceVariant)
                    Text(
                        "Gambar pembahasan · tap untuk layar penuh",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(label: String, value: String, highlightError: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (highlightError) scheme.error else scheme.onSurface,
        )
    }
}
