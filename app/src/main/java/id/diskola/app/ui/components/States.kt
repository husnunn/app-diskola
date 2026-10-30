package id.diskola.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Motion
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.BannerRules
import id.diskola.app.utils.SkeletonRules
import kotlinx.coroutines.delay

/**
 * Skeleton shown while a screen's first load is in flight — the shape of what is coming, with a shimmer passing over it, instead of the old
 * grey rows plus a spinner (two signals; the spinner stays for *actions*). It appears only after [SkeletonRules.SHOW_DELAY_MILLIS], so a fast
 * answer never flashes it, then fades in. TalkBack hears "Memuat" once; the blocks themselves are silent. [rows] is how many rows or cards
 * to draw ([SkeletonVariant.Detail] is a single block). [padding] is the skeleton's inset: pass the content's own padding so its edges
 * line up with the real cards (the default suits a screen without one).
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    rows: Int = 3,
    variant: SkeletonVariant = SkeletonVariant.ListRow,
    padding: PaddingValues = PaddingValues(Spacing.lg),
) {
    var delayPassed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SkeletonRules.SHOW_DELAY_MILLIS)
        delayPassed = true
    }
    val fade by animateFloatAsState(targetValue = if (delayPassed) 1f else 0f, animationSpec = tween(Motion.FAST), label = "skeletonFade")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .graphicsLayer { alpha = fade }
            .semantics(mergeDescendants = true) {
                contentDescription = "Memuat"
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        SkeletonContent(variant, rows)
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

/**
 * Inline error banner: the X clears it at once, otherwise it clears itself after [BannerRules.autoDismissMillis] (stretched when the system
 * asks for more time to read, e.g. with TalkBack) — a new message restarts the clock.
 */
@Composable
fun BannerError(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(message) {
        val base = BannerRules.autoDismissMillis(message)
        delay(accessibility?.calculateRecommendedTimeoutMillis(base, containsIcons = true, containsText = true, containsControls = true) ?: base)
        latestOnDismiss()
    }
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
