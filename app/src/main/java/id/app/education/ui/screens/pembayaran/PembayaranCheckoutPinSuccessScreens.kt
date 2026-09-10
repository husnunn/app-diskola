package id.app.education.ui.screens.pembayaran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.dataclass.mock.MockPembayaran
import id.app.education.dataclass.mock.formatRupiah
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.CardVariant
import id.app.education.ui.components.DetailScaffold
import id.app.education.ui.components.NumericKeypad
import id.app.education.ui.components.PinDots
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.extendedColors
import id.app.education.viewmodel.PembayaranViewModel

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onPay: (amountLabel: String) -> Unit,
    viewModel: PembayaranViewModel = hiltViewModel(),
) {
    val saldo by viewModel.saldo.collectAsStateWithLifecycle()
    val bills = remember { viewModel.selectedBills() }
    val subtotal = bills.sumOf { it.amount }
    val fee = if (bills.isNotEmpty()) MockPembayaran.CHECKOUT_ADMIN_FEE else 0L
    val total = subtotal + fee
    var useSaldo by remember { mutableStateOf(true) }

    DetailScaffold(
        title = "Konfirmasi Pembayaran",
        onBack = onBack,
        bottomBar = {
            id.app.education.ui.components.SelectionSummaryBar(
                summaryText = "Total bayar",
                buttonText = "Bayar ${formatRupiah(total)}",
                onClick = { onPay(formatRupiah(total)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("RINCIAN TAGIHAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppCard {
                Column {
                    bills.forEach { bill ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                            Text(bill.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text(formatRupiah(bill.amount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Biaya layanan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(formatRupiah(fee), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total bayar", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text(formatRupiah(total), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Text("METODE PEMBAYARAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppCard(variant = if (useSaldo) CardVariant.Outlined else CardVariant.Filled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = useSaldo, onClick = { useSaldo = true })
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Saldo Klaspay", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Tersedia ${formatRupiah(saldo)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            AppCard(variant = if (!useSaldo) CardVariant.Outlined else CardVariant.Filled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !useSaldo, onClick = { useSaldo = false })
                    Column {
                        Text("Virtual Account", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text("BNI · BRI · Mandiri · Permata", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            AppCard(variant = CardVariant.Filled) {
                Text(
                    "Pembayaran yang sudah diproses tidak dapat dibatalkan. Pastikan tagihan dan nominal sudah benar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.size(Spacing.huge))
        }
    }
}

@Composable
fun PinScreen(
    amountLabel: String,
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(pin) {
        if (pin.length == 6) onConfirmed()
    }

    DetailScaffold(title = "Masukkan PIN", onBack = onBack, useCloseIcon = true) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.size(Spacing.lg))
            Text(
                "Masukkan 6 digit PIN Klaspay untuk menyelesaikan pembayaran $amountLabel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(Spacing.xxl))
            PinDots(length = pin.length)
            Spacer(modifier = Modifier.size(Spacing.sm))
            Text("Lupa PIN?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.size(Spacing.xxl))
            NumericKeypad(
                onDigit = { digit -> if (pin.length < 6) pin += digit },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            )
        }
    }
}

@Composable
fun PaymentSuccessScreen(amountLabel: String, onDownload: () -> Unit, onBackToPembayaran: () -> Unit) {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(extended.brandGradientDark, Color(0xFF0E8377), extended.brandGradientLight),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.size(Spacing.huge))
        Box(
            modifier = Modifier.size(88.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.size(Spacing.lg))
        Text("Pembayaran Berhasil", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
        Spacer(modifier = Modifier.size(Spacing.sm))
        Text(amountLabel, style = MaterialTheme.typography.displaySmall, color = Color.White)
        Spacer(modifier = Modifier.size(Spacing.sm))
        Text("8 Sep 2026 · 10.24 · ID TRX 20260908A7741", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
        Spacer(modifier = Modifier.size(Spacing.xl))
        AppCard {
            Column {
                Text("RINCIAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(Spacing.sm))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Total", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(amountLabel, style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Metode", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("Saldo Klaspay", style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Saldo akhir", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(formatRupiah(247_500), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.size(Spacing.xl))
        AppButton(text = "Unduh Bukti Bayar", onClick = onDownload, variant = id.app.education.ui.components.ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.size(Spacing.sm))
        AppButton(text = "Kembali ke Pembayaran", onClick = onBackToPembayaran, modifier = Modifier.fillMaxWidth())
    }
}
