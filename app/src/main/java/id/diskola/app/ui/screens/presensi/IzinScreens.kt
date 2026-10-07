package id.diskola.app.ui.screens.presensi

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.LeaveRequestTable
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.ErrorText
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.components.PillTabRow
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.IzinAddViewModel
import id.diskola.app.viewmodel.IzinDetailViewModel
import id.diskola.app.viewmodel.IzinListViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FILTERS = listOf("Semua" to null, "Pending" to "pending", "Disetujui" to "approved", "Ditolak" to "rejected")
private val DATE_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))

private fun dateLabel(raw: String): String = runCatching { DATE_LABEL.format(LocalDate.parse(raw.take(10))) }.getOrDefault(raw)

private fun typeLabel(raw: String): String = when (raw.lowercase()) {
    "sakit" -> "Sakit"
    "izin" -> "Izin"
    else -> raw.replaceFirstChar { it.uppercase() }
}

private fun statusBadge(approval: String): Pair<String, BadgeTone> = when (approval.lowercase()) {
    "approved" -> "Disetujui" to BadgeTone.Success
    "rejected" -> "Ditolak" to BadgeTone.Error
    else -> "Menunggu" to BadgeTone.Warning
}

// ---- Daftar ----

/** "Pengajuan Izin" (doc `07` §10.1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinListScreen(
    initialFilter: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenDetail: (String) -> Unit,
    changed: Boolean,
    onChangedHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IzinListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val gate by viewModel.gate.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start(initialFilter) }
    LaunchedEffect(changed) {
        if (changed) {
            viewModel.onChanged()
            onChangedHandled()
        }
    }

    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            items.isNotEmpty() && last >= items.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMore() }

    DetailScaffold(
        title = "Pengajuan Izin",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                AppButton(text = "Ajukan Izin", onClick = onAdd, enabled = gate.block == null, modifier = Modifier.fillMaxWidth())
                gate.block?.let { ErrorText(it.message) }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PillTabRow(
                options = FILTERS.map { it.first },
                selectedIndex = FILTERS.indexOfFirst { it.second == filter }.coerceAtLeast(0),
                onSelect = { viewModel.setFilter(FILTERS[it].second) },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
            )
            if (errorMessage.isNotBlank()) {
                BannerError(
                    message = errorMessage,
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
                )
            }
            PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.sm, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (items.isEmpty()) {
                        if (!loading) item {
                            EmptyState(
                                title = "Belum ada pengajuan",
                                description = "",
                                icon = Icons.Rounded.EventNote,
                                modifier = Modifier.fillParentMaxHeight(0.6f),
                            )
                        }
                    } else {
                        items(items, key = { it.uuid }) { leave -> LeaveCard(leave, onClick = { onOpenDetail(leave.uuid) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveCard(leave: LeaveRequestTable, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (label, tone) = statusBadge(leave.approvalStatus)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                typeLabel(leave.status),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(label = label, tone = tone)
        }
        if (leave.note.isNotBlank()) Text(leave.note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 2)
        Text(dateLabel(leave.date), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
    }
}

// ---- Form ----

/** "Tambah Izin" (doc `07` §10.2). */
@Composable
fun IzinAddScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IzinAddViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val proof by viewModel.proof.collectAsStateWithLifecycle()
    val fileError by viewModel.fileError.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> viewModel.onFilePicked(uri) }
    LaunchedEffect(done) {
        if (done) {
            Toast.makeText(context, "Pengajuan berhasil dikirim", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    val locked = gate.block != null || loading
    DetailScaffold(
        title = "Tambah Izin",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(text = "Kirim", onClick = viewModel::submit, enabled = !locked, loading = loading, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            val banner = gate.block?.message ?: if (gate.rejectedToday) "Pengajuan hari ini ditolak. Anda dapat mengajukan ulang." else null
            banner?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error) }
            if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })

            FormCard {
                FieldLabel("Jenis Pengajuan", required = true)
                listOf("sakit" to "Sakit", "izin" to "Izin").forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(enabled = !locked) { viewModel.onStatusChange(value) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = status == value, onClick = { viewModel.onStatusChange(value) }, enabled = !locked)
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            FormCard {
                FieldLabel("Upload Surat", required = true)
                AppButton(
                    text = proof?.displayName ?: "Belum ada file dipilih",
                    onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
                    enabled = !locked,
                    variant = ButtonVariant.Outlined,
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Rounded.AttachFile, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (fileError != null) ErrorText(fileError!!) else {
                    Text(
                        "Upload file Surat dokter / surat pendukung lainya",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FormCard {
                FieldLabel("Keterangan", required = true, counter = "${note.trim().length} / ${PresensiRules.NOTE_MAX}")
                AppTextField(
                    value = note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = "Isi keterangan izin",
                    enabled = !locked,
                )
            }
        }
    }
}

// ---- Detail ----

/** "Detail Pengajuan" (doc `07` §10.3). */
@Composable
fun IzinDetailScreen(
    uuid: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IzinDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val item by viewModel.item.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    LaunchedEffect(uuid) { viewModel.start(uuid) }
    if (notFound) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Pengajuan tidak ditemukan", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    DetailScaffold(title = "Detail Pengajuan", onBack = onBack, modifier = modifier) { padding ->
        val leave = item
        if (leave != null) {
            val scheme = MaterialTheme.colorScheme
            val (label, tone) = statusBadge(leave.approvalStatus)
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(typeLabel(leave.status), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
                        Text(dateLabel(leave.date), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                    StatusBadge(label = label, tone = tone)
                }
                FormCard {
                    FieldLabel("Keterangan", required = false)
                    Text(leave.note.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
                if (leave.approvalStatus.equals("rejected", ignoreCase = true)) {
                    FormCard {
                        FieldLabel("Alasan Penolakan", required = false)
                        Text(
                            leave.rejectionNote.ifBlank { "Tidak ada keterangan alasan penolakan." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                        )
                    }
                }
                if (leave.fileUrl.isNotBlank()) {
                    FormCard {
                        FieldLabel("File Bukti", required = false)
                        AppButton(
                            text = leave.fileUrl.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "Lihat file bukti",
                            onClick = {
                                val url = resolveAssetUrl(leave.fileUrl)
                                // Only a real http(s) address can be opened; a scheme-less value would resolve to nothing.
                                val opened = url.startsWith("http", ignoreCase = true) &&
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
                                if (!opened) Toast.makeText(context, "Tidak dapat membuka file", Toast.LENGTH_SHORT).show()
                            },
                            variant = ButtonVariant.Outlined,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (leave.reviewedAt.isNotBlank()) {
                    Text("Direview: ${dateLabel(leave.reviewedAt)}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}
