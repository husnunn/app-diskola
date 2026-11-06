package id.diskola.app.ui.screens.magang

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.ui.components.ProofImage
import id.diskola.app.ui.components.openLink
import id.diskola.app.ui.components.ImagePreviewDialog
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.components.ErrorText
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.components.KeepNewestVisibleEffect
import id.diskola.app.ui.components.LeaveDetailContent
import id.diskola.app.ui.components.LeaveDetailUi
import id.diskola.app.ui.components.LeaveFact
import id.diskola.app.ui.components.LeaveProofUi
import id.diskola.app.ui.components.ProofFileStatus
import id.diskola.app.ui.components.LeaveTypeIcon
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.PillTabRow
import id.diskola.app.ui.components.SkeletonVariant
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.MagangRules
import id.diskola.app.utils.MagangRules.Leave
import id.diskola.app.utils.MagangRules.LeaveField
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.utils.isImageFile
import id.diskola.app.viewmodel.MagangIzinAddViewModel
import id.diskola.app.viewmodel.MagangViewModel
import id.diskola.app.viewmodel.ProofFileViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FILTERS = listOf("Semua" to null, "Sakit" to MagangRules.TYPE_SAKIT, "Izin" to MagangRules.TYPE_IZIN)
private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))

private fun monthIndex(month: YearMonth): Int = month.year * 12 + (month.monthValue - 1)

