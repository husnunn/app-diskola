package id.diskola.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors

private val PickerMaxWidth = 400.dp

/**
 * The date picker in the app's dialog dress (same card colour, corner and buttons as [AppDialog]) instead of Material's text-button dialog.
 * The calendar itself is Material's: it needs the full 360 dp, so the window keeps the platform width rules of `DatePickerDialog`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    state: DatePickerState,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Batal",
) {
    val container = MaterialTheme.extendedColors.dialogSurface
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = DiskolaExtraShapes.dialog,
        colors = DatePickerDefaults.colors(containerColor = container),
        confirmButton = {
            AppButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = state.selectedDateMillis != null,
                modifier = Modifier.padding(end = Spacing.sm, bottom = Spacing.sm),
            )
        },
        dismissButton = {
            AppButton(
                text = dismissText,
                onClick = onDismiss,
                variant = ButtonVariant.Tonal,
                modifier = Modifier.padding(end = Spacing.sm, bottom = Spacing.sm),
            )
        },
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(containerColor = container),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = Spacing.xxl, end = Spacing.xxl, top = Spacing.xxl).semantics { heading() },
                )
            },
        )
    }
}

/** A 24-hour dial in the app's dialog dress: title, [TimePicker], then "Batal" (tonal) and the confirm button side by side. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    state: TimePickerState,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Batal",
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.padding(horizontal = Spacing.lg), contentAlignment = Alignment.Center) {
            TimePickerCard(state, title, confirmText, onConfirm, onDismiss, dismissText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerCard(
    state: TimePickerState,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String,
) {
    val container = MaterialTheme.extendedColors.dialogSurface
    Surface(
        modifier = Modifier.widthIn(max = PickerMaxWidth).fillMaxWidth(),
        shape = DiskolaExtraShapes.dialog,
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
    ) {
        // Landscape phones are shorter than the dial: the card scrolls rather than cutting the buttons off.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl).semantics { heading() },
            )
            TimePicker(state = state, colors = TimePickerDefaults.colors(containerColor = container))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppButton(text = dismissText, onClick = onDismiss, variant = ButtonVariant.Tonal, modifier = Modifier.weight(1f))
                AppButton(text = confirmText, onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Time picker — light", showBackground = true)
@Composable
private fun TimePickerLightPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            TimePickerCard(rememberTimePickerState(13, 20, true), "Batas pengumpulan", "Simpan", {}, {}, "Batal")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Time picker — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TimePickerDarkPreview() {
    DiskolaTheme {
        Box(Modifier.padding(Spacing.lg)) {
            TimePickerCard(rememberTimePickerState(7, 5, true), "Jadwal Pemanggilan", "Simpan", {}, {}, "Batal")
        }
    }
}
