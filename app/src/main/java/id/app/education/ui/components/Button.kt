package id.app.education.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Motion

enum class ButtonVariant { Filled, Tonal, Outlined, Text }

/**
 * The design-system button — 4 [ButtonVariant]s x enabled/pressed/disabled states, per the
 * DiskolaKit component gallery. Shared by both the Auth flow and (eventually) non-auth screens.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) Motion.PRESS_SCALE else 1f,
        label = "buttonPressScale",
    )
    val isEnabled = enabled && !loading
    val height = if (variant == ButtonVariant.Text) 48.dp else 52.dp

    val colors = buttonColors(variant, isEnabled, pressed)

    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { if (!isEnabled) disabled() },
        shape = DiskolaExtraShapes.button,
        color = colors.container,
        contentColor = colors.content,
        border = colors.border,
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = colors.content,
                )
            } else if (leadingIcon != null) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    leadingIcon()
                    Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
                }
            } else {
                Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
            }
        }
    }
}

private data class ButtonColors(
    val container: Color,
    val content: Color,
    val border: BorderStroke?,
)

@Composable
private fun buttonColors(variant: ButtonVariant, enabled: Boolean, pressed: Boolean): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        ButtonVariant.Filled -> when {
            !enabled -> ButtonColors(scheme.surfaceContainerHighest, scheme.onSurfaceVariant.copy(alpha = 0.55f), null)
            pressed -> ButtonColors(scheme.primaryContainer, scheme.onPrimaryContainer, null)
            else -> ButtonColors(scheme.primary, scheme.onPrimary, null)
        }
        ButtonVariant.Tonal -> when {
            !enabled -> ButtonColors(scheme.surfaceContainerHighest, scheme.onSurfaceVariant.copy(alpha = 0.55f), null)
            pressed -> ButtonColors(scheme.surfaceContainerHigh, scheme.onSecondaryContainer, null)
            else -> ButtonColors(scheme.secondaryContainer, scheme.onSecondaryContainer, null)
        }
        ButtonVariant.Outlined -> when {
            !enabled -> ButtonColors(Color.Transparent, scheme.onSurfaceVariant.copy(alpha = 0.55f), BorderStroke(1.5.dp, scheme.outlineVariant))
            pressed -> ButtonColors(scheme.surfaceContainerLow, scheme.primary, BorderStroke(1.5.dp, scheme.primary))
            else -> ButtonColors(Color.Transparent, scheme.primary, BorderStroke(1.5.dp, scheme.outline))
        }
        ButtonVariant.Text -> when {
            !enabled -> ButtonColors(Color.Transparent, scheme.onSurfaceVariant.copy(alpha = 0.55f), null)
            pressed -> ButtonColors(scheme.surfaceContainerLow, scheme.primary, null)
            else -> ButtonColors(Color.Transparent, scheme.primary, null)
        }
    }
}
