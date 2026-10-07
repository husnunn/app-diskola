package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Month + year chooser (year within ±30 of today, as the legacy `MonthYearPickerDialog`). */
@Composable
fun MonthYearPickerDialog(
    initial: YearMonth,
    onPick: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val thisYear = remember { LocalDate.now().year }
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.month) }
    val scheme = MaterialTheme.colorScheme

    AppDialog(
        onDismiss = onDismiss,
        title = "Pilih Bulan",
        body = "",
        primaryButtonText = "Pilih",
        onPrimaryClick = { onPick(YearMonth.of(year, month)) },
        secondaryButtonText = "Batal",
        onSecondaryClick = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (year > thisYear - 30) year-- }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Tahun sebelumnya")
                }
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { if (year < thisYear + 30) year++ }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Tahun berikutnya")
                }
            }
            Month.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { m ->
                        val selected = m == month
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .then(
                                    if (selected) Modifier.background(scheme.primary, DiskolaExtraShapes.card)
                                    else Modifier
                                        .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                                        .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card),
                                )
                                .clickable { month = m }
                                .padding(Spacing.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                m.getDisplayName(TextStyle.SHORT, Locale("id")),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) scheme.onPrimary else scheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
