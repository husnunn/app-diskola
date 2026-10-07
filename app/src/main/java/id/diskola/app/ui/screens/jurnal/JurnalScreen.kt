package id.diskola.app.ui.screens.jurnal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AutoTimeGate
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.JurnalRow
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.QrScanner
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.JurnalViewModel
import java.time.LocalTime
import kotlinx.coroutines.launch

/**
 * Jurnal KBM list (doc `07` §7.1): today's hours and class sessions. Students get "Hadiri kelas" (manual or QR) and
 * "Isi Jurnal"; teachers get "Mulai kelas", "Detail" and "Isi Jurnal". [initialAction] carries a Hub-card shortcut
 * ("hadiri"/"isi") that is replayed once the list has loaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurnalScreen(
    initialAction: String,
    initialAttendanceId: Int,
    initialPlotId: Int,
    onBack: () -> Unit,
    onOpenVerifikasi: (attendanceId: Int, title: String) -> Unit,
    onOpenForm: (plotId: Int, plotLabel: String) -> Unit,
    onOpenDetail: (attendanceId: Int, createdAt: String, plot: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JurnalViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isStudent = viewModel.isStudent

    var methodRow by remember { mutableStateOf<JurnalRow?>(null) }
    var startRow by remember { mutableStateOf<JurnalRow?>(null) }
    var shortcutHandled by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Hub shortcut: replayed once, after the first load.
    LaunchedEffect(loaded) {
        if (!loaded || shortcutHandled || initialAction.isBlank()) return@LaunchedEffect
        shortcutHandled = true
        val row = rows.firstOrNull { it.attendanceId != null && it.attendanceId == initialAttendanceId }
        when (initialAction) {
            "hadiri" -> if (row != null && JurnalRules.studentUi(row).action == JurnalRules.StudentAction.HADIRI) methodRow = row
            "isi" -> {
                val target = row ?: rows.firstOrNull { it.plotId == initialPlotId }
                if (target != null) onOpenForm(target.plotId, target.timeLabel)
            }
        }
    }

    AutoTimeGate(
        title = "Peringatan",
        body = "Harap atur tanggal dan waktu ponsel ke \"Otomatis\"",
        onCancel = onBack,
    )

    DetailScaffold(title = "Jurnal", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (errorMessage.isNotBlank() && loaded) {
                BannerError(
                    message = errorMessage,
                    onDismiss = viewModel::clearError,
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                )
            }
            when {
                !loaded && loading -> LoadingState(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md))
                !loaded -> ErrorState(
                    description = errorMessage.ifBlank { "Periksa koneksi internet Anda dan coba lagi." },
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refresh()
                    },
                )
                else -> PullToRefreshBox(isRefreshing = loading, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (rows.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Belum ada jurnal hari ini",
                                    description = "Jadwal atau sesi jurnal untuk tanggal ini masih kosong.",
                                    icon = Icons.Rounded.EventBusy,
                                    actionLabel = "Muat ulang",
                                    onAction = viewModel::refresh,
                                    modifier = Modifier.fillParentMaxHeight(0.7f),
                                )
                            }
                        } else {
                            items(rows, key = { it.key }) { row ->
                                if (isStudent) {
                                    StudentRowCard(
                                        row = row,
                                        onHadiri = { methodRow = row },
                                        onIsiJurnal = { onOpenForm(row.plotId, row.timeLabel) },
                                    )
                                } else {
                                    TeacherRowCard(
                                        row = row,
                                        onMulai = { startRow = row },
                                        onDetail = { onOpenDetail(row.attendanceId ?: 0, row.createdAt, row.timeLabel) },
                                        onIsiJurnal = { onOpenForm(row.plotId, row.timeLabel) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    methodRow?.let { row ->
        val options = JurnalRules.methodOptions(row, LocalTime.now())
        AppBottomSheet(title = "Pilih Metode Presensi", onDismiss = { methodRow = null }) {
            Column(
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    "Silakan pilih metode yang ingin digunakan untuk presensi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                if (options.manual) {
                    AppButton(
                        text = "Verifikasi Jurnal",
                        onClick = {
                            methodRow = null
                            onOpenVerifikasi(row.attendanceId ?: 0, "${row.subjectName} · ${row.className}".trimEnd(' ', '·'))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (options.qr) {
                    AppButton(
                        text = "Scan QR",
                        variant = ButtonVariant.Outlined,
                        onClick = {
                            methodRow = null
                            scope.launch {
                                when (val result = QrScanner.scan(context)) {
                                    is QrScanner.Result.Scanned -> viewModel.submitQr(result.raw)
                                    QrScanner.Result.Cancelled -> Toast.makeText(context, "QR Code dibatalkan", Toast.LENGTH_SHORT).show()
                                    is QrScanner.Result.Failed -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        "Scan QR hanya tersedia saat jam pelajaran ini berlangsung.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    startRow?.let { row ->
        AppDialog(
            onDismiss = { startRow = null },
            title = "Mulai kelas",
            body = "Presensi Anda sebagai pengajar akan dicatat untuk ${row.subjectName} ${row.className} (${row.timeLabel}). Mulai sekarang?",
            primaryButtonText = "Ya, Mulai",
            onPrimaryClick = {
                startRow = null
                viewModel.startClass(row)
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { startRow = null },
        )
    }

    if (busy) AppLoadingDialog(message = "memproses")

    notice?.let { current ->
        AppDialog(
            onDismiss = viewModel::dismissNotice,
            title = current.title,
            body = current.message,
            primaryButtonText = if (current.openRow != null) "Ok" else "Tutup",
            onPrimaryClick = {
                viewModel.dismissNotice()
                current.openRow?.let { onOpenDetail(it.attendanceId ?: 0, it.createdAt, it.timeLabel) }
            },
            dismissible = current.openRow == null,
        )
    }
}

// ---- Rows ----

@Composable
private fun StudentRowCard(row: JurnalRow, onHadiri: () -> Unit, onIsiJurnal: () -> Unit) {
    val ui = JurnalRules.studentUi(row)
    RowCard(row) {
        if (row.status == JurnalRules.EMPTY) StatusBadge("Kosong", BadgeTone.Neutral)
        ui.statusText?.let { StatusBadge(it, toneOf(it)) }
        if (ui.presentLabel) StatusBadge("Anda Hadir Dikelas Ini", BadgeTone.Success)
        when (ui.action) {
            JurnalRules.StudentAction.HADIRI -> AppButton(text = "Hadiri kelas", onClick = onHadiri, modifier = Modifier.fillMaxWidth())
            JurnalRules.StudentAction.ISI_JURNAL ->
                AppButton(text = "Isi Jurnal", onClick = onIsiJurnal, variant = ButtonVariant.Tonal, modifier = Modifier.fillMaxWidth())
            JurnalRules.StudentAction.NONE -> Unit
        }
    }
}

@Composable
private fun TeacherRowCard(row: JurnalRow, onMulai: () -> Unit, onDetail: () -> Unit, onIsiJurnal: () -> Unit) {
    RowCard(row) {
        if (row.status.isNotBlank()) StatusBadge(row.status, toneOf(row.status))
        when (JurnalRules.teacherAction(row)) {
            JurnalRules.TeacherAction.ISI_JURNAL ->
                AppButton(text = "Isi Jurnal", onClick = onIsiJurnal, variant = ButtonVariant.Tonal, modifier = Modifier.fillMaxWidth())
            JurnalRules.TeacherAction.DETAIL ->
                AppButton(text = "Detail", onClick = onDetail, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            JurnalRules.TeacherAction.MULAI_KELAS -> AppButton(text = "Mulai kelas", onClick = onMulai, modifier = Modifier.fillMaxWidth())
            JurnalRules.TeacherAction.NONE -> Unit
        }
    }
}

@Composable
private fun RowCard(row: JurnalRow, actions: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AppCard(variant = CardVariant.Elevated) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(scheme.surfaceContainerLow, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (row.iconUrl.isNotBlank()) {
                        AsyncImage(model = resolveAssetUrl(row.iconUrl), contentDescription = null, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = scheme.primary)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                    Text(
                        row.subjectName.ifBlank { "Jam Pelajaran" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = listOf(row.teacherName, row.className).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(
                    row.timeLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.background(scheme.surfaceContainerLow, RoundedCornerShape(8.dp)).padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
            actions()
        }
    }
}

private fun toneOf(status: String): BadgeTone = when (status) {
    JurnalRules.SESSION_DONE -> BadgeTone.Success
    JurnalRules.SESSION_ASSIGNMENT -> BadgeTone.Warning
    JurnalRules.SESSION_NOT_DONE -> BadgeTone.Error
    else -> BadgeTone.Neutral
}
