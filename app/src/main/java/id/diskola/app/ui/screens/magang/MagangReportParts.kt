package id.diskola.app.ui.screens.magang

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.MagangAttendanceItem
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppFilterChip
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.SkeletonVariant
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.resolveAssetUrl
import java.time.LocalDate

/** Tab "Laporan": the two filter chips and one card per report, newest first. [range] null is "Semua". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LaporanTab(
    reports: List<MagangAttendanceItem>,
    range: Pair<LocalDate, LocalDate>?,
    loaded: Boolean,
    loading: Boolean,
    errorMessage: String,
    onPickRange: () -> Unit,
    onClearRange: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onOpenReport: (MagangAttendanceItem) -> Unit,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = Spacing.md)
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AppFilterChip(label = "Semua", selected = range == null, onClick = onClearRange)
            AppFilterChip(
                label = range?.let { MagangRules.rangeLabel(it.first, it.second) } ?: "Pilih Tanggal",
                selected = range != null,
                onClick = onPickRange,
            )
        }
        if (errorMessage.isNotBlank() && loaded) {
            BannerError(
                message = errorMessage,
                onDismiss = onDismissError,
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
            )
        }
        when {
            !loaded && loading -> LoadingState(padding = contentPadding, variant = SkeletonVariant.ListRow)
            !loaded -> ErrorState(
                description = errorMessage.ifBlank { "Periksa koneksi internet Anda dan coba lagi." },
                onRetry = onRetry,
            )
            else -> {
                val shown = MagangRules.reportsIn(MagangRules.reportsOf(reports), range?.first, range?.second)
                PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (shown.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Data tidak tersedia",
                                    description = if (range == null) "Belum ada laporan magang." else "Tidak ada laporan pada rentang tanggal ini.",
                                    icon = Icons.Rounded.EventBusy,
                                    modifier = Modifier.fillParentMaxHeight(0.7f),
                                )
                            }
                        } else {
                            items(shown) { report ->
                                ReportCard(
                                    item = report,
                                    onViewMore = { onOpenReport(report) },
                                    onOpenAttachment = onOpenAttachment,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One report: company, check-in and check-out, the attachment thumbnail, and the text cut short with "Lihat Selengkapnya" when long. */
@Composable
internal fun ReportCard(
    item: MagangAttendanceItem,
    onViewMore: () -> Unit,
    onOpenAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val attachment = MagangRules.attachmentOf(item)
    val text = MagangRules.reportText(item)
    AppCard(modifier = modifier, variant = CardVariant.Elevated) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                item.company?.name?.trim().orEmpty().ifEmpty { "Tempat Magang" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider(color = scheme.outlineVariant)
            LabeledRow("Masuk", MagangRules.dateTimeLabel(item.check_in_at))
            LabeledRow("Keluar", MagangRules.dateTimeLabel(item.check_out_at))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lampiran", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (attachment == null) {
                    Text("-", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                } else {
                    AsyncImage(
                        model = resolveAssetUrl(attachment),
                        contentDescription = "Lampiran laporan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(DiskolaExtraShapes.iconBox)
                            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.iconBox)
                            .clickable { onOpenAttachment(attachment) },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Laporan", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (MagangRules.isLongReport(item.daily_report)) {
                    TextButton(onClick = onViewMore, modifier = Modifier.heightIn(min = 48.dp)) { Text("Lihat Selengkapnya") }
                }
            }
        }
    }
}

@Composable
internal fun LabeledRow(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
    }
}

/** The attachment on a full screen: pinch to zoom, drag when zoomed, "Tutup" top right. [model] is a URL or a local file. */
@Composable
internal fun ImagePreviewDialog(model: Any, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
            AsyncImage(
                model = model,
                contentDescription = "Lampiran laporan",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm)) {
                Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color.White)
            }
        }
    }
}

/** The tap target of the report form: the picked picture with a remove button, or the "Tambah Gambar" box. */
@Composable
internal fun AttachmentBox(
    photo: Any?,
    processing: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    if (photo != null) {
        Box(modifier = modifier.fillMaxWidth()) {
            AsyncImage(
                model = photo,
                contentDescription = "Gambar terlampir",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(DiskolaExtraShapes.card)
                    .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                    .clickable(onClick = onPreview),
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs).background(scheme.surfaceContainerLowest.copy(alpha = 0.85f), DiskolaExtraShapes.chipBadge),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus gambar", tint = scheme.onSurface)
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(DiskolaExtraShapes.card)
                .background(scheme.surfaceContainerLow)
                .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                .clickable(enabled = !processing, onClick = onPick)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (processing) "Memproses gambar…" else "Tambah Gambar",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.primary,
            )
            Text("JPG, PNG (maks. 5 MB)", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}
