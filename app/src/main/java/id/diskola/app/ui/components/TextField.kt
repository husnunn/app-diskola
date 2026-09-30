package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing

/**
 * The design-system text field — empty/focused/error/readonly states, per the DiskolaKit gallery.
 * When [onClick] is set (e.g. a "pick school" field), the field becomes a tap target instead of
 * an editable one — pair it with `readOnly = true`.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    errorText: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    // KeyboardOptions' own default (None) — kept explicit since the guest-confirmation phrase
    // field (`GuestConfirmDialog`) relies on this staying off; the legacy app's XML EditText did
    // auto-capitalize there (`textCapSentences`), which fought its own case-sensitive match.
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val isError = errorText != null

    val borderColor = when {
        isError -> scheme.error
        readOnly -> Color.Transparent
        focused -> scheme.primary
        else -> scheme.outlineVariant
    }
    val borderWidth = if (focused || isError) 2.dp else 1.5.dp
    val containerColor = if (readOnly) scheme.surfaceContainer else Color.Transparent
    val labelColor = when {
        isError -> scheme.error
        focused -> scheme.primary
        else -> scheme.onSurfaceVariant
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(containerColor, DiskolaExtraShapes.textField)
                .border(borderWidth, borderColor, DiskolaExtraShapes.textField)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.outline,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled && !readOnly && onClick == null,
                        readOnly = readOnly,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (readOnly) scheme.onSurfaceVariant else scheme.onSurface,
                        ),
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
                        cursorBrush = SolidColor(scheme.primary),
                        interactionSource = interactionSource,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                trailing?.invoke()
            }
        }
        if (isError) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = scheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = errorText,
                    style = TextStyle(fontSize = 11.sp),
                    color = scheme.error,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTextFieldPreview() {
    DiskolaTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AppTextField(value = "", onValueChange = {}, label = "NISN / NIS / NIK", placeholder = "Contoh: 0051234567")
        }
    }
}
