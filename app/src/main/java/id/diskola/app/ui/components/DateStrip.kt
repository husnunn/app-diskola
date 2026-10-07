package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_NAME = DateTimeFormatter.ofPattern("EEE", Locale("id"))

/**
 * Horizontal strip of one cell per date (`dd` + short weekday). Unlike the Asesmen schedule strip,
 * no date is disabled — an agenda is browsable in the past and future — and the selected cell is
 * scrolled into view whenever [selected] or [dates] change.
 */
@Composable
fun DateStrip(
    dates: List<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected, dates) {
        val index = dates.indexOf(selected)
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(dates, key = { it.toEpochDay() }) { date ->
            val isSelected = date == selected
            val scheme = MaterialTheme.colorScheme
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .heightIn(min = 64.dp)
                    .then(
                        if (isSelected) Modifier.background(scheme.primary, DiskolaExtraShapes.card)
                        else Modifier
                            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card),
                    )
                    .clickable { onSelect(date) }
                    .padding(vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    date.dayOfMonth.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isSelected) scheme.onPrimary else scheme.onSurface,
                )
                Text(
                    DAY_NAME.format(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}
