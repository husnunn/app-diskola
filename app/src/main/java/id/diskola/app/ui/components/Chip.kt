package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors

/** Filter chip — active (filled primary) / inactive (outlined). */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(36.dp)
            .then(
                if (selected) Modifier.background(scheme.primary, DiskolaExtraShapes.chipBadge)
                else Modifier.border(1.5.dp, scheme.outlineVariant, DiskolaExtraShapes.chipBadge)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xs))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = if (selected) scheme.onPrimary else scheme.onSurface,
        )
    }
}

enum class BadgeTone { Success, Warning, Error, Neutral }

/** Status badge — dot + label, e.g. attendance status (Hadir/Terlambat/Alpa). */
@Composable
fun StatusBadge(label: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.extendedColors
    val scheme = MaterialTheme.colorScheme
    val (container, content, dot) = when (tone) {
        BadgeTone.Success -> Triple(scheme.surfaceContainerLow, extended.success, extended.success)
        BadgeTone.Warning -> Triple(scheme.surfaceContainerLow, extended.warning, extended.warning)
        BadgeTone.Error -> Triple(scheme.errorContainer, scheme.onErrorContainer, scheme.onErrorContainer)
        BadgeTone.Neutral -> Triple(scheme.surfaceContainerLow, scheme.onSurfaceVariant, scheme.onSurfaceVariant)
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .background(container, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).background(dot, CircleShape))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/** Small numeric counter badge, e.g. unread notifications. */
@Composable
fun CounterBadge(count: Int, modifier: Modifier = Modifier, max: Int = 99) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .wrapContentWidth()
            .heightIn(min = 20.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > max) "$max+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}
