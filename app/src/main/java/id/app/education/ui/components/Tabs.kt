package id.app.education.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Spacing

/** Filled pill tab row — e.g. Notifikasi's "Semua"/"Belum dibaca". */
@Composable
fun PillTabRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val scheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .then(
                        if (selected) Modifier.background(scheme.primary, DiskolaExtraShapes.chipBadge)
                        else Modifier.background(scheme.surfaceContainerLowest, DiskolaExtraShapes.chipBadge)
                            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.chipBadge)
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimary else scheme.onSurface,
                )
            }
        }
    }
}

/** Underline tab row — e.g. SPP's "Belum Lunas"/"Sudah Lunas". */
@Composable
fun UnderlineTabRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Row {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(vertical = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                    )
                }
            }
        }
        Row {
            options.forEachIndexed { index, _ ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (selected) scheme.primary else scheme.outlineVariant),
                )
            }
        }
    }
}
