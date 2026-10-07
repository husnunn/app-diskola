package id.diskola.app.ui.screens.jurnal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.ScheduleDetailData
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.FieldLabel
import id.diskola.app.ui.components.FormCard
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.JurnalRules
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.EditableStudent
import id.diskola.app.viewmodel.JurnalDetailViewModel

private val TEACHER_SESSION_STATUSES = listOf(JurnalRules.SESSION_DONE, JurnalRules.SESSION_ASSIGNMENT, JurnalRules.SESSION_NOT_DONE)
private val STUDENT_LEGEND = listOf("hadir" to "H", "izin" to "I", "sakit" to "S", "alpha" to "A")

/**
 * "Detail Kelas" (doc `07` §9). A teacher whose class has started edits tujuan, status sesi and each student's
 * H/I/S/A and saves it in one call; everyone else sees it read-only. Leaving with unsaved edits asks first.
 */
@Composable
fun JurnalDetailScreen(
    attendanceId: Int,
    createdAt: String,
    plot: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JurnalDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isStarted by viewModel.isStarted.collectAsStateWithLifecycle()
    val canEdit by viewModel.canEdit.collectAsStateWithLifecycle()
    val objective by viewModel.objective.collectAsStateWithLifecycle()
    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val dirty by viewModel.dirty.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    var editObjective by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(attendanceId) { viewModel.start(attendanceId, createdAt) }

    fun requestClose() {
        if (dirty) confirmDiscard = true else onBack()
    }
    BackHandler(onBack = ::requestClose)

    DetailScaffold(
        title = "Detail Kelas",
        onBack = ::requestClose,
        modifier = modifier,
        actions = {
            if (viewModel.isTeacher && canEdit) {
                TextButton(onClick = viewModel::save, enabled = !loading, modifier = Modifier.heightIn(min = 48.dp)) { Text("Simpan") }
            }
        },
    ) { padding ->
        val current = detail
        when {
            current == null && loadError == null -> LoadingState(modifier = Modifier.padding(padding).padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md))
            current == null -> Unit
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                if (errorMessage.isNotBlank()) BannerError(message = errorMessage, onDismiss = viewModel::clearError)

                SubjectHeader(current)

                FormCard {
                    InfoRow("Pengajar", current.teacher_name)
                    InfoRow("Kelas", listOf(current.class_name, current.school_major_name).filter { it.isNotBlank() }.joinToString(" · "))
                    InfoRow("Jadwal", plot.ifBlank { listOf(current.plot_start_at, current.plot_end_at).filter { it.isNotBlank() }.joinToString(" - ") })
                    InfoRow("Kehadiran", viewModel.breakdownLabel())
                }

                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tujuan Pembelajaran", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        if (canEdit) {
                            TextButton(onClick = { editObjective = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(if (objective.isBlank()) "Isi" else "Ubah")
                            }
                        }
                    }
                    Text(
                        objective.ifBlank { "Belum diisi" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (objective.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }

                FormCard {
                    FieldLabel("Status Pembelajaran", required = viewModel.isTeacher)
                    if (viewModel.isTeacher) {
                        if (!isStarted) {
                            Text(
                                "Mulai kelas dulu (tombol Absensi) sebelum mengisi dan menyimpan status jurnal.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        RadioOptions(
                            options = TEACHER_SESSION_STATUSES,
                            selected = sessionStatus.takeIf { it.isNotBlank() },
                            onSelect = viewModel::onSessionStatus,
                            enabled = canEdit,
                        )
                    } else {
                        Text(sessionStatus.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (current.capture_photo_url.isNotBlank()) {
                    FormCard {
                        FieldLabel("Foto Suasana KBM", required = false)
                        AsyncImage(
                            model = resolveAssetUrl(current.capture_photo_url),
                            contentDescription = "Foto suasana KBM",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(DiskolaExtraShapes.card)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card),
                        )
                    }
                }

                AttendanceList(students = students, editable = canEdit, onStatus = viewModel::onStudentStatus)
            }
        }
    }

    // Load failed: a blocking dialog with retry, as legacy.
    if (loadError != null && detail == null) {
        AppDialog(
            onDismiss = {},
            title = "Jurnal gagal dimuat",
            body = loadError.orEmpty(),
            primaryButtonText = "Coba Lagi",
            onPrimaryClick = viewModel::load,
            secondaryButtonText = "Tutup",
            onSecondaryClick = onBack,
            dismissible = false,
        )
    }

    if (editObjective) {
        var draft by remember { mutableStateOf(objective) }
        AppDialog(
            onDismiss = { editObjective = false },
            title = "Tujuan Pembelajaran",
            body = "",
            primaryButtonText = "Simpan",
            onPrimaryClick = {
                viewModel.onObjectiveSaved(draft)
                editObjective = false
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { editObjective = false },
        ) {
            AppTextField(value = draft, onValueChange = { draft = it }, placeholder = "Tujuan pembelajaran", minLines = 3)
        }
    }

    if (confirmDiscard) {
        AppDialog(
            onDismiss = { confirmDiscard = false },
            title = "Buang perubahan?",
            body = "Perubahan jurnal yang belum disimpan akan hilang.",
            primaryButtonText = "Buang",
            onPrimaryClick = {
                confirmDiscard = false
                viewModel.discard()
                onBack()
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { confirmDiscard = false },
        )
    }

    if (loading && detail != null) AppLoadingDialog(message = "Menyimpan jurnal ...")

    if (saved) {
        AppDialog(
            onDismiss = {},
            title = "Jurnal Berhasil Disimpan",
            body = "Data jurnal dan kehadiran siswa telah tersimpan.",
            primaryButtonText = "Kembali ke Daftar Jadwal",
            onPrimaryClick = {
                viewModel.consumeSaved()
                onSaved()
            },
            dismissible = false,
        )
    }
}

@Composable
private fun SubjectHeader(detail: ScheduleDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            detail.subject_name.ifBlank { "Mata pelajaran" },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (detail.teacher_name.isNotBlank()) {
            Text(detail.teacher_name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AttendanceList(students: List<EditableStudent>, editable: Boolean, onStatus: (studentId: Int, status: String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FormCard {
        Text("Data Kehadiran", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
        Text(
            "H Hadir · I Izin · S Sakit · A Alpha",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerHigh, DiskolaExtraShapes.chipBadge).padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text("Nama Siswa", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text("Status", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.width(STATUS_COLUMN))
        }
        if (students.isEmpty()) {
            Text("Belum ada data kehadiran siswa", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        students.forEachIndexed { index, student ->
            if (index > 0) HorizontalDivider(color = scheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                    Text(student.name, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    JurnalRules.statusSourceLabel(student.source)?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
                StatusSelector(
                    current = student.status,
                    enabled = editable && student.overridable,
                    onSelect = { onStatus(student.id, it) },
                )
            }
        }
    }
}

private val STATUS_COLUMN = 160.dp

/** H / I / S / A chips; a read-only row still shows which one applies, just not tappable. */
@Composable
private fun StatusSelector(current: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.width(STATUS_COLUMN)) {
        STUDENT_LEGEND.forEach { (value, letter) ->
            val selected = current == value
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .heightIn(min = 48.dp)
                    .then(
                        if (enabled) Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(value) }) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .heightIn(min = 32.dp)
                        .background(
                            if (selected) scheme.primary else scheme.surfaceContainerLow,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) scheme.onPrimary else if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
