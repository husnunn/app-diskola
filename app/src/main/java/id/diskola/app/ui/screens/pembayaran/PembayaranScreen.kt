package id.diskola.app.ui.screens.pembayaran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.repository.WalletInfo
import id.diskola.app.ui.components.ActivationGate
import id.diskola.app.ui.components.AdaptiveTileGrid
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.WalletBalanceHeader
import id.diskola.app.ui.components.WalletHeaderState
import id.diskola.app.ui.components.iconFor
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.PaymentRules.PaymentDestination
import id.diskola.app.viewmodel.PaymentHubViewModel

/**
 * Pembayaran tab (legacy `PembayaranPage`): wallet balance, the shortcuts, the Tagihanku card, the school menu and the PPOB grid.
 *
 * Everything that needs a wallet asks [gate] to start activation while the wallet is inactive. A destination that is not in
 * [available] yet answers with "Fitur dalam pengembangan" — payment screens are rebuilt one sub-phase at a time and none of them
 * may show dummy data to a real account in the meantime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PembayaranScreen(
    gate: ActivationGate,
    available: Set<PaymentDestination>,
    onNavigate: (PaymentDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentHubViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var infoSheet by rememberSaveable { mutableStateOf(false) }
    var comingSoon by rememberSaveable { mutableStateOf(false) }
    var libraryDialog by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // The tab is visible again — also right after activation, which flips the session flag the hub reads.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun open(destination: PaymentDestination) {
        when {
            !state.walletActive -> gate.prompt()
            destination in available -> onNavigate(destination)
            else -> comingSoon = true
        }
    }

    val headerState = when {
        !state.walletActive -> WalletHeaderState.Inactive
        state.wallet != null -> WalletHeaderState.Ready(PaymentRules.formatRupiah(state.wallet!!.balance))
        state.loadFailed -> WalletHeaderState.Failed
        else -> WalletHeaderState.Loading
    }

    PullToRefreshBox(isRefreshing = state.loading && state.walletActive, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            WalletBalanceHeader(
                state = headerState,
                onInfo = { if (state.walletActive) infoSheet = true else gate.prompt() },
                onActivate = gate.start,
                onRetry = viewModel::refresh,
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding)) {
                Box(modifier = Modifier.offset(y = -HeroOverlap)) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        AppCard(variant = CardVariant.Elevated, contentPadding = PaddingValues(vertical = Spacing.md)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                QuickAction("add_card", "Top Up", { open(PaymentDestination.TopUp) }, Modifier.weight(1f))
                                QuickAction("qr_code_scanner", "QR Pay", { open(PaymentDestination.QrPay) }, Modifier.weight(1f))
                                QuickAction("history", "Riwayat", { open(PaymentDestination.Riwayat) }, Modifier.weight(1f))
                            }
                        }

                        TagihankuCard(onClick = { open(PaymentDestination.Tagihanku) })

                        AdaptiveTileGrid(items = PaymentRules.hubMenu(state.isStudent)) { item, tileModifier ->
                            MenuTile(
                                icon = item.icon(),
                                label = item.label,
                                modifier = tileModifier,
                                onClick = {
                                    val destination = item.destination
                                    if (destination == null) libraryDialog = true else open(destination)
                                },
                            )
                        }

                        Text("Pembelian", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        AdaptiveTileGrid(items = PaymentRules.ppobItems(state.isTeacher)) { item, tileModifier ->
                            MenuTile(icon = item.icon(), label = item.label, modifier = tileModifier, onClick = { open(item.destination) })
                        }
                        Spacer(modifier = Modifier.size(Spacing.huge))
                    }
                }
            }
        }
    }

    if (infoSheet) state.wallet?.let { WalletInfoSheet(it, onDismiss = { infoSheet = false }) }
    if (comingSoon) {
        AppDialog(
            onDismiss = { comingSoon = false },
            title = "Fitur dalam pengembangan",
            body = "Menu ini sedang dalam proses pembangunan, ditunggu updatenya yah...",
            primaryButtonText = "Ok Deh",
            onPrimaryClick = { comingSoon = false },
        )
    }
    if (libraryDialog) {
        AppDialog(
            onDismiss = { libraryDialog = false },
            title = "Fitur belum tersedia",
            body = "Fitur ini belum tersedia untuk sekolah ini",
            primaryButtonText = "Ok Deh",
            onPrimaryClick = { libraryDialog = false },
        )
    }
}

private fun PaymentRules.HubMenuItem.icon(): ImageVector = when (this) {
    PaymentRules.HubMenuItem.BayarSpp -> Icons.Rounded.ReceiptLong
    PaymentRules.HubMenuItem.DanaPartisipasi -> Icons.Rounded.VolunteerActivism
    PaymentRules.HubMenuItem.PinjamBuku -> Icons.Rounded.LocalLibrary
}

private fun PaymentRules.PpobItem.icon(): ImageVector = when (this) {
    PaymentRules.PpobItem.Pulsa -> Icons.Rounded.PhoneAndroid
    PaymentRules.PpobItem.Listrik -> Icons.Rounded.Bolt
    PaymentRules.PpobItem.Air -> Icons.Rounded.WaterDrop
    PaymentRules.PpobItem.Internet -> Icons.Rounded.Wifi
    PaymentRules.PpobItem.Game -> Icons.Rounded.SportsEsports
    PaymentRules.PpobItem.Bpjs -> Icons.Rounded.HealthAndSafety
}

@Composable
private fun QuickAction(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 48.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(iconFor(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TagihankuCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(
            "Tagihanku",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f).padding(start = Spacing.md),
        )
        Text("Lihat Semua", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun MenuTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The legacy "ID Wallet" top sheet: the wallet's name and the id the user copies for transfers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletInfoSheet(wallet: WalletInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AppBottomSheet(title = "ID Wallet", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (wallet.name.isNotBlank()) {
                Text(wallet.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ID Wallet", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        wallet.userId.ifBlank { "-" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    enabled = wallet.userId.isNotBlank(),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ID Wallet", wallet.userId))
                        Toast.makeText(context, "ID Wallet tersalin", Toast.LENGTH_SHORT).show()
                    },
                ) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin ID Wallet") }
            }
            AppButton(text = "Tutup", onClick = onDismiss, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
        }
    }
}
