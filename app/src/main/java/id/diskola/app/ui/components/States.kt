package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/** Skeleton rows shown while a screen's first load is in flight. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(rows) { index ->
            val opacity = if (index == 0) 1f else 0.7f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = opacity),
                            DiskolaExtraShapes.iconBox,
                        ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = opacity), DiskolaExtraShapes.chipBadge),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = opacity), DiskolaExtraShapes.chipBadge),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
            Text("Memuat…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Empty-state placeholder with exactly one recovery action, per the README's rule. */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.WifiOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateScaffold(
        modifier = modifier,
        icon = icon,
        iconBackground = MaterialTheme.colorScheme.surfaceContainerLow,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        description = description,
        actionLabel = actionLabel,
        onAction = onAction,
        actionVariant = ButtonVariant.Outlined,
    )
}

/** Error-state placeholder — same shell as [EmptyState], filled retry action. */
@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    title: String = "Terjadi kesalahan",
    description: String = "Periksa koneksi internet Anda dan coba lagi.",
    icon: ImageVector = Icons.Rounded.WifiOff,
    onRetry: (() -> Unit)? = null,
) {
    StateScaffold(
        modifier = modifier,
        icon = icon,
        iconBackground = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.onErrorContainer,
        title = title,
        description = description,
        actionLabel = if (onRetry != null) "Coba Lagi" else null,
        onAction = onRetry,
        actionVariant = ButtonVariant.Filled,
    )
}

@Composable
private fun StateScaffold(
    modifier: Modifier,
    icon: ImageVector,
    iconBackground: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    actionVariant: ButtonVariant,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(30.dp))
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xs))
            AppButton(text = actionLabel, onClick = onAction, variant = actionVariant, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}

/** Dismissible inline error banner. */
@Composable
fun BannerError(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, DiskolaExtraShapes.card)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
