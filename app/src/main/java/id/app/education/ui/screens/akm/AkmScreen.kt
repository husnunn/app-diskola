package id.app.education.ui.screens.akm

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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.dataclass.mock.AkmItem
import id.app.education.dataclass.mock.AkmScoreItem
import id.app.education.dataclass.mock.AkmStatus
import id.app.education.dataclass.mock.MockAkm
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppDialog
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.DetailScaffold
import id.app.education.ui.components.EmptyState
import id.app.education.ui.components.UnderlineTabRow
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.AkmViewModel

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
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val strictMode by viewModel.strictMode.collectAsStateWithLifecycle()
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var timeGateVisible by rememberSaveable { mutableStateOf(showTimeGate) }
    var reuploadFor by rememberSaveable { mutableStateOf<Int?>(null) }

    DetailScaffold(
        title = "Asesmen",
        onBack = onBack,
        modifier = modifier,
        actions = { StrictModeChip(strict = strictMode, onClick = viewModel::toggleStrictMode) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UnderlineTabRow(
                options = listOf("Jadwal", "Nilai"),
                selectedIndex = tabIndex,
                onSelect = { tabIndex = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
            )

            if (tabIndex == 0) {
                ScheduleTab(
                    selectedDay = selectedDay,
                    onSelectDay = viewModel::selectDay,
                    items = viewModel.scheduleFor(selectedDay),
                    statusOf = { statuses[it] ?: AkmStatus.BelumSinkron },
                    onOpen = onOpenDetail,
                )
            } else {
                ScoreTab(
                    scores = viewModel.scores,
                    onOpen = onOpenScore,
                    onReupload = { reuploadFor = it },
                )
            }
        }
    }

    if (timeGateVisible) {
        AppDialog(
            onDismiss = { timeGateVisible = false },
            title = "Atur Waktu Otomatis",
            body = "Harap atur tanggal dan waktu ponsel ke Otomatis. Asesmen tidak dapat dimulai bila waktu perangkat diatur manual.",
            primaryButtonText = "Buka Pengaturan",
            onPrimaryClick = { timeGateVisible = false },
            secondaryButtonText = "Jangan Ubah",
            onSecondaryClick = {
                timeGateVisible = false
                onBack()
            },
            dismissible = false,
        )
    }

    reuploadFor?.let {
        AppDialog(
            onDismiss = { reuploadFor = null },
            title = "Upload Ulang Jawaban",
            body = "Jawaban lokal akan dikirim ulang ke server. Upload ulang hanya dapat dilakukan sekali per menit dan harus dari perangkat yang dipakai saat ujian.",
            primaryButtonText = "Upload",
            onPrimaryClick = { reuploadFor = null },
            secondaryButtonText = "Batal",
            onSecondaryClick = { reuploadFor = null },
        )
    }
}

@Composable
private fun StrictModeChip(strict: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .padding(end = Spacing.sm)
            .heightIn(min = 36.dp)
            .background(scheme.surfaceContainerLowest, RoundedCornerShape(18.dp))
            .border(1.5.dp, scheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
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
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    items: List<AkmItem>,
    statusOf: (Int) -> AkmStatus,
    onOpen: (Int) -> Unit,
) {
    Column {
        Text(
            MockAkm.MONTH_LABEL,
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
            MockAkm.scheduleDays.forEach { (day, dow) ->
                DayCell(
                    day = day,
                    dayOfWeek = dow,
                    selected = day == selectedDay,
                    past = day < MockAkm.TODAY,
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
                    if (item.government) Icons.Rounded.AccountBalance else Icons.Rounded.Assignment,
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
                    "${item.kind} · ${item.questionCount} soal",
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
