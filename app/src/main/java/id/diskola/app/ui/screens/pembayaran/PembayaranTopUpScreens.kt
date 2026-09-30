package id.diskola.app.ui.screens.pembayaran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.mock.MockPembayaran
import id.diskola.app.dataclass.mock.formatRupiah
import id.diskola.app.ui.components.AdaptiveTileGrid
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.PembayaranViewModel
import id.diskola.app.viewmodel.TopUpMethod

@Composable
fun TopUpScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: PembayaranViewModel = hiltViewModel(),
) {
    val saldo by viewModel.saldo.collectAsStateWithLifecycle()
    val amount by viewModel.topUpAmount.collectAsStateWithLifecycle()
    val method by viewModel.topUpMethod.collectAsStateWithLifecycle()
    val fee = if (method == TopUpMethod.VIRTUAL_ACCOUNT) MockPembayaran.TOPUP_VA_FEE else 0L

    DetailScaffold(
        title = "Top Up Saldo",
        onBack = onBack,
        bottomBar = {
            id.diskola.app.ui.components.SelectionSummaryBar(
                summaryText = "Total bayar · ${formatRupiah(amount + fee)}",
                buttonText = "Lanjut",
                enabled = amount >= 10_000,
                onClick = onContinue,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("Saldo saat ini", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatRupiah(saldo), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            Text("NOMINAL TOP UP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatRupiah(amount),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Minimal Rp 10.000 · maksimal Rp 5.000.000 per transaksi.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AdaptiveTileGrid(items = MockPembayaran.topUpPresets) { preset, tileModifier ->
                val selected = preset == amount
                Box(
                    modifier = tileModifier
                        .height(48.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                            RoundedCornerShape(14.dp),
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable { viewModel.setTopUpAmount(preset) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        formatRupiah(preset),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text("METODE TOP UP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MethodOption(
                title = "Virtual Account",
                subtitle = "BNI · BRI · Mandiri · Permata · biaya Rp 1.500",
                selected = method == TopUpMethod.VIRTUAL_ACCOUNT,
                onSelect = { viewModel.setTopUpMethod(TopUpMethod.VIRTUAL_ACCOUNT) },
            )
            MethodOption(
                title = "Agen Klaspay",
                subtitle = "Setor tunai di koperasi atau bendahara sekolah",
                selected = method == TopUpMethod.AGEN_KLASPAY,
                onSelect = { viewModel.setTopUpMethod(TopUpMethod.AGEN_KLASPAY) },
            )
            Spacer(modifier = Modifier.size(Spacing.huge))
        }
    }
}

@Composable
private fun MethodOption(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    AppCard(variant = if (selected) CardVariant.Outlined else CardVariant.Filled, modifier = Modifier.clickable(onClick = onSelect)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.padding(start = Spacing.sm)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TopUpVaScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val vaNumber = "8808 7788 1234 0051"

    DetailScaffold(title = "Instruksi Pembayaran", onBack = onBack, useCloseIcon = true) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AppCard(variant = CardVariant.Filled) {
                Text(
                    "Selesaikan dalam 23 jam 58 menit. Lewat batas, kode VA otomatis kedaluwarsa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppCard {
                Column {
                    Text("Virtual Account BNI", style = MaterialTheme.typography.titleSmall)
                    Text("a.n. SMK DEMO · Klaspay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.size(Spacing.md))
                    Text("Nomor Virtual Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vaNumber, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text("Jumlah transfer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(101_500), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Transfer tepat sampai angka terakhir agar saldo otomatis masuk.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AppCard(variant = CardVariant.Filled) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Cara Pembayaran", style = MaterialTheme.typography.titleSmall)
                    listOf(
                        "Buka aplikasi mobile banking, pilih menu Transfer → Virtual Account.",
                        "Masukkan nomor VA di atas, lalu periksa nama penerima.",
                        "Masukkan jumlah transfer, konfirmasi, dan simpan bukti bayar.",
                        "Saldo Klaspay bertambah otomatis dalam 1–5 menit.",
                    ).forEachIndexed { index, step ->
                        Row {
                            Text("${index + 1}. ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(step, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            AppButton(
                text = "Salin Nomor VA",
                onClick = { clipboard.setText(AnnotatedString(vaNumber)) },
                variant = ButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(text = "Cek Status di Riwayat", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
