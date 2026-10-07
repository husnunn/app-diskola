package id.diskola.app.ui.screens.tugas

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.HomeworkCollected
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.SearchableOptionSheet
import id.diskola.app.ui.components.SelectOption
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.viewmodel.TugasGuruViewModel
import id.diskola.app.viewmodel.TugasPenilaianViewModel

private val TAB_LABELS = listOf("List Tugas", "Penilaian")

/** `HomeworkTeacherPage` shell (doc §6.3/§6.4) — "List Tugas" (Kelas/Mapel filters, FAB create,
 * kebab Edit/Hapus) + "Penilaian" (no filters). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasGuruScreen(
    onBack: () -> Unit,
    onOpenTugas: (HomeworkTable) -> Unit,
    onUpload: () -> Unit,
    onEdit: (HomeworkTable) -> Unit,
    onOpenScoredGroup: (HomeworkCollected) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TugasGuruViewModel = hiltViewModel(),
    penilaianViewModel: TugasPenilaianViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    DetailScaffold(title = "Tugas", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UnderlineTabRow(
                options = TAB_LABELS,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
            )
            when (selectedTab) {
                0 -> ListTugasTab(viewModel, onOpenTugas, onUpload, onEdit)
                else -> PenilaianTab(penilaianViewModel, onOpenScoredGroup)
            }
        }
    }
}

private enum class GuruFilter { Kelas, Mapel }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTugasTab(
    viewModel: TugasGuruViewModel,
    onOpenTugas: (HomeworkTable) -> Unit,
    onUpload: () -> Unit,
    onEdit: (HomeworkTable) -> Unit,
) {
    val items by viewModel.tugasList.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val subjectFilter by viewModel.subjectFilter.collectAsStateWithLifecycle()
    val classFilter by viewModel.classFilter.collectAsStateWithLifecycle()
    val loading by viewModel.listLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var openSheet by remember { mutableStateOf<GuruFilter?>(null) }
    var kebabFor by remember { mutableStateOf<HomeworkTable?>(null) }
    var deleteFor by remember { mutableStateOf<HomeworkTable?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilterField(
                    label = "KELAS",
                    value = classes.find { it.id == classFilter }?.let { classLabel(it.grade, it.name) } ?: "Semua",
                    onClick = { openSheet = GuruFilter.Kelas },
                    modifier = Modifier.weight(1f),
                )
                FilterField(
                    label = "MATA PELAJARAN",
                    value = subjects.find { it.id == subjectFilter?.toLong() }?.name ?: "Semua",
                    onClick = { openSheet = GuruFilter.Mapel },
                    modifier = Modifier.weight(1f),
                )
            }

            if (errorMessage.isNotBlank()) {
                BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
            }

            if (items.isEmpty() && !loading) {
                EmptyState(
                    title = "Belum terdapat tugas",
                    description = "Silahkan buat tugas untuk siswa",
                    icon = Icons.Rounded.Assignment,
                )
            } else {
                PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(items, key = { it.id }) { item ->
                            TugasGuruListItem(item = item, onClick = { onOpenTugas(item) }, onMoreClick = { kebabFor = item })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onUpload,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = DiskolaExtraShapes.iconBox,
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.xl),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text("Tambah", modifier = Modifier.padding(start = Spacing.sm))
        }
    }

    openSheet?.let { filter ->
        val title = if (filter == GuruFilter.Kelas) "Pilih Kelas" else "Pilih Mata Pelajaran"
        val options = buildList {
            add(SelectOption(null, "Semua"))
            if (filter == GuruFilter.Kelas) {
                classes.sortedWith(compareBy({ it.grade }, { it.name.lowercase() })).forEach { add(SelectOption(it.id, classLabel(it.grade, it.name))) }
            } else {
                subjects.sortedBy { it.name.lowercase() }.forEach { add(SelectOption(it.id.toInt(), it.name)) }
            }
        }
        SearchableOptionSheet(
            title = title,
            options = options,
            selectedId = if (filter == GuruFilter.Kelas) classFilter else subjectFilter,
            onSelect = { option ->
                if (filter == GuruFilter.Kelas) viewModel.setClassFilter(option.id) else viewModel.setSubjectFilter(option.id)
                openSheet = null
            },
            onDismiss = { openSheet = null },
            searchPlaceholder = if (filter == GuruFilter.Kelas) "Cari kelas" else "Cari mata pelajaran",
        )
    }

    kebabFor?.let { item ->
        AppBottomSheet(title = item.title.ifBlank { "Tugas" }, onDismiss = { kebabFor = null }) {
            Column {
                KebabRow(icon = Icons.Rounded.Edit, label = "Edit Tugas", destructive = false) {
                    kebabFor = null
                    onEdit(item)
                }
                KebabRow(icon = Icons.Rounded.Delete, label = "Hapus Tugas", destructive = true) {
                    kebabFor = null
                    deleteFor = item
                }
            }
        }
    }

    deleteFor?.let { item ->
        AppDialog(
            onDismiss = { deleteFor = null },
            title = "Hapus Tugas",
            body = "Anda yakin akan menghapus tugas?",
            primaryButtonText = "Hapus",
            onPrimaryClick = {
                viewModel.deleteTugas(item.id.toInt())
                deleteFor = null
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { deleteFor = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PenilaianTab(viewModel: TugasPenilaianViewModel, onOpenScoredGroup: (HomeworkCollected) -> Unit) {
    val items by viewModel.scoredGroups.collectAsStateWithLifecycle()
    val loading by viewModel.listLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    if (errorMessage.isNotBlank()) {
        BannerError(message = errorMessage, onDismiss = { viewModel.clearError() }, modifier = Modifier.padding(horizontal = ScreenHorizontalPadding))
    }

    if (items.isEmpty() && !loading) {
        EmptyState(
            title = "Belum terdapat penilaian",
            description = "Belum ada tugas yang dikumpulkan siswa",
            icon = Icons.Rounded.Assignment,
        )
    } else {
        PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items, key = { it.id }) { item -> TugasPenilaianListItem(item = item, onClick = { onOpenScoredGroup(item) }) }
            }
        }
    }
}

@Composable
private fun TugasPenilaianListItem(item: HomeworkCollected, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val allScored = item.count_assignment_collected_all == item.count_assignment_collected_scored
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(item.title.ifBlank { "Tanpa judul" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            if (item.upload_at_label.isNotBlank()) {
                Text("Diupload ${item.upload_at_label}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (item.message_label.isNotBlank()) {
                Text(
                    item.message_label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (allScored) MaterialTheme.extendedColors.success else scheme.error,
                )
            }
        }
        Text("Lihat", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = scheme.primary)
    }
}

/** "<grade> - <name>" when grade>0, matching the Materi equivalent's dropdown label rule. */
private fun classLabel(grade: Int, name: String): String = if (grade > 0) "$grade - $name" else name

@Composable
private fun FilterField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.5.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, maxLines = 1)
        }
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun KebabRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, destructive: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (destructive) scheme.error else scheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (destructive) scheme.error else scheme.onSurface,
            modifier = Modifier.padding(start = Spacing.md),
        )
    }
}
