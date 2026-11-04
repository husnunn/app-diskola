package id.diskola.app.ui.screens.magang

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.MagangRules
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))

/** The month the date strip shows; tapping it opens the month and year picker (legacy had no arrows). */
@Composable
internal fun MonthLabel(month: YearMonth, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = ScreenHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            MONTH_LABEL.format(month),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = scheme.onSurface,
        )
        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Pilih bulan dan tahun", tint = scheme.onSurfaceVariant)
    }
}

/**
 * One schedule day: company, date, hours, then either the buttons the server allows ("Laporan Harian", "Masuk") or its status text.
 * Both buttons are stacked when both flags are set; legacy drew them on top of each other.
 */
@Composable
internal fun ScheduleCard(
    item: MagangScheduleItem,
    onMasuk: () -> Unit,
    onLaporan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val time = item.schedule
    val actions = MagangRules.actions(item.attendance)
    val status = MagangRules.statusText(item.attendance)
    AppCard(modifier = modifier, variant = CardVariant.Elevated) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(scheme.surfaceContainerLow, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Business, contentDescription = null, tint = scheme.primary)
                }
                Text(
                    MagangRules.companyName(item),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = Spacing.md),
                )
                Text(
                    MagangRules.dateLabel(time?.date),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(scheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
            HorizontalDivider(color = scheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pukul", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    MagangRules.timeRange(time?.start_time.orEmpty(), time?.end_time.orEmpty()),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
            }
            status?.let { StatusBadge(it, BadgeTone.Neutral) }
            actions.forEach { action ->
                when (action) {
                    MagangRules.CardAction.LaporanHarian ->
                        AppButton(text = "Laporan Harian", onClick = onLaporan, variant = ButtonVariant.Tonal, modifier = Modifier.fillMaxWidth())
                    MagangRules.CardAction.Masuk ->
                        AppButton(text = "Masuk", onClick = onMasuk, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
