package id.diskola.app.ui.screens.pembayaran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.mock.TransactionDirection
import id.diskola.app.dataclass.mock.TransactionItem
import id.diskola.app.dataclass.mock.formatRupiah
import id.diskola.app.ui.components.AdaptiveTileGrid
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.WalletBalanceHeader
import id.diskola.app.ui.components.iconFor
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.PembayaranViewModel

@Composable
fun PembayaranScreen(
    onTopUp: () -> Unit,
    onTransfer: () -> Unit,
    onQr: () -> Unit,
    onRiwayat: () -> Unit,
    onSpp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PembayaranViewModel = hiltViewModel(),
) {
    val saldo by viewModel.saldo.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WalletBalanceHeader(
            saldoLabel = formatRupiah(saldo),
            walletId = viewModel.walletId,
            onCopyWalletId = {},
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
        ) {
            Box(modifier = Modifier.offset(y = -HeroOverlap)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    AppCard(variant = CardVariant.Elevated, contentPadding = PaddingValues(vertical = Spacing.md)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            QuickAction("add_card", "Top Up", onTopUp, Modifier.weight(1f))
                            QuickAction("swap_horiz", "Transfer", onTransfer, Modifier.weight(1f))
                            QuickAction("qr_code_scanner", "QR Pay", onQr, Modifier.weight(1f))
                            QuickAction("history", "Riwayat", onRiwayat, Modifier.weight(1f))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp))
                            .clickable(onClick = onSpp)
                            .padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(iconFor("receipt_long"), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                            Text("Tagihanku", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("5 tagihan menunggu · 2 lewat jatuh tempo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Text("Pembayaran Sekolah", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    AdaptiveTileGrid(items = schoolPaymentTiles) { tile, tileModifier ->
                        SchoolPaymentTile(tile = tile, onClick = onSpp, modifier = tileModifier)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Transaksi Terakhir", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("Lihat semua", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onRiwayat))
                    }
                    AppCard(variant = CardVariant.Elevated, contentPadding = PaddingValues(0.dp)) {
                        Column {
                            viewModel.recentTransactions.forEachIndexed { index, tx ->
                                TransactionRow(tx)
                                if (index != viewModel.recentTransactions.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(Spacing.huge))
                }
            }
        }
    }
}

private data class SchoolPaymentTileData(val icon: String, val label: String)
private val schoolPaymentTiles = listOf(
    SchoolPaymentTileData("receipt_long", "SPP"),
    SchoolPaymentTileData("bolt", "PPOB"),
    SchoolPaymentTileData("volunteer_activism", "Partisipasi"),
    SchoolPaymentTileData("shopping_bag", "Toko"),
    SchoolPaymentTileData("local_library", "Perpus"),
    SchoolPaymentTileData("more_horiz", "Lainnya"),
)

@Composable
private fun SchoolPaymentTile(tile: SchoolPaymentTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(iconFor(tile.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(tile.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun QuickAction(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(iconFor(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TransactionRow(tx: TransactionItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(tx.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val isOut = tx.direction == TransactionDirection.OUT
        Text(
            (if (isOut) "− " else "+ ") + formatRupiah(tx.amount),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