private fun monthOf(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

/** Every izin the student has, from both lists, as the screens below show them. */
@Composable
private fun rememberLeaves(shared: MagangViewModel): List<Leave> {
    val schedules by shared.schedules.collectAsStateWithLifecycle()
    val attendance by shared.attendance.collectAsStateWithLifecycle()
    return remember(schedules, attendance) {
        MagangRules.mergeLeaves(MagangRules.leavesFromAttendance(attendance), MagangRules.leavesFromSchedules(schedules))
    }
}

// ---- Daftar ----

/** "Pengajuan Izin" (doc `07` §11.6): one month at a time, optionally one type; "Ajukan Izin" opens the form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagangIzinListScreen(
    shared: MagangViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenDetail: (leaveId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaves = rememberLeaves(shared)
    val scheduleLoaded by shared.loaded.collectAsStateWithLifecycle()
    val attendanceLoaded by shared.attendanceLoaded.collectAsStateWithLifecycle()
    val loading by shared.loading.collectAsStateWithLifecycle()
    val scheduleError by shared.errorMessage.collectAsStateWithLifecycle()
    val attendanceError by shared.attendanceError.collectAsStateWithLifecycle()

    var month by rememberSaveable { mutableIntStateOf(monthIndex(YearMonth.now())) }
    var type by rememberSaveable { mutableStateOf<String?>(null) }

    val known = scheduleLoaded || attendanceLoaded
    val error = attendanceError.ifBlank { scheduleError }
    val contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.sm, bottom = Spacing.xl)

    DetailScaffold(
        title = "Pengajuan Izin",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            ) {
                AppButton(text = "Ajukan Izin", onClick = onAdd, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PillTabRow(
                options = FILTERS.map { it.first },
                selectedIndex = FILTERS.indexOfFirst { it.second == type }.coerceAtLeast(0),
                onSelect = { type = FILTERS[it].second },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
            )
            MonthNavigator(
                month = monthOf(month),
                onPrev = { month -= 1 },
                onNext = { month += 1 },
            )
            if (error.isNotBlank() && known) {
                BannerError(
                    message = error,
                    onDismiss = {
                        shared.clearError()
                        shared.clearAttendanceError()
                    },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                )
            }
            when {
                !known && loading -> LoadingState(padding = contentPadding, variant = SkeletonVariant.ListRow)
                !known -> ErrorState(
                    description = error.ifBlank { "Periksa koneksi internet Anda dan coba lagi." },
                    onRetry = {
                        shared.clearError()
                        shared.clearAttendanceError()
                        shared.refresh()
                    },
                )
                else -> {
                    val shown = MagangRules.leavesIn(leaves, monthOf(month), type)
                    val listState = rememberLazyListState()
                    KeepNewestVisibleEffect(listState, shown.firstOrNull()?.id)
                    PullToRefreshBox(isRefreshing = loading, onRefresh = shared::refresh, modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = contentPadding,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (shown.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = "Belum ada Pengajuan Izin",
                                        description = "",
                                        icon = Icons.Rounded.EventNote,
                                        modifier = Modifier.fillParentMaxHeight(0.6f),
                                    )
                                }
                            } else {
                                items(shown, key = { it.id }) { leave -> LeaveCard(leave, onClick = { onOpenDetail(leave.id) }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Previous month, the month, next month; the label is not a button (legacy had only the two arrows). */
@Composable
internal fun MonthNavigator(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya") }
        Text(
            MONTH_LABEL.format(month),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya") }
    }
}

@Composable
internal fun LeaveCard(leave: Leave, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    AppCard(modifier = modifier.clip(DiskolaExtraShapes.card).clickable(onClick = onClick), variant = CardVariant.Elevated) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            LeaveTypeIcon(leave.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    MagangRules.typeLabel(leave.type),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Text(MagangRules.dateLabel(leave.date), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                if (leave.companyName.isNotBlank()) {
                    Text(leave.companyName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (leave.note.isNotBlank()) {
                    Text(leave.note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---- Ajukan ----

/** "Ajukan Izin": for today's schedule only. A banner says when none can be filed (already filed, or nothing the server allows). */
@Composable
fun MagangIzinAddScreen(
    shared: MagangViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MagangIzinAddViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val schedules by shared.schedules.collectAsStateWithLifecycle()
    val leaves = rememberLeaves(shared)

    val type by viewModel.type.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val proof by viewModel.proof.collectAsStateWithLifecycle()
    val processing by viewModel.processing.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val invalid by viewModel.invalid.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val submittable = MagangRules.submittableToday(schedules, today)
    val banner = MagangRules.leaveBanner(MagangRules.todayLeave(leaves, today), submittable)

    var chosenId by rememberSaveable { mutableLongStateOf(0L) }
    val scheduleId = if (banner != null) 0L else submittable.firstOrNull { it.id == chosenId }?.id ?: submittable.firstOrNull()?.id ?: 0L

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> viewModel.onFilePicked(uri) }
    LaunchedEffect(done) {
        if (done) {
            Toast.makeText(context, "Pengajuan berhasil dikirim", Toast.LENGTH_SHORT).show()
            shared.refresh()
            onDone()
        }
    }

    val locked = banner != null || submitting || processing
    DetailScaffold(
        title = "Ajukan Izin",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            // The reason of a failed send sits above the button: at the top of the form it was out of sight whenever the form had scrolled.
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                if (errorMessage.isNotBlank()) {
                    BannerError(
                        message = errorMessage,
                        onDismiss = viewModel::clearError,
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
                    )
                }
                Box(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md)) {
                    AppButton(
                        text = "Kirim",
                        onClick = { viewModel.submit(scheduleId) },
                        enabled = !locked,
                        loading = submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
            banner?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error) }

            if (submittable.size > 1 && banner == null) {
                FormCard {
                    FieldLabel("Pilih Jadwal", required = true)
                    submittable.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !locked) { chosenId = item.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = scheduleId == item.id, onClick = { chosenId = item.id }, enabled = !locked)
                            Text(
                                MagangRules.scheduleChoiceLabel(item),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (invalid?.field == LeaveField.Schedule) ErrorText(invalid!!.message)
                }
            }

            FormCard {
                FieldLabel("Jenis Pengajuan", required = true)
                listOf(MagangRules.TYPE_SAKIT to "Sakit", MagangRules.TYPE_IZIN to "Izin").forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(enabled = !locked) { viewModel.onTypeChange(value) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = type == value, onClick = { viewModel.onTypeChange(value) }, enabled = !locked)
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (invalid?.field == LeaveField.Type) ErrorText(invalid!!.message)
            }

            FormCard {
                FieldLabel("Upload Surat", required = true)
                AppButton(
                    text = if (processing) "Memproses gambar…" else proof?.displayName ?: "Belum ada file dipilih",
                    onClick = { picker.launch(arrayOf("image/*")) },
                    enabled = !locked,
                    variant = ButtonVariant.Outlined,
                    leadingIcon = { Icon(Icons.Rounded.AttachFile, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                proof?.let {
                    AsyncImage(
                        model = it.file,
                        contentDescription = "Bukti terlampir",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(DiskolaExtraShapes.card),
                    )
                }
                if (invalid?.field == LeaveField.File) ErrorText(invalid!!.message) else {
                    Text(
                        "Upload foto bukti surat dokter / surat pendukung",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FormCard {
                FieldLabel("Keterangan", required = true, counter = "${note.trim().length} / ${MagangRules.LEAVE_NOTE_MAX}")
                AppTextField(
                    value = note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = "Isi keterangan izin",
                    minLines = 3,
                    enabled = !locked,
                    errorText = invalid?.takeIf { it.field == LeaveField.Note }?.message,
                    followCursor = true,
                )
            }
        }
    }
}

// ---- Detail ----

/** "Detail Pengajuan"; legacy had a "Hapus Pengajuan" button that it always hid, so there is none here. Drawn by the shared [LeaveDetailContent]. */
@Composable
fun MagangIzinDetailScreen(
    leaveId: Long,
    shared: MagangViewModel,
    onBack: () -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    proofViewModel: ProofFileViewModel = hiltViewModel(),
) {
    val leave = rememberLeaves(shared).firstOrNull { it.id == leaveId }
    val opening by proofViewModel.opening.collectAsStateWithLifecycle()
    val proofError by proofViewModel.error.collectAsStateWithLifecycle()

    DetailScaffold(title = "Detail Pengajuan", onBack = onBack, modifier = modifier) { padding ->
        if (leave == null) {
            EmptyState(
                title = "Pengajuan tidak ditemukan",
                description = "Pengajuan ini tidak ada lagi di daftar. Kembali dan muat ulang.",
                icon = Icons.Rounded.EventNote,
                actionLabel = "Kembali",
                onAction = onBack,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            val url = resolveAssetUrl(leave.file)
            val ui = LeaveDetailUi(
                type = leave.type,
                typeLabel = MagangRules.typeLabel(leave.type),
                dateLabel = MagangRules.dateLabel(leave.date),
                facts = buildList {
                    if (leave.companyName.isNotBlank()) add(LeaveFact("Perusahaan", leave.companyName))
                    if (leave.note.isNotBlank()) add(LeaveFact("Keterangan", leave.note))
                },
                proof = leave.file.takeIf { it.isNotBlank() }?.let {
                    LeaveProofUi(
                        url = url,
                        fileName = it.substringBefore('?').substringAfterLast('/').takeIf { name -> name.isNotBlank() } ?: "Lihat file bukti",
                        isImage = isImageFile(it),
                    )
                },
            )
            LeaveDetailContent(
                ui = ui,
                onOpenFile = {
                    proofViewModel.open(url, ui.proof?.fileName ?: "bukti.pdf") { file -> onOpenPdf(file.absolutePath, "File Bukti") }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
    ProofFileStatus(opening = opening, error = proofError, onDismissError = proofViewModel::dismissError)
}
