package id.diskola.app.ui.screens.magang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.MagangScheduleItem
import id.diskola.app.ui.components.AppDateRangePickerDialog
import id.diskola.app.ui.components.AutoTimeGate
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DateStrip
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.MonthYearPickerDialog
import id.diskola.app.ui.components.SkeletonVariant
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.MagangViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private val TAB_LABELS = listOf("Jadwal", "Laporan")

/**
 * "Magang" shell (doc `07` §11): the student's internship schedule (a date strip and one card per day) and, in the second tab, the reports
 * sent so far. [onOpenMasuk] and [onOpenLaporan] take the schedule id the card belongs to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagangScreen(
    onBack: () -> Unit,
    onOpenMasuk: (scheduleId: Long) -> Unit,
    onOpenLaporan: (scheduleId: Long) -> Unit,
    onOpenReport: (reportId: Long) -> Unit,
    onOpenIzin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MagangViewModel = hiltViewModel(),
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val attendanceLoaded by viewModel.attendanceLoaded.collectAsStateWithLifecycle()
    val attendanceError by viewModel.attendanceError.collectAsStateWithLifecycle()
    val reportRange by viewModel.reportRange.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }
    var attachment by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AutoTimeGate(
        title = "Peringatan",
        body = "Harap atur tanggal dan waktu ponsel ke \"Otomatis\"",
        onCancel = onBack,
    )

    DetailScaffold(
        title = "Magang",
        onBack = onBack,
        modifier = modifier,
        actions = {
            TextButton(onClick = onOpenIzin, modifier = Modifier.heightIn(min = 48.dp)) { Text("Tambah Izin") }
            if (tab == 0) {
                IconButton(onClick = { showGuide = true }) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Panduan")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UnderlineTabRow(
                options = TAB_LABELS,
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
            )
            if (tab == 0) {
                JadwalTab(
                    selected = selected,
                    schedules = schedules,
                    loaded = loaded,
                    loading = loading,
                    errorMessage = errorMessage,
                    onSelectDate = viewModel::selectDate,
                    onPickMonth = { showMonthPicker = true },
                    onRefresh = viewModel::refresh,
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refresh()
                    },
                    onDismissError = viewModel::clearError,
                    onMasuk = { onOpenMasuk(it.id) },
                    onLaporan = { onOpenLaporan(it.id) },
                )
            } else {
                LaporanTab(
                    reports = attendance,
                    range = reportRange,
                    loaded = attendanceLoaded,
                    loading = loading,
                    errorMessage = attendanceError,
                    onPickRange = { showRangePicker = true },
                    onClearRange = viewModel::clearReportRange,
                    onRefresh = viewModel::refresh,
                    onRetry = {
                        viewModel.clearAttendanceError()
                        viewModel.refresh()
                    },
                    onDismissError = viewModel::clearAttendanceError,
                    onOpenReport = { onOpenReport(it.id) },
                    onOpenAttachment = { attachment = it },
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initial = YearMonth.from(selected),
            onPick = {
                showMonthPicker = false
                viewModel.selectMonth(it)
            },
            onDismiss = { showMonthPicker = false },
        )
    }
    if (showGuide) MagangGuideSheet(onDismiss = { showGuide = false })
    if (showRangePicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = reportRange?.first?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = reportRange?.second?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        AppDateRangePickerDialog(
            state = state,
            title = "Pilih Rentang Tanggal",
            confirmText = "Pilih",
            onConfirm = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis
                if (start != null && end != null) {
                    viewModel.setReportRange(
                        Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate(),
                        Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate(),
                    )
                }
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false },
        )
    }
    attachment?.let { ImagePreviewDialog(model = resolveAssetUrl(it), onDismiss = { attachment = null }) }
}

/** Tab "Jadwal": month label, one cell per day of that month, and the cards of the chosen day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JadwalTab(
    selected: LocalDate,
    schedules: List<MagangScheduleItem>,
    loaded: Boolean,
    loading: Boolean,
    errorMessage: String,
    onSelectDate: (LocalDate) -> Unit,
    onPickMonth: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onMasuk: (MagangScheduleItem) -> Unit,
    onLaporan: (MagangScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val month = YearMonth.from(selected)
    val contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = Spacing.md)
    Column(modifier = modifier.fillMaxSize()) {
        MonthLabel(month = month, onClick = onPickMonth)
        DateStrip(dates = MagangRules.daysOf(month), selected = selected, onSelect = onSelectDate)
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
                val cards = MagangRules.schedulesOn(schedules, selected)
                PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (cards.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Jadwal Tidak Tersedia",
                                    description = "Tidak ada jadwal magang pada tanggal ini.",
                                    icon = Icons.Rounded.EventBusy,
                                    modifier = Modifier.fillParentMaxHeight(0.7f),
                                )
                            }
                        } else {
                            items(cards) { item ->
                                ScheduleCard(item = item, onMasuk = { onMasuk(item) }, onLaporan = { onLaporan(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}
