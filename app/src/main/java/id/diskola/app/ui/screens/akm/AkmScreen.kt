package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.akm.AkmItem
import id.diskola.app.dataclass.akm.AkmScoreItem
import id.diskola.app.dataclass.akm.AkmStatus
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AutoTimeGate
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AkmViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

private sealed interface ReuploadDialog {
    data class Confirm(val id: Int) : ReuploadDialog
    data object NotFound : ReuploadDialog
}

/**
 * Asesmen home — scheduled exams on one tab, past scores on the other.
 *
 * [showTimeGate] fires the "set your clock to automatic" gate, which Home raises when opening
 * this screen: an exam cannot be trusted if the device clock is set by hand.
 */
@Composable
fun AkmScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onOpenScore: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showTimeGate: Boolean = false,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val scores by viewModel.scores.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val strictMode by viewModel.strictMode.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var reuploadDialog by remember { mutableStateOf<ReuploadDialog?>(null) }
    val calendar = remember { Calendar.getInstance() }
    val monthLabel = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time) }
    val scheduleDays = remember {
        val dowFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..daysInMonth).map { day ->
            val cursor = calendar.clone() as Calendar
            cursor.set(Calendar.DAY_OF_MONTH, day)
            day to dowFormat.format(cursor.time)
        }
    }
    val today = remember { calendar.get(Calendar.DAY_OF_MONTH) }

    // Ticks every 30s so a card whose exam just ended drops off the list on its own, without
    // requiring the student to leave and reopen the screen.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    DetailScaffold(
        title = "Asesmen",
        onBack = onBack,
        modifier = modifier,
        actions = { StrictModeChip(strict = strictMode) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UnderlineTabRow(
                options = listOf("Jadwal", "Nilai"),
                selectedIndex = tabIndex,
                onSelect = { tabIndex = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
            )

            if (loading && items.isEmpty() && scores.isEmpty()) {
                LoadingState()
            } else if (tabIndex == 0) {
                ScheduleTab(
                    monthLabel = monthLabel,
                    scheduleDays = scheduleDays,
                    today = today,
                    selectedDay = selectedDay,
                    onSelectDay = viewModel::selectDay,
                    items = items.filter { it.endDay == selectedDay && !it.hasEnded(now) },
                    statusOf = { statuses[it] ?: AkmStatus.BelumSinkron },
                    onOpen = onOpenDetail,
                )
            } else {
                ScoreTab(
                    scores = scores,
                    onOpen = onOpenScore,
                    onReupload = { reuploadDialog = ReuploadDialog.Confirm(it) },
                )
            }
        }
    }

    AutoTimeGate(
        title = "Atur Waktu Otomatis",
        body = "Harap atur tanggal dan waktu ponsel ke Otomatis. Asesmen tidak dapat dimulai bila waktu perangkat diatur manual.",
        onCancel = onBack,
        enabled = showTimeGate,
    )

    when (val dialog = reuploadDialog) {
        is ReuploadDialog.Confirm -> AppDialog(
            onDismiss = { reuploadDialog = null },
            title = "Upload Ulang Jawaban",
            body = "Jawaban lokal akan dikirim ulang ke server. Upload ulang hanya dapat dilakukan sekali per menit dan harus dari perangkat yang dipakai saat ujian.",
            primaryButtonText = "Upload",
            onPrimaryClick = {
                val resent = viewModel.reuploadAnswer(dialog.id)
                reuploadDialog = if (resent) null else ReuploadDialog.NotFound
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { reuploadDialog = null },
        )

        ReuploadDialog.NotFound -> AppDialog(
            onDismiss = { reuploadDialog = null },
            title = "Informasi",
            body = "Data jawaban tidak ditemukan di perangkat ini. Anda tidak bisa melakukan upload ulang jawaban dari perangkat ini.",
            primaryButtonText = "Tutup",
            onPrimaryClick = { reuploadDialog = null },
        )

        null -> Unit
    }
}

/**
 * Read-only: [strict] comes from the server's `exam_lock_mode` (`mobile/setting-akm`), not
 * something a student is allowed to switch off on-device.
 */
