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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.mock.TransferContact
import id.diskola.app.dataclass.mock.formatRupiah
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.QrViewfinder
import id.diskola.app.ui.components.SelectionSummaryBar
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.PembayaranViewModel

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: PembayaranViewModel = hiltViewModel(),
) {
    val saldo by viewModel.saldo.collectAsStateWithLifecycle()
    val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()
    val amount by viewModel.transferAmount.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf("") }
    var destinationQuery by remember { mutableStateOf("") }

    DetailScaffold(
        title = "Transfer Saldo",
        onBack = onBack,
        bottomBar = {
            SelectionSummaryBar(
                summaryText = if (selectedContact != null) "Kirim ke ${selectedContact!!.name}" else "Pilih tujuan transfer",
                buttonText = "Lanjut",
                enabled = selectedContact != null && amount >= 5_000,
                onClick = onContinue,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AppTextField(value = destinationQuery, onValueChange = { destinationQuery = it }, label = "Tujuan transfer", placeholder = "Cari nama atau ID wallet")

            Text("Nominal transfer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatRupiah(amount), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Tersedia ${formatRupiah(saldo)} · minimal Rp 5.000 · tanpa biaya antar siswa.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Simple stand-in amount stepper since there's no numeric entry widget for underline fields yet.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(10_000L, 25_000L, 50_000L, 100_000L).forEach { preset ->
                    id.diskola.app.ui.components.AppButton(
                        text = formatRupiah(preset),
                        onClick = { viewModel.setTransferAmount(preset) },
                        variant = id.diskola.app.ui.components.ButtonVariant.Outlined,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            AppTextField(value = note, onValueChange = { note = it }, label = "Catatan (opsional)", placeholder = "mis. bayar kantin")

            Text("SERING DITRANSFER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                items(viewModel.transferContacts, key = { it.id }) { contact ->
                    ContactChip(contact = contact, selected = contact.id == selectedContact?.id, onClick = { viewModel.selectContact(contact) })
                }
            }
            Spacer(modifier = Modifier.size(Spacing.huge))
        }
    }
}

@Composable
private fun ContactChip(contact: TransferContact, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                .border(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(contact.initials, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.size(Spacing.xs))
        Text(contact.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(contact.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun QrScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1513))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
            }
            Text("QR Pay", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = Color.White) }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            QrViewfinder()
            Spacer(modifier = Modifier.size(Spacing.lg))
            Text(
                "Arahkan kamera ke QRIS kantin, koperasi, atau unit usaha sekolah.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = Spacing.xxl),
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.xl)) {
            Text("Bayar dengan Saldo Klaspay", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.size(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                id.diskola.app.ui.components.AppButton(
                    text = "Dari Galeri",
                    onClick = {},
                    variant = id.diskola.app.ui.components.ButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
                id.diskola.app.ui.components.AppButton(
                    text = "QR Saya",
                    onClick = {},
                    variant = id.diskola.app.ui.components.ButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
