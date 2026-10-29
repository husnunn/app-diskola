package id.diskola.app.ui.screens.pembayaran

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import id.diskola.app.dataclass.ResponData.PaymentInvoiceTable
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AppDialogTone
import id.diskola.app.utils.PaymentDates
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.PaymentStatusRules
import id.diskola.app.viewmodel.PaymentDetailViewModel
import id.diskola.app.viewmodel.PaymentGuideViewModel
import java.time.ZoneId
import kotlinx.coroutines.launch

private fun copy(context: Context, text: String, toast: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Diskola", text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) { content() }
}

/** A labelled value with a "SALIN" action (number and total of the guide). */
@Composable
private fun CopyRow(label: String, value: String, onCopy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value.ifBlank { "-" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCopy, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("SALIN", modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------
// "Selesaikan Pembayaran" (doc 08 §2.5, legacy PaymentGuidePage)
// ---------------------------------------------------------------------------------------------------------------------------------

@Composable
fun PaymentGuideScreen(
    trxId: String,
    partnerReff2: String,
    onBack: () -> Unit,
    onOpenDetail: (trxId: String, paymentMethod: String) -> Unit,
    viewModel: PaymentGuideViewModel,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(trxId) { viewModel.start(trxId) }

    DetailScaffold(title = "Selesaikan Pembayaran", onBack = onBack) { padding ->
        val invoice = state.invoice
        if (invoice != null) {
            PaymentGuideContent(
                invoice = invoice,
                partnerReff2 = partnerReff2.ifBlank { invoice.partner_reff2 },
                sections = state.sections,
                guideLoading = state.guideLoading,
                onCopyNumber = { copy(context, invoice.payment_code, "Nomor ${invoice.type} tersalin") },
                // Decision Q3: the total shown is the total copied (legacy copied the amount without the admin fee).
                onCopyTotal = { copy(context, invoice.total_amount.toString(), "Total pembayaran tersalin") },
                onOpenDetail = { onOpenDetail(invoice.trx_id, invoice.channel_method_name) },
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (state.loading) AppLoadingDialog(message = "Menampilkan data transaksi...")
    if (state.missing) {
        AppDialog(
            onDismiss = onBack,
            dismissible = false,
            title = "Perhatian",
            body = "Data transaksi tidak ditemukan",
            primaryButtonText = "Kembali",
            onPrimaryClick = onBack,
        )
    }
}

@Composable
private fun PaymentGuideContent(
    invoice: PaymentInvoiceTable,
    partnerReff2: String,
    sections: Map<String, List<String>>,
    guideLoading: Boolean,
    onCopyNumber: () -> Unit,
    onCopyTotal: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val scheme = MaterialTheme.colorScheme
    val deadline = PaymentDates.parseDeadline(invoice.expired_at, zone)
    var guideOpen by rememberSaveable { mutableStateOf(false) }
    var openSection by rememberSaveable { mutableStateOf<String?>(null) }
    val arrow by animateFloatAsState(if (guideOpen) 180f else 0f, label = "guideArrow")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Static deadline, no countdown (legacy shows none either).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.extendedColors.warningContainer, DiskolaExtraShapes.card)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("Batas waktu pembayaran:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.extendedColors.onWarningContainer)
            Text(
                deadline?.let { PaymentDates.guideDate(it, zone) } ?: invoice.expired_at.ifBlank { "-" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.extendedColors.onWarningContainer,
                textAlign = TextAlign.Center,
            )
            deadline?.let {
                Text(PaymentDates.guideTime(it, zone), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.extendedColors.onWarningContainer)
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(width = 64.dp, height = 40.dp).background(scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = invoice.image_url.ifBlank { null },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(Spacing.xs).fillMaxSize(),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                    Text(invoice.channel_method_name.ifBlank { "-" }, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                    Text("Partner Reff: $partnerReff2", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = scheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("ID Transaksi:", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Text(invoice.trx_id, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
            CopyRow(PaymentStatusRules.numberLabel(invoice.channel_method_name), invoice.payment_code, onCopyNumber)
            CopyRow("Total Pembayaran:", PaymentRules.formatRupiah(invoice.total_amount), onCopyTotal)
        }

        AppButton(text = "Lihat Detail Pembayaran", onClick = onOpenDetail, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())

        // "Petunjuk Pembayaran": closed at first; each type opens its numbered steps.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DiskolaExtraShapes.card)
                .background(scheme.surfaceContainerLowest)
                .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { guideOpen = !guideOpen }.padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Petunjuk Pembayaran", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(arrow))
            }
            AnimatedVisibility(visible = guideOpen) {
                Column {
                    when {
                        guideLoading && sections.isEmpty() -> Text(
                            "Memuat petunjuk…",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.lg),
                        )
                        sections.isEmpty() -> Text(
                            "Petunjuk belum tersedia untuk metode ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.lg),
                        )
                        else -> sections.forEach { (type, steps) ->
                            HorizontalDivider(color = scheme.outlineVariant)
                            val open = openSection == type
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { openSection = if (open) null else type }
                                    .padding(horizontal = Spacing.lg),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(type, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = scheme.onSurface, modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(if (open) 180f else 0f))
                            }
                            AnimatedVisibility(visible = open) {
                                Column(
                                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    steps.forEachIndexed { index, step ->
                                        Row {
                                            Text("${index + 1}.", style = MaterialTheme.typography.bodySmall, color = scheme.primary, modifier = Modifier.padding(end = Spacing.sm))
                                            Text(step, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            PaymentStatusRules.deadlineNote(invoice.expired_at),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurfaceVariant,
        )
        // "Batalkan" belongs to Tagihanku invoices (cancelable), which arrive with P4.
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------
// "Detail Pembayaran" (doc 08 §2.6, legacy PaymentDetailPage)
// ---------------------------------------------------------------------------------------------------------------------------------

@Composable
fun PaymentDetailScreen(trxId: String, paymentMethod: String, onBack: () -> Unit, viewModel: PaymentDetailViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(trxId) { viewModel.start(trxId) }
    DetailScaffold(title = "Detail Pembayaran", onBack = onBack) { padding ->
        state.invoice?.let { PaymentDetailContent(it, paymentMethod, Modifier.padding(padding)) }
    }
    if (state.loading) AppLoadingDialog(message = "Menampilkan data transaksi...")
    if (state.missing) {
        AppDialog(
            onDismiss = onBack,
            dismissible = false,
            title = "Perhatian",
            body = "Data transaksi tidak ditemukan",
            primaryButtonText = "Kembali",
            onPrimaryClick = onBack,
        )
    }
}

@Composable
private fun PaymentDetailContent(invoice: PaymentInvoiceTable, paymentMethod: String, modifier: Modifier = Modifier) {
    val zone = ZoneId.systemDefault()
    val scheme = MaterialTheme.colorScheme
    val (chipLabel, chipTone) = PaymentStatusRules.chip(invoice.status)
    val method = paymentMethod.ifBlank { invoice.channel_method_name }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            val (container, content) = toneColors(chipTone)
            StatusPill(chipLabel, container, content)
            Text("Total Bayar", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(PaymentRules.formatRupiah(invoice.total_amount), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
        }
        SectionCard {
            Text(PaymentStatusRules.detailCardTitle(invoice.type), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            HorizontalDivider(color = scheme.outlineVariant)
            DetailRow("ID Transaksi", invoice.trx_id)
            DetailRow("Jenis Pembayaran", invoice.type.uppercase())
            DetailRow("Keperluan", invoice.note.ifBlank { "-" })
            DetailRow("Tanggal Transaksi", PaymentDates.display(invoice.created_at, zone).ifBlank { "-" })
            DetailRow("Metode Bayar", method.uppercase().ifBlank { "-" })
            DetailRow("Status", invoice.status.ifBlank { "-" }, valueColor = toneColors(PaymentStatusRules.statusTone(invoice.status)).second)
            HorizontalDivider(color = scheme.outlineVariant)
            DetailRow(PaymentStatusRules.amountRowLabel(invoice.type), PaymentRules.formatRupiah(invoice.amount))
            DetailRow(
                "Biaya Admin Bank",
                PaymentStatusRules.adminFeeLabel(invoice.admin_fee),
                valueColor = if (invoice.admin_fee == 0L) MaterialTheme.extendedColors.onSuccessContainer else scheme.onSurface,
            )
            DetailRow("Total Pembayaran", PaymentRules.formatRupiah(invoice.total_amount), emphasize = true)
        }
        if (PaymentStatusRules.isRetail(method)) {
            Text(
                PaymentStatusRules.RETAIL_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extendedColors.onWarningContainer,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.extendedColors.warningContainer, DiskolaExtraShapes.card).padding(Spacing.lg),
            )
        }
    }
}

/** Container and content colour of a status tone. */
@Composable
private fun toneColors(tone: PaymentStatusRules.Tone): Pair<Color, Color> {
    val extended = MaterialTheme.extendedColors
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        PaymentStatusRules.Tone.Success -> extended.successContainer to extended.onSuccessContainer
        PaymentStatusRules.Tone.Error -> scheme.errorContainer to scheme.onErrorContainer
        PaymentStatusRules.Tone.Warning -> extended.warningContainer to extended.onWarningContainer
        PaymentStatusRules.Tone.Info -> extended.infoContainer to extended.onInfoContainer
        PaymentStatusRules.Tone.Neutral -> scheme.surfaceContainerHigh to scheme.onSurface
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------
// "Transaksi Berhasil" (doc 08 §2.7, legacy SuccessPayPage — wallet payments)
// ---------------------------------------------------------------------------------------------------------------------------------

@Composable
fun PaymentDoneScreen(type: String, amountLabel: String, items: List<String>, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var closing by rememberSaveable { mutableStateOf(false) }
    PaymentDoneContent(
        type = type,
        amountLabel = amountLabel,
        items = items,
        onClose = {
            if (closing) return@PaymentDoneContent
            closing = true
            // After an SPP payment legacy asks Google Play for a review, then closes (`SuccessPayPage.kt:82-102`); a failure just closes.
            if (type.equals("SPP", ignoreCase = true)) {
                scope.launch {
                    runCatching {
                        val activity = context.findActivity() ?: error("no activity")
                        val manager = ReviewManagerFactory.create(context)
                        val info = manager.requestReview()
                        manager.launchReview(activity, info)
                    }
                    onClose()
                }
            } else {
                onClose()
            }
        },
    )
}

@Composable
private fun PaymentDoneContent(type: String, amountLabel: String, items: List<String>, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    Surface(color = scheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xxl),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier.padding(top = Spacing.huge).size(112.dp).background(extended.successContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = extended.onSuccessContainer, modifier = Modifier.size(64.dp))
                }
                Text("Transaksi Berhasil", style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, textAlign = TextAlign.Center)
                StatusPill("lunas", extended.successContainer, extended.onSuccessContainer)
                Text(
                    "Saldo terpotong untuk melakukan pembayaran ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                SectionCard(modifier = Modifier.padding(top = Spacing.md)) {
                    Text(type, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                    items.filter { it.isNotBlank() }.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    DetailRow("Total", amountLabel, emphasize = true)
                }
            }
            AppButton(text = "OK, Kembali", onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------------------------------------------

private val previewInvoice = PaymentInvoiceTable(
    trx_id = "TRX-2026102700123", type = "SPP", product_name = "SPP", channel_method_name = "BANK BRI", payment_code = "8888 0012 3456 7890",
    partner_reff2 = "REF2-77", admin_fee = 4_000, amount = 325_000, total_amount = 329_000, note = "SPP Juli, Agustus 2026",
    created_at = "2026-10-27T01:00:00.000Z", expired_at = "28/10/2026 08:00", status = "pending",
)

@Preview(name = "Guide — light", showBackground = true, heightDp = 900)
@Composable
private fun PaymentGuidePreview() {
    DiskolaTheme {
        PaymentGuideContent(
            invoice = previewInvoice, partnerReff2 = "REF2-77",
            sections = mapOf("ATM" to listOf("Masukkan kartu ATM dan PIN", "Pilih Transaksi Lain > Pembayaran > BRIVA"), "Mobile Banking" to listOf("Buka BRImo")),
            guideLoading = false, onCopyNumber = {}, onCopyTotal = {}, onOpenDetail = {},
        )
    }
}

@Preview(name = "Detail — dark", showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentDetailPreview() {
    DiskolaTheme { PaymentDetailContent(previewInvoice.copy(channel_method_name = "RETAIL ALFAMART", admin_fee = 0), paymentMethod = "") }
}

@Preview(name = "Done — light", showBackground = true)
@Preview(name = "Done — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentDonePreview() {
    DiskolaTheme { PaymentDoneContent("SPP", "Rp 325.000", listOf("SPP Juli 2026", "SPP Agustus 2026"), onClose = {}) }
}
