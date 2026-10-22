package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/**
 * The design-system dialog (replaces the legacy `prettyAlert`). Pass `dismissible = false` for
 * non-cancelable dialogs like the mandatory-update prompt.
 */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    title: String,
    body: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    dismissible: Boolean = true,
    primaryLoading: Boolean = false,
    primaryEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
        ),
    ) {
        // A long title/body (a server message, say) scrolls inside the dialog instead of pushing the buttons off screen.
        val maxContentHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog)
                .padding(Spacing.xxl),
        ) {
            Column(
                modifier = Modifier.heightIn(max = maxContentHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    content?.invoke()
                }
                AppButton(
                    text = primaryButtonText,
                    onClick = onPrimaryClick,
                    loading = primaryLoading,
                    enabled = primaryEnabled,
                    variant = ButtonVariant.Filled,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (secondaryButtonText != null && onSecondaryClick != null) {
                    AppButton(
                        text = secondaryButtonText,
                        onClick = onSecondaryClick,
                        variant = ButtonVariant.Text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
