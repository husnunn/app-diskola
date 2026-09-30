package id.diskola.app.ui.screens.pembayaran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.mock.TransactionDirection
import id.diskola.app.dataclass.mock.TransactionItem
import id.diskola.app.dataclass.mock.formatRupiah
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.BillRow
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.PaidBillRow
import id.diskola.app.ui.components.PillTabRow
import id.diskola.app.ui.components.SelectionSummaryBar
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.PembayaranViewModel

@Composable
fun RiwayatScreen(onBack: () -> Unit, viewModel: PembayaranViewModel = hiltViewModel()) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val filtered = when (tabIndex) {
        1 -> viewModel.history.filter { it.direction == TransactionDirection.IN }
        2 -> viewModel.history.filter { it.direction == TransactionDirection.OUT }
        else -> viewModel.history
    }

    DetailScaffold(title = "Riwayat Transaksi", onBack = onBack) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                PillTabRow(options = listOf("Semua", "Masuk", "Keluar"), selectedIndex = tabIndex, onSelect = { tabIndex = it })
                Spacer(modifier = Modifier.size(Spacing.md))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Masuk bulan ini", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatRupiah(viewModel.monthIncome), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keluar bulan ini", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatRupiah(viewModel.monthExpense), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.lg)) {
                item {
                    Text("JUNI 2026", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Spacing.sm))
                }
                items(filtered, key = { it.id }) { tx ->
                    HistoryRow(tx)
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Memuat transaksi Mei 2026…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.sm))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(tx: TransactionItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text("${tx.subtitle} · ${tx.dateLabel}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val isOut = tx.direction == TransactionDirection.OUT
        Text(
            (if (isOut) "− " else "+ ") + formatRupiah(tx.amount),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun SppScreen(
    onBack: () -> Unit,
    onBayar: () -> Unit,
    viewModel: PembayaranViewModel = hiltViewModel(),
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val unpaid by viewModel.unpaidBills.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedBillIds.collectAsStateWithLifecycle()
    val total = unpaid.filter { it.id in selectedIds }.sumOf { it.amount }

    DetailScaffold(
        title = "Tagihan SPP",
        onBack = onBack,
        bottomBar = {
            if (tabIndex == 0) {
                SelectionSummaryBar(
                    summaryText = "${selectedIds.size} tagihan dipilih · ${formatRupiah(total)}",
                    buttonText = "Bayar",
                    enabled = selectedIds.isNotEmpty(),
                    onClick = onBayar,
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UnderlineTabRow(options = listOf("Belum Lunas", "Sudah Lunas"), selectedIndex = tabIndex, onSelect = { tabIndex = it })
            if (tabIndex == 0) {
                Text(
                    "Pilih tagihan yang ingin dibayar. Beberapa tagihan bisa dibayar sekaligus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
                LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(unpaid, key = { it.id }) { bill ->
                        BillRow(bill = bill, checked = bill.id in selectedIds, onCheckedChange = { viewModel.toggleBillSelection(bill.id) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(viewModel.paidBills, key = { it.id }) { bill -> PaidBillRow(bill) }
                    item {
                        Text(
                            "Bukti bayar dapat diunduh dari detail transaksi.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
