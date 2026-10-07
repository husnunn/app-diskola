package id.diskola.app.ui.screens.presensi

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.PresensiDayTable
import id.diskola.app.dataclass.ResponData.PresensiRekapTable
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AutoTimeGate
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.MonthYearPickerDialog
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.PresensiAction
import id.diskola.app.viewmodel.PresensiViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TAB_LABELS = listOf("Data Absensi", "Rekap Absensi")
private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))
private val DAY_LABEL = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id"))

/** "Presensi" shell (doc `07` §4): Data Absensi + Rekap Absensi, with "Tambah Izin" and "Panduan" in the bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresensiScreen(
    onBack: () -> Unit,
    onOpenMasuk: () -> Unit,
    onOpenOffsite: () -> Unit,
    onOpenIzin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresensiViewModel = hiltViewModel(),
) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val canPerform by viewModel.canPerform.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var chooseType by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var offsiteDetail by remember { mutableStateOf<OffsiteDetail?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(tab) { if (tab == 1) viewModel.ensureRekap() }

    AutoTimeGate(
        title = "Peringatan",
        body = "Harap atur tanggal dan waktu ponsel ke \"Otomatis\"",
        onCancel = onBack,
    )

    DetailScaffold(
        title = "Presensi",
        onBack = onBack,
        modifier = modifier,
        actions = {
            TextButton(onClick = onOpenIzin, modifier = Modifier.heightIn(min = 48.dp)) { Text("Tambah Izin") }
            IconButton(onClick = { showGuide = true }) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Panduan")
            }
        },
        bottomBar = {
            if (tab == 0) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                ) {
                    AppButton(
                        text = "Lakukan Presensi",
                        onClick = {
                            when (val action = viewModel.onPresensiClick()) {
                                is PresensiAction.Blocked -> alertMessage = action.message
                                is PresensiAction.OffsiteBlocked -> alertMessage = action.message
                                PresensiAction.ChooseType -> chooseType = true
                                PresensiAction.Onsite -> onOpenMasuk()
                            }
                        },
                        enabled = canPerform,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
            if (errorMessage.isNotBlank()) {
                BannerError(
                    message = errorMessage,
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                )
            }
            if (tab == 0) {
                AbsenTab(
                    month = month,
                    days = days,
                    loading = loading,
                    onPrev = { viewModel.shiftMonth(-1) },
                    onNext = { viewModel.shiftMonth(1) },
                    onPickMonth = { showMonthPicker = true },
                    onRefresh = viewModel::refresh,
                    onOffsiteDetail = { offsiteDetail = it },
                )
            } else {
                RekapTab(viewModel)
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

    if (chooseType) {
        AppBottomSheet(title = "Pilih Jenis Presensi", onDismiss = { chooseType = false }) {
            Column(
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppButton(
                    text = "Presensi di Sekolah",
                    onClick = {
                        chooseType = false
                        onOpenMasuk()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(
                    text = "Presensi Dinas Luar",
                    onClick = {
                        chooseType = false
                        onOpenOffsite()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    alertMessage?.let { message ->
        AppDialog(
            onDismiss = { alertMessage = null },
            title = "Pemberitahuan",
            body = message,
            primaryButtonText = "Tutup",
            onPrimaryClick = { alertMessage = null },
        )
    }

    if (showGuide) GuideSheet(onDismiss = { showGuide = false })
    offsiteDetail?.let { OffsiteDetailSheet(it, onDismiss = { offsiteDetail = null }) }
}

// ---- Data Absensi ----

private data class OffsiteDetail(
    val title: String,
    val status: String,
    val time: String,
    val address: String,
    val note: String,
    val photoUrl: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbsenTab(
    month: YearMonth,
    days: List<PresensiDayTable>,
    loading: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickMonth: () -> Unit,
    onRefresh: () -> Unit,
    onOffsiteDetail: (OffsiteDetail) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isCurrent = month == YearMonth.now()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya") }
            Text(
                (if (isCurrent) "Bulan ini • " else "") + MONTH_LABEL.format(month),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onPickMonth).padding(vertical = Spacing.md),
            )
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya") }
        }
        TableHeader(listOf("Tanggal" to 1.4f, "Jam masuk" to 1f, "Jam keluar" to 1f))

        val listState = rememberLazyListState()
        // Open the current month at today's row; other months start at the 1st.
        LaunchedEffect(month, days.isNotEmpty()) {
            if (isCurrent && days.isNotEmpty()) {
                val index = days.indexOfFirst { it.date == LocalDate.now().toString() }
                if (index > 0) listState.scrollToItem(index)
            }
        }
        PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (days.isEmpty()) {
                    if (!loading) item {
                        EmptyState(
                            title = "Data kehadiran tidak tersedia",
                            description = "",
                            icon = Icons.Rounded.EventBusy,
                            modifier = Modifier.fillParentMaxHeight(0.6f),
                        )
                    }
                } else {
                    items(days, key = { it.date }) { day -> DayRow(day, onOffsiteDetail) }
                }
            }
        }
    }
}

@Composable
private fun TableHeader(columns: List<Pair<String, Float>>) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .background(scheme.surfaceContainerHigh, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        columns.forEachIndexed { index, (label, weight) ->
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
                textAlign = if (index == 0) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.weight(weight),
            )
        }
    }
}

@Composable
private fun DayRow(day: PresensiDayTable, onOffsiteDetail: (OffsiteDetail) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val success = MaterialTheme.extendedColors.success
    val dateLabel = remember(day.date) { runCatching { DAY_LABEL.format(LocalDate.parse(day.date)) }.getOrDefault(day.date) }
    val approvedLeave = day.leaveRequestStatus.equals("approved", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(if (day.isHoliday) scheme.surfaceContainerLowest else scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1.4f))
        if (day.isHoliday) {
            Text(
                "Libur",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(2f),
            )
        } else {
            val (inText, inColor) = when {
                approvedLeave -> day.leaveRequestType.replaceFirstChar { it.uppercase() }.ifBlank { "Izin" } to success
                day.attendAt.isBlank() -> "-" to scheme.error
                day.attendIsLate -> day.attendAt to scheme.error
                else -> day.attendAt to scheme.onSurface
            }
            val (outText, outColor) = when {
                day.leaveAt.isBlank() -> "-" to scheme.error
                day.leaveIsEarly -> day.leaveAt to scheme.error
                else -> day.leaveAt to scheme.onSurface
            }
            TimeCell(inText, inColor, day.attendIsOffsite, Modifier.weight(1f)) {
                onOffsiteDetail(
                    OffsiteDetail("Presensi Masuk Dinas Luar", day.attendStatus, day.attendAt, day.attendAddress, day.attendNote, day.attendPhotoUrl),
                )
            }
            TimeCell(outText, outColor, day.leaveIsOffsite, Modifier.weight(1f)) {
                onOffsiteDetail(
                    OffsiteDetail("Presensi Pulang Dinas Luar", day.leaveStatus, day.leaveAt, day.leaveAddress, day.leaveNote, day.leavePhotoUrl),
                )
            }
        }
    }
}

@Composable
private fun TimeCell(text: String, color: androidx.compose.ui.graphics.Color, offsite: Boolean, modifier: Modifier, onDetail: () -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
        if (offsite) {
            IconButton(onClick = onDetail, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Visibility, contentDescription = "Detail presensi dinas luar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OffsiteDetailSheet(detail: OffsiteDetail, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var zoomed by remember { mutableStateOf(false) }
    AppBottomSheet(title = detail.title, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val badStatus = detail.status.contains("Terlambat", ignoreCase = true) || detail.status.contains("Awal", ignoreCase = true)
            DetailLine("Status", detail.status.ifBlank { "-" }, if (badStatus) scheme.error else scheme.onSurface)
            DetailLine("Jam", detail.time.ifBlank { "-" })
            DetailLine("Alamat", detail.address.ifBlank { "-" })
            DetailLine("Catatan", detail.note.ifBlank { "-" })
            Text("Foto Bukti", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            if (detail.photoUrl.isBlank()) {
                Text("-", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            } else {
                AsyncImage(
                    model = resolveAssetUrl(detail.photoUrl),
                    contentDescription = "Foto bukti",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(DiskolaExtraShapes.card)
                        .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                        .clickable { zoomed = true },
                )
            }
            AppButton(text = "OKE", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
    if (zoomed) {
        AppDialog(
            onDismiss = { zoomed = false },
            title = "Foto Bukti",
            body = "",
            primaryButtonText = "Tutup",
            onPrimaryClick = { zoomed = false },
        ) {
            AsyncImage(
                model = resolveAssetUrl(detail.photoUrl),
                contentDescription = "Foto bukti",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(DiskolaExtraShapes.card),
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

// ---- Rekap ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RekapTab(viewModel: PresensiViewModel) {
    val year by viewModel.year.collectAsStateWithLifecycle()
    val rows by viewModel.rekap.collectAsStateWithLifecycle()
    val loading by viewModel.rekapLoading.collectAsStateWithLifecycle()
    val filterEnabled by viewModel.rekapFilterEnabled.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val isCurrent = year == LocalDate.now().year

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs).alpha(if (filterEnabled) 1f else 0.4f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.shiftYear(-1) }, enabled = filterEnabled) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Tahun sebelumnya")
            }
            Text(
                (if (isCurrent) "Tahun ini • " else "") + year,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.shiftYear(1) }, enabled = filterEnabled) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Tahun berikutnya")
            }
        }
        TableHeader(listOf("Bulan" to 1.6f, "H" to 0.6f, "T" to 0.6f, "I" to 0.6f, "S" to 0.6f))
        PullToRefreshBox(isRefreshing = loading, onRefresh = viewModel::refreshRekap, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.xs, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (rows.isEmpty()) {
                    if (!loading) item {
                        EmptyState(
                            title = "Data rekap tidak tersedia",
                            description = "",
                            icon = Icons.Rounded.EventBusy,
                            modifier = Modifier.fillParentMaxHeight(0.6f),
                        )
                    }
                } else {
                    items(rows, key = { it.orderIndex }) { row -> RekapRow(row) }
                }
            }
        }
    }
}

@Composable
private fun RekapRow(row: PresensiRekapTable) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${row.month} ${row.year}", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1.6f))
        listOf(row.ontime to false, row.late to true, row.izin to false, row.sakit to false).forEach { (value, redIfPositive) ->
            Text(
                value.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (redIfPositive && value > 0) scheme.error else scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.6f),
            )
        }
    }
}

// ---- Panduan ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuideSheet(onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AppBottomSheet(title = "Panduan", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            listOf(
                "Data Absensi" to "Ketuk untuk melihat data absensi harian Anda.",
                "Rekap Absensi" to "Ketuk untuk melihat rekap absensi dalam periode tertentu.",
                "Tambah Izin" to "Ketuk ikon ini untuk mengajukan atau melihat pengajuan izin presensi.",
            ).forEachIndexed { index, (title, text) ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = scheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.size(28.dp).background(scheme.primary, androidx.compose.foundation.shape.CircleShape).padding(top = 3.dp),
                    )
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }
            }
            AppButton(text = "Mengerti", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
