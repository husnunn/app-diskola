package id.diskola.app.ui.screens.pembayaran

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.PaymentChannelItem
import id.diskola.app.repository.PaymentChannelGroup
import id.diskola.app.repository.SppBill
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AppDialogTone
import id.diskola.app.utils.PaymentChannelRules
import id.diskola.app.utils.PaymentDates
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.SppRules
import id.diskola.app.viewmodel.PaymentMethodState
import id.diskola.app.viewmodel.SppBillsState
import id.diskola.app.viewmodel.SppDialog
import id.diskola.app.viewmodel.SppEvent
import id.diskola.app.viewmodel.SppHeader
import id.diskola.app.viewmodel.SppPaymentViewModel
import java.time.ZoneId

private val SPP_TABS = listOf("Tagihan", "Sudah Dibayar")

/** Runs [onResume] each time the screen comes back to the foreground (legacy `onResume`/`onStart` reloads). */
@Composable
private fun OnResume(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) onResume() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Bayar SPP — month list (doc 08 §9.3, §9.4, §9.6)
// ---------------------------------------------------------------------------------------------------------------------------------

@Composable
fun SppBillsScreen(
    viewModel: SppPaymentViewModel,
    showPaid: Boolean,
    onBack: () -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenTagihanku: () -> Unit,
) {
    val context = LocalContext.current
    val header by viewModel.header.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(if (showPaid) 1 else 0) }

    OnResume(viewModel::onListResumed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SppEvent.Toast -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                SppEvent.OpenCheckout -> onOpenCheckout()
                SppEvent.OpenTagihanku -> onOpenTagihanku()
                else -> Unit
            }
        }
    }

    SppBillsContent(
        header = header,
        bills = bills,
        tab = tab,
        onTab = { tab = it },
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onToggle = viewModel::toggle,
        onPay = viewModel::onPayClicked,
    )

    if (dialog == SppDialog.PreviousBillPending) {
        AppDialog(
            onDismiss = viewModel::dismissDialog,
            tone = AppDialogTone.WARNING,
            title = SppRules.INQUIRY_TITLE,
            body = SppRules.INQUIRY_MESSAGE,
            primaryButtonText = "Cek Tagihanku",
            onPrimaryClick = viewModel::openTagihanku,
            secondaryButtonText = "Batalkan",
            onSecondaryClick = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun SppBillsContent(
    header: SppHeader,
    bills: SppBillsState,
    tab: Int,
    onTab: (Int) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onPay: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    DetailScaffold(
        title = "Bayar SPP",
        onBack = onBack,
        bottomBar = {
            AnimatedVisibility(visible = tab == 0 && bills.selected.isNotEmpty()) {
                TotalBar(label = SppRules.totalLabel(bills.selected.size), total = bills.total, buttonText = "Bayar Sekarang", onClick = onPay)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StudentCard(header, modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md))
            UnderlineTabRow(
                options = SPP_TABS,
                selectedIndex = tab,
                onSelect = onTab,
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
            )
            PullToRefreshBox(isRefreshing = bills.loading && bills.loaded, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                val items = if (tab == 0) bills.unpaid else bills.paid
                when {
                    !bills.loaded && bills.loading -> LoadingState(rows = 4)
                    items.isEmpty() -> EmptyState(
                        title = SppRules.EMPTY_TITLE,
                        description = if (tab == 0) SppRules.EMPTY_UNPAID else SppRules.EMPTY_PAID,
                        icon = Icons.Rounded.ReceiptLong,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(items, key = { index, bill -> bill.transactionId.ifBlank { "spp-$index" } }) { index, bill ->
                            if (tab == 0) {
                                UnpaidBillRow(bill, checked = index in bills.selected, onCheckedChange = { onToggle(index, it) })
                            } else {
                                PaidBillRow(bill, paidLabel = PaymentDates.display(bill.paidAt, zone))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCard(header: SppHeader, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.primary, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            header.name.ifBlank { "-" },
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOf(header.nisn, header.className).filter { it.isNotBlank() }.joinToString(" | "),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onPrimary.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(color = scheme.onPrimary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
            Text("Total Saldo", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimary, modifier = Modifier.weight(1f).padding(start = Spacing.sm))
            Text(
                header.balance?.let(PaymentRules::formatRupiah) ?: "Memuat…",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimary,
            )
        }
    }
}

@Composable
private fun UnpaidBillRow(bill: SppBill, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DiskolaExtraShapes.card)
            .background(if (checked) scheme.secondaryContainer else scheme.surfaceContainerLowest)
            .border(1.dp, if (checked) scheme.primary else scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(bill.name.ifBlank { "SPP" }, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            StatusPill("Segera Lunasi", scheme.errorContainer, scheme.onErrorContainer)
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = Spacing.sm)) {
            Text("Total", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(PaymentRules.formatRupiah(bill.price), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        }
    }
}

@Composable
private fun PaidBillRow(bill: SppBill, paidLabel: String) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bill.name.ifBlank { "SPP" }, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
            StatusPill("Lunas", extended.successContainer, extended.onSuccessContainer)
        }
        Row {
            Text("Total", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(PaymentRules.formatRupiah(bill.price), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        }
        Text("Terbayar: $paidLabel", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
internal fun StatusPill(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = content,
        modifier = Modifier.background(container, CircleShape).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}

/** The bar fixed at the bottom: label + total on the left, the action on the right. */
@Composable
private fun TotalBar(label: String, total: Long, buttonText: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(PaymentRules.formatRupiah(total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            AppButton(text = buttonText, onClick = onClick)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------
// "Rincian Pembayaran" (doc 08 §9.5, legacy CheckoutPage)
// ---------------------------------------------------------------------------------------------------------------------------------

@Composable
fun SppCheckoutScreen(viewModel: SppPaymentViewModel, onBack: () -> Unit, onPay: () -> Unit) {
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    // The picks are cleared when the list reloads; without any there is nothing to show here.
    LaunchedEffect(bills.selected.isEmpty()) { if (bills.selected.isEmpty()) onBack() }
    SppCheckoutContent(months = bills.selectedBills, total = bills.total, onBack = onBack, onPay = onPay)
}

@Composable
private fun SppCheckoutContent(months: List<SppBill>, total: Long, onBack: () -> Unit, onPay: () -> Unit) {
    DetailScaffold(
        title = "Rincian Pembayaran",
        onBack = onBack,
        bottomBar = { TotalBar(label = SppRules.totalLabel(months.size), total = total, buttonText = "Bayar Sekarang", onClick = onPay) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ScreenHorizontalPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DetailRow("Jenis pembayaran", "SPP")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                months.forEach { DetailRow(it.name.ifBlank { "SPP" }, PaymentRules.formatRupiah(it.price)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow(SppRules.totalLabel(months.size), PaymentRules.formatRupiah(total), emphasize = true)
            }
        }
    }
}

@Composable
internal fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------
// "Pilih Metode Pembayaran" (doc 08 §2.4, legacy PaymentTypePage)
// ---------------------------------------------------------------------------------------------------------------------------------

@Composable
fun SppPaymentMethodScreen(
    viewModel: SppPaymentViewModel,
    onBack: () -> Unit,
    onAskPin: (channelName: String) -> Unit,
    onPaidFromWallet: (amountLabel: String, months: List<String>) -> Unit,
    onInvoiceCreated: (trxId: String, partnerReff2: String) -> Unit,
) {
    val header by viewModel.header.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val methods by viewModel.methods.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val paying by viewModel.paying.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadMethods() }
    OnResume(viewModel::onMethodScreenResumed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SppEvent.AskPin -> onAskPin(event.channelName)
                is SppEvent.PaidFromWallet -> onPaidFromWallet(event.amountLabel, event.months)
                is SppEvent.InvoiceCreated -> onInvoiceCreated(event.trxId, event.partnerReff2)
                else -> Unit
            }
        }
    }

    PaymentMethodContent(
        methods = methods,
        amount = bills.total,
        balance = header.balance,
        onBack = onBack,
        onRefresh = viewModel::loadMethods,
        onToggleCategory = viewModel::toggleCategory,
        onChoose = viewModel::choose,
    )

    if (paying) AppLoadingDialog(message = "Memproses pembayaran, mohon tunggu")
    when (val shown = dialog) {
        is SppDialog.AmountWarning -> AppDialog(
            onDismiss = viewModel::dismissDialog,
            tone = AppDialogTone.WARNING,
            title = PaymentChannelRules.WARNING_TITLE,
            body = shown.message,
            primaryButtonText = "Oke",
            onPrimaryClick = viewModel::dismissDialog,
        )
        is SppDialog.PayFailed -> AppDialog(
            onDismiss = viewModel::dismissDialog,
            tone = AppDialogTone.ERROR,
            title = "Pembayaran Gagal",
            body = shown.message,
            primaryButtonText = "Mengerti",
            onPrimaryClick = viewModel::dismissDialog,
        )
        is SppDialog.MethodsFailed -> AppDialog(
            onDismiss = {
                viewModel.dismissDialog()
                onBack()
            },
            dismissible = false,
            tone = AppDialogTone.ERROR,
            title = "Gagal memuat metode pembayaran",
            body = shown.message,
            primaryButtonText = "Kembali",
            onPrimaryClick = {
                viewModel.dismissDialog()
                onBack()
            },
        )
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodContent(
    methods: PaymentMethodState,
    amount: Long,
    balance: Long?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleCategory: (String) -> Unit,
    onChoose: (PaymentChannelItem, Boolean) -> Unit,
) {
    DetailScaffold(title = "Pilih Metode Pembayaran", onBack = onBack) { padding ->
        PullToRefreshBox(
            isRefreshing = methods.loading && methods.groups.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (methods.groups.isEmpty() && methods.loading) {
                LoadingState(rows = 5)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "amount") { AmountSummary(amount) }
                    methods.groups.forEach { group ->
                        item(key = "group-${group.key}") {
                            if (group.key == PaymentChannelRules.SALDO_KEY) {
                                WalletRow(group, amount, balance, onChoose)
                            } else {
                                CategoryCard(group, expanded = methods.expanded == group.key, onToggle = { onToggleCategory(group.key) }, onChoose = { onChoose(it, false) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountSummary(amount: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Total Pembayaran", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
        Text(PaymentRules.formatRupiah(amount), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/**
 * The wallet ("saldo") row: not an accordion; it pays with the wallet channel (`channel_id == 1`) when the balance covers the amount,
 * otherwise it is greyed with "Saldo tidak cukup". Legacy also drew a "Top Up Saldo" button here that did nothing; it is left out.
 */
@Composable
private fun WalletRow(group: PaymentChannelGroup, amount: Long, balance: Long?, onChoose: (PaymentChannelItem, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val channel = PaymentChannelRules.walletChannel(group.channels)
    val enough = balance != null && !PaymentChannelRules.needTopUp(balance, amount)
    val enabled = enough && channel != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(DiskolaExtraShapes.card)
            .background(scheme.surfaceContainerLowest)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(enabled = enabled) { channel?.let { onChoose(it, true) } }
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(Icons.Rounded.AccountBalanceWallet, enabled)
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
            Text("Saldo", style = MaterialTheme.typography.titleSmall, color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant)
            Text(
                balance?.let(PaymentRules::formatRupiah) ?: "Memuat saldo…",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (balance != null && !enough) {
                Text(PaymentChannelRules.SALDO_NOT_ENOUGH, style = MaterialTheme.typography.labelMedium, color = scheme.error)
            }
        }
    }
}

@Composable
private fun CategoryCard(group: PaymentChannelGroup, expanded: Boolean, onToggle: () -> Unit, onChoose: (PaymentChannelItem) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val category = PaymentChannelRules.categoryOf(group.key)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DiskolaExtraShapes.card)
            .background(scheme.surfaceContainerLowest)
            .border(1.dp, if (expanded) scheme.primary else scheme.outlineVariant, DiskolaExtraShapes.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onToggle)
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(category.icon.vector(), enabled = true)
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text(category.info, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Tutup" else "Buka", modifier = Modifier.rotate(rotation))
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                group.channels.forEach { channel ->
                    HorizontalDivider(color = scheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onChoose(channel) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 32.dp)
                                .background(scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = channel.image_url.ifBlank { null },
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.padding(Spacing.xs).fillMaxSize(),
                            )
                        }
                        Text(
                            channel.channel_method_name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f).padding(start = Spacing.md),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryIcon(icon: ImageVector, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (enabled) scheme.secondaryContainer else scheme.surfaceContainerHigh, DiskolaExtraShapes.iconBox),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) scheme.onSecondaryContainer else scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

private fun PaymentChannelRules.Icon.vector(): ImageVector = when (this) {
    PaymentChannelRules.Icon.Online -> Icons.Rounded.CreditCard
    PaymentChannelRules.Icon.Merchant -> Icons.Rounded.Storefront
    PaymentChannelRules.Icon.Atm -> Icons.Rounded.AccountBalance
    PaymentChannelRules.Icon.Saldo -> Icons.Rounded.AccountBalanceWallet
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------------------------------------------

private val previewBills = listOf(
    SppBill("A", "SPP Juli 2026", "2026-07-01", "", 150_000, false),
    SppBill("B", "SPP Agustus 2026 — termasuk biaya praktikum laboratorium komputer", "2026-08-01", "", 175_000, false),
    SppBill("C", "SPP September 2026", "2026-09-01", "", 150_000, false),
)

@Preview(name = "SPP list — light", showBackground = true, heightDp = 720)
@Composable
private fun SppBillsPreview() {
    DiskolaTheme {
        SppBillsContent(
            header = SppHeader("Rizky Ramadhan Saputra", "20202601", "X RPL 1", 1_250_000),
            bills = SppBillsState(unpaid = previewBills, selected = setOf(0, 1), loaded = true),
            tab = 0, onTab = {}, onBack = {}, onRefresh = {}, onToggle = { _, _ -> }, onPay = {},
        )
    }
}

@Preview(name = "SPP paid — dark", showBackground = true, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SppPaidDarkPreview() {
    DiskolaTheme {
        SppBillsContent(
            header = SppHeader("Rizky", "20202601", "X RPL 1", null),
            bills = SppBillsState(paid = listOf(SppBill("P", "SPP Juni 2026", "2026-06-01", "2026-06-10T03:00:00.000Z", 150_000, false)), loaded = true),
            tab = 1, onTab = {}, onBack = {}, onRefresh = {}, onToggle = { _, _ -> }, onPay = {},
        )
    }
}

@Preview(name = "Methods — light", showBackground = true, heightDp = 720)
@Composable
private fun PaymentMethodPreview() {
    DiskolaTheme {
        PaymentMethodContent(
            methods = PaymentMethodState(
                groups = listOf(
                    PaymentChannelGroup("saldo", listOf(PaymentChannelItem(channel_method_id = 1, channel_method_name = "SALDO", channel_id = 1))),
                    PaymentChannelGroup("virtual account", listOf(PaymentChannelItem(channel_method_name = "BANK BRI"), PaymentChannelItem(channel_method_name = "BANK MANDIRI"))),
                    PaymentChannelGroup("retail", listOf(PaymentChannelItem(channel_method_name = "ALFAMART"))),
                ),
                expanded = "virtual account",
            ),
            amount = 325_000, balance = 100_000, onBack = {}, onRefresh = {}, onToggleCategory = {}, onChoose = { _, _ -> },
        )
    }
}

@Preview(name = "Checkout — dark", showBackground = true, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SppCheckoutPreview() {
    DiskolaTheme { SppCheckoutContent(months = previewBills.take(2), total = 325_000, onBack = {}, onPay = {}) }
}
