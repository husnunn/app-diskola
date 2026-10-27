package id.diskola.app.ui.screens.akm

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.AkmSessionRules.Priming
import id.diskola.app.viewmodel.AkmPrimingViewModel

/**
 * "Sebelum Memulai Ujian" (doc 06 §5.4): the last stop before the session. It lists what the system records on its own, and — for a
 * strict exam — says the screen will be pinned. Texts are legacy's, word for word. Back returns to the detail; "Mulai Ujian" opens the
 * session ([onStart] does that), so nothing is recorded by merely looking at this screen.
 */
@Composable
fun AkmPrimingScreen(
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmPrimingViewModel = hiltViewModel(),
) {
    val watermark by viewModel.watermark.collectAsStateWithLifecycle()
    AkmPrimingContent(
        strict = viewModel.strict,
        watermark = watermark,
        onBack = onBack,
        onStart = onStart,
        modifier = modifier,
    )
}

@Composable
internal fun AkmPrimingContent(
    strict: Boolean,
    watermark: AkmRules.AkmWatermark,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailScaffold(
        title = Priming.TITLE,
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(text = Priming.START, onClick = onStart, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (watermark.enabled) {
                InfoBanner(icon = Icons.Rounded.ReportProblem, text = Priming.watermarkNotice(watermark.label))
            }

            PrimingGroup(
                heading = Priming.VIOLATIONS_HEADING,
                rows = Priming.VIOLATIONS,
                icon = Icons.Rounded.Warning,
                tint = MaterialTheme.colorScheme.error,
            )

            PrimingGroup(
                heading = Priming.NEUTRAL_HEADING,
                rows = Priming.NEUTRAL,
                icon = Icons.Rounded.Info,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                footnote = Priming.NEUTRAL_FOOTNOTE,
            )

            Text(
                Priming.SEND_NOTICE,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (strict) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, DiskolaExtraShapes.card)
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        Priming.LOCKDOWN_MESSAGE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimingGroup(
    heading: String,
    rows: List<String>,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    footnote: String? = null,
) {
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
            heading,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = scheme.onSurface,
        )
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Text(
                    row,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }
        }
        if (footnote != null) {
            Text(
                footnote,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AkmPrimingStrictPreview() {
    DiskolaTheme {
        AkmPrimingContent(strict = true, watermark = AkmRules.AkmWatermark.None, onBack = {}, onStart = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AkmPrimingCancelledDarkPreview() {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        AkmPrimingContent(
            strict = false,
            watermark = AkmRules.AkmWatermark(enabled = true, label = "DIBATALKAN"),
            onBack = {},
            onStart = {},
        )
    }
}
