package id.diskola.app.ui.screens.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.AgendaCheckMode
import id.diskola.app.dataclass.ResponData.AgendaUiStatus
import id.diskola.app.dataclass.ResponData.StaffAgendaItem
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DateStrip
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.MonthYearPickerDialog
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AgendaMingguanViewModel
import id.diskola.app.viewmodel.AgendaTapAction
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))

/** "Agenda Mingguan" (doc `05` §8) — month date strip + the selected day's sessions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaMingguanScreen(
    onBack: () -> Unit,
    onOpenDetail: (date: String, agendaId: Int) -> Unit,
    onOpenCheck: (date: String, agendaId: Int, mode: AgendaCheckMode) -> Unit,
    onOpenPresensi: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgendaMingguanViewModel = hiltViewModel(),
) {
    val selected by viewModel.selectedDate.collectAsStateWithLifecycle()
    val day by viewModel.day.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var showMonthPicker by remember { mutableStateOf(false) }
    var gateAlert by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val month = YearMonth.from(selected)
    val dates = remember(month) { (1..month.lengthOfMonth()).map { month.atDay(it) } }
    val current = day

    DetailScaffold(title = "Agenda Mingguan", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { showMonthPicker = true }
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    MONTH_LABEL.format(selected),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Pilih bulan", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (current != null && current.agendaEnabled && current.summary.required_sessions > 0) {
                    Text(
                        "${current.summary.checked} / ${current.summary.required_sessions} sesi",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
            DateStrip(dates = dates, selected = selected, onSelect = viewModel::selectDate)

            if (errorMessage.isNotBlank()) {
                BannerError(
                    message = errorMessage,
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                )
            }

            PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        current == null -> if (!loading) item {
                            EmptyState(title = "Gagal memuat agenda", description = "", icon = Icons.Rounded.EventNote, modifier = Modifier.fillParentMaxHeight(0.6f))
                        }
                        !current.agendaEnabled -> item {
                            EmptyState(title = "Agenda belum dikonfigurasi untuk workgroup Anda", description = "", icon = Icons.Rounded.EventNote, modifier = Modifier.fillParentMaxHeight(0.6f))
                        }
                        current.agendas.isEmpty() -> item {
                            EmptyState(title = "Tidak ada agenda pada tanggal ini", description = "", icon = Icons.Rounded.EventNote, modifier = Modifier.fillParentMaxHeight(0.6f))
                        }
                        else -> items(current.agendas, key = { it.agenda_id }) { item ->
                            AgendaItemCard(item = item, onClick = {
                                when (val action = viewModel.resolveTap(item, current)) {
                                    is AgendaTapAction.Detail -> onOpenDetail(selected.toString(), action.item.agenda_id)
                                    is AgendaTapAction.CheckIn -> onOpenCheck(selected.toString(), action.item.agenda_id, AgendaCheckMode.CHECK_IN)
                                    is AgendaTapAction.CheckOut -> onOpenCheck(selected.toString(), action.item.agenda_id, AgendaCheckMode.CHECK_OUT)
                                    AgendaTapAction.NeedPresensi -> gateAlert = true
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initial = month,
            onPick = {
                showMonthPicker = false
                viewModel.selectMonth(it)
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (gateAlert) {
        AppDialog(
            onDismiss = { gateAlert = false },
            title = "Pemberitahuan",
            body = "Silakan presensi masuk terlebih dahulu sebelum check-in agenda",
            primaryButtonText = "Presensi",
            onPrimaryClick = {
                gateAlert = false
                onOpenPresensi()
            },
            secondaryButtonText = "Tutup",
            onSecondaryClick = { gateAlert = false },
        )
    }
}

@Composable
private fun AgendaItemCard(item: StaffAgendaItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(kindIcon(item.kind), contentDescription = null, tint = scheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text(item.kind, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            }
            StatusBadge(label = item.uiStatus.label, tone = item.uiStatus.tone())
        }
        Text(
            item.note?.takeIf { it.isNotBlank() } ?: "Tidak ada catatan",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            Column {
                Text("Mulai", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(item.startAt.ifBlank { "-" }, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
            }
            Column {
                Text("Selesai", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(item.endAt.ifBlank { "-" }, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
            }
        }
    }
}

internal fun AgendaUiStatus.tone(): BadgeTone = when (this) {
    AgendaUiStatus.PENDING -> BadgeTone.Neutral
    AgendaUiStatus.NEED_CHECKOUT, AgendaUiStatus.LATE -> BadgeTone.Warning
    AgendaUiStatus.HADIR -> BadgeTone.Success
}

private fun kindIcon(kind: String) = when (kind.uppercase()) {
    "SESSION" -> Icons.Rounded.Description
    "BREAK", "ISTIRAHAT" -> Icons.Rounded.Place
    else -> Icons.Rounded.StickyNote2
}