@Composable
private fun StrictModeChip(strict: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .padding(end = Spacing.sm)
            .heightIn(min = 36.dp)
            .background(scheme.surfaceContainerLowest, RoundedCornerShape(18.dp))
            .border(1.5.dp, scheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (strict) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            if (strict) "Mode ketat: aktif" else "Mode ketat: mati",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = scheme.onSurface,
            modifier = Modifier.padding(start = Spacing.xs),
        )
    }
}

@Composable
private fun ScheduleTab(
    monthLabel: String,
    scheduleDays: List<Pair<Int, String>>,
    today: Int,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    items: List<AkmItem>,
    statusOf: (Int) -> AkmStatus,
    onOpen: (Int) -> Unit,
) {
    Column {
        Text(
            monthLabel.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            scheduleDays.forEach { (day, dow) ->
                DayCell(
                    day = day,
                    dayOfWeek = dow,
                    selected = day == selectedDay,
                    past = day < today,
                    onClick = { onSelectDay(day) },
                )
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                title = "Belum terdapat ujian",
                description = "Tidak ada jadwal asesmen pada tanggal ini.",
                icon = Icons.Rounded.EventBusy,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.lg, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items, key = { it.id }) { item ->
                    ScheduleCard(item = item, status = statusOf(item.id), onOpen = { onOpen(item.id) })
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, dayOfWeek: String, selected: Boolean, past: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(52.dp)
            .heightIn(min = 64.dp)
            .background(
                if (selected) scheme.primary else scheme.surfaceContainerLowest,
                DiskolaExtraShapes.card,
            )
            .border(
                1.5.dp,
                if (selected) scheme.primary else scheme.outlineVariant,
                DiskolaExtraShapes.card,
            )
            .then(if (past) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val contentAlpha = if (past) 0.3f else 1f
        Text(
            dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = (if (selected) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = contentAlpha),
        )
        Text(
            day.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = (if (selected) scheme.onPrimary else scheme.onSurface).copy(alpha = contentAlpha),
        )
    }
}

@Composable
private fun ScheduleCard(item: AkmItem, status: AkmStatus, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).background(scheme.surfaceContainerLow, DiskolaExtraShapes.iconBox),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Assignment,
                    contentDescription = null,
                    tint = scheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                )
                Text(
                    "Asesmen Sekolah · ${item.questionCount} soal",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    "Berakhir ${item.endLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AkmChip(status.label)
            if (item.needsPassword) {
                AkmChip("Perlu password", icon = Icons.Rounded.Key)
            }
        }
        AppButton(text = "Ikuti Ujian", onClick = onOpen, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun AkmChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accent: Boolean = false,
    error: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        error -> scheme.errorContainer
        accent -> scheme.secondaryContainer
        else -> scheme.surfaceContainerLow
    }
    val foreground = when {
        error -> scheme.onErrorContainer
        accent -> scheme.onSecondaryContainer
        else -> scheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .background(background, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = foreground,
            modifier = if (icon != null) Modifier.padding(start = Spacing.xs) else Modifier,
        )
    }
}

@Composable
private fun ScoreTab(scores: List<AkmScoreItem>, onOpen: (Int) -> Unit, onReupload: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    if (scores.isEmpty()) {
        EmptyState(
            title = "Belum ada nilai",
            description = "Nilai asesmen yang sudah dikumpulkan akan muncul di sini.",
            icon = Icons.Rounded.EventBusy,
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.lg, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(scores, key = { it.id }) { score ->
            val canReupload = score.status == AkmStatus.SudahDikumpulkan || score.status == AkmStatus.MenungguPenilaian
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                    .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                    .clickable { onOpen(score.id) }
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
                        Text(
                            score.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                            color = scheme.onSurface,
                        )
                        Text(
                            "${score.dateLabel} · ${score.status.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier.size(56.dp).background(scheme.surfaceContainerLow, DiskolaExtraShapes.card),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (score.status == AkmStatus.SudahDinilai) score.score.toString() else "—",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = scheme.primary,
                        )
                    }
                }
                if (canReupload) {
                    AppButton(
                        text = "Upload Ulang",
                        onClick = { onReupload(score.id) },
                        variant = ButtonVariant.Outlined,
                    )
                }
            }
        }
    }
}
