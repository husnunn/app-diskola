package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** The keyboard height must stay unchanged this long before a focused field is brought into view. */
private const val KEYBOARD_SETTLE_MS = 150L

/** Extra room kept around the caret line when it is scrolled into view. */
private val CURSOR_MARGIN = 16.dp

/** Give up waiting for a keyboard (hardware keyboard, no IME) after this long and bring the field into view anyway. */
private const val KEYBOARD_WAIT_MS = 1_200L

/** How long after an auto-focus the keyboard is asked to show a second time. */
private const val KEYBOARD_SHOW_RETRY_MS = 200L

/**
 * The design-system text field — empty/focused/error/readonly states, per the DiskolaKit gallery.
 * When [onClick] is set (e.g. a "pick school" field), the field becomes a tap target instead of
 * an editable one — pair it with `readOnly = true`.
 */
@OptIn(FlowPreview::class)
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
    /** More than 1 turns the field into a growing multi-line box (e.g. "Tujuan Pembelajaran"). */
    minLines: Int = 1,
    /**
     * The keyboard's action key. [ImeAction.Done] (also on a multi-line field) shows "Selesai" instead of Enter. [ImeAction.Next] shows
     * "Lanjut" and moves focus to the next field (the platform default, no [onImeAction] needed).
     */
    imeAction: ImeAction = ImeAction.Default,
    /** What the action key does; for [ImeAction.Done] the default closes the keyboard and drops focus. */
    onImeAction: (() -> Unit)? = null,
    /** Keeps the line the cursor is on in view while typing and when the keyboard opens/closes (long multi-line answers). */
    followCursor: Boolean = false,
    /**
     * Takes focus and opens the keyboard once, the first time the field is shown with this set (it may switch from false to true later,
     * e.g. when a loading dialog goes away). Leaving the screen and coming back, or rotating it, does not do it again. Only for the
     * first field of a short form: on a long form the keyboard would cover the content and the bottom bar.
     */
    autoFocus: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var autoFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoFocus) {
        if (!autoFocus || autoFocusDone) return@LaunchedEffect
        // The requester throws while the field is not on screen yet; stay armed so the next try can still succeed.
        if (runCatching { focusRequester.requestFocus() }.isFailure) return@LaunchedEffect
        autoFocusDone = true
        keyboardController?.show()
        // Some keyboards ignore a show() sent while the window is still taking focus.
        delay(KEYBOARD_SHOW_RETRY_MS)
        keyboardController?.show()
    }

    val focused by interactionSource.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val isError = errorText != null

    // The platform only scrolls the caret into view, so a field sitting just above a sticky bottom bar was left half hidden behind
    // it once the keyboard had come up. Wait until the keyboard has stopped moving (a fixed delay fires while it is still sliding,
    // when the field still looks fully visible), then bring the whole field into view.
    val bringIntoView = remember { BringIntoViewRequester() }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(focused) {
        if (focused) {
            withTimeoutOrNull(KEYBOARD_WAIT_MS) { snapshotFlow { ime.getBottom(density) }.debounce(KEYBOARD_SETTLE_MS).first { it > 0 } }
            bringIntoView.bringIntoView()
        }
    }

    // The caret is assumed to sit at the end of the text (the field only sees the string, not the selection): that is where
    // an answer is being typed, and it keeps the last written line visible above the keyboard.
    val focusManager = LocalFocusManager.current
    val cursorInView = remember { BringIntoViewRequester() }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val marginPx = with(density) { CURSOR_MARGIN.toPx() }
    LaunchedEffect(value, focused, followCursor) {
        if (!followCursor || !focused) return@LaunchedEffect
        val layout = textLayout ?: return@LaunchedEffect
        val offset = value.length.coerceIn(0, layout.layoutInput.text.length)
        cursorInView.bringIntoView(layout.getCursorRect(offset).inflate(marginPx))
    }

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
                .bringIntoViewRequester(bringIntoView)
                .heightIn(min = 52.dp)
                .background(containerColor, DiskolaExtraShapes.textField)
                .border(borderWidth, borderColor, DiskolaExtraShapes.textField)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = if (minLines > 1) 14.dp else 0.dp),
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
                        singleLine = minLines <= 1,
                        minLines = minLines,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (readOnly) scheme.onSurfaceVariant else scheme.onSurface,
                        ),
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = imeAction),
                        keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() ?: focusManager.clearFocus() }),
                        onTextLayout = { textLayout = it },
                        cursorBrush = SolidColor(scheme.primary),
                        interactionSource = interactionSource,
                        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(cursorInView).focusRequester(focusRequester),
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

@Preview(showBackground = true)
@Composable
private fun AppTextFieldAutoFocusPreview() {
    DiskolaTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppTextField(value = "", onValueChange = {}, label = "Kata sandi lama", placeholder = "Fokus otomatis", autoFocus = true, imeAction = ImeAction.Next)
            AppTextField(value = "", onValueChange = {}, label = "Kata sandi baru", placeholder = "Lanjut dari keyboard", imeAction = ImeAction.Done)
        }
    }
}
