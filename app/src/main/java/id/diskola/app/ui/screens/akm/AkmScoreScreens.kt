package id.diskola.app.ui.screens.akm

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import id.diskola.app.dataclass.ResponData.AkmExplanationData
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.ProgressTrack
import id.diskola.app.ui.components.ScoreRing
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AkmViewModel

/** Scored result for one assessment, broken down per subtest. */
@Composable
fun AkmScoreScreen(
    scoreId: Int,
    onBack: () -> Unit,
    onOpenExplanation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val scores by viewModel.scores.collectAsStateWithLifecycle()
    val score = scores.find { it.id == scoreId }
    val scheme = MaterialTheme.colorScheme

    DetailScaffold(title = "Hasil Asesmen", onBack = onBack, modifier = modifier) { padding ->
        if (score == null) {
            LoadingState(modifier = Modifier.padding(padding))
            return@DetailScaffold
        }
        val subtests = viewModel.subtestScoresFor(scoreId)

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

            if (subtests.isNotEmpty()) {
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
                        subtests.forEach { subtest ->
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

/**
 * One card per question, each with its own `file_path` — confirmed real shape from
 * `docs/repo lama/asesmen/asesmen-teknis-nilai-pembahasan.md` §5.3 (`GET
 * .../exam-schedules-scored/{id}/explains?gov_schedule=0`). There is no combined document for the
 * whole assessment; every question's explanation is a separate asset (usually a PDF/image URL).
 */
@Composable
fun AkmExplainScreen(
    scoreId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val loading by viewModel.explanationLoading.collectAsStateWithLifecycle()
    val error by viewModel.explanationError.collectAsStateWithLifecycle()
    val explanations by viewModel.explanations.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(scoreId) { viewModel.fetchExplanation(scoreId) }

    DetailScaffold(title = "Pembahasan", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> LoadingState()
                error != null -> ErrorState(
                    description = error ?: "Gagal memuat pembahasan.",
                    onRetry = { viewModel.fetchExplanation(scoreId) },
                )
                explanations.isEmpty() -> EmptyState(
                    title = "Pembahasan belum tersedia",
                    description = "Server tidak mengembalikan data pembahasan untuk asesmen ini.",
                    icon = Icons.Rounded.QuestionAnswer,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = ScreenHorizontalPadding,
                        end = ScreenHorizontalPadding,
                        top = Spacing.lg,
                        bottom = Spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(explanations) { index, explanation ->
                        ExplanationCard(
                            number = index + 1,
                            explanation = explanation,
                            onOpen = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(explanation.file_path)))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(number: Int, explanation: AkmExplanationData, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            "Soal $number",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = scheme.primary,
        )
        Text(
            explanation.question.question.ifBlank { "(Teks soal tidak tersedia)" },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
        )
        if (explanation.file_path.isNotBlank()) {
            AppButton(
                text = "Lihat Pembahasan",
                onClick = onOpen,
                variant = ButtonVariant.Outlined,
                leadingIcon = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                "Pembahasan untuk soal ini belum tersedia.",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
