package id.diskola.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.Spacing

/**
 * Grid of equal-width tiles that derives its column count from the width actually available, so a
 * tile stays around [minTileWidth] on a phone and on a tablet instead of stretching to fill.
 *
 * Measures its own height, unlike `LazyVerticalGrid` — which is what lets it sit inside a
 * `verticalScroll` parent without the caller hardcoding a row-pitch calculation that silently
 * breaks whenever the column count or item count changes.
 */
@Composable
fun <T> AdaptiveTileGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    minTileWidth: Dp = 104.dp,
    maxColumns: Int = 6,
    spacing: Dp = Spacing.sm,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = ((maxWidth + spacing) / (minTileWidth + spacing))
            .toInt()
            .coerceIn(1, maxColumns)

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    rowItems.forEach { item -> itemContent(item, Modifier.weight(1f)) }
                    // Keeps a short last row's tiles the same width as every other row's.
                    repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
