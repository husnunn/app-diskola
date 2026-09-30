package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.mock.BillItem
import id.diskola.app.dataclass.mock.formatRupiah
import id.diskola.app.ui.theme.Spacing

/** Selectable bill row for the SPP "Belum Lunas" checklist. */
@Composable
fun BillRow(
    bill: BillItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(bill.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(formatRupiah(bill.amount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Jatuh tempo ${bill.dueDateLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (bill.overdue) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.sm))
                        StatusBadge(label = "LEWAT JATUH TEMPO", tone = BadgeTone.Error)
                    }
                }
            }
        }
    }
}

/** Read-only paid bill row for the SPP "Sudah Lunas" list. */
@Composable
fun PaidBillRow(bill: BillItem, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(bill.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Dibayar ${bill.paidOnLabel.orEmpty()} · ${bill.paidMethodLabel.orEmpty()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatRupiah(bill.amount), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Sticky bottom bar showing selection count + total, used by the SPP checklist and Top Up. */
@Composable
fun SelectionSummaryBar(
    summaryText: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(Spacing.xl),
    ) {
        Text(summaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.sm))
        AppButton(text = buttonText, onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth())
    }
}
