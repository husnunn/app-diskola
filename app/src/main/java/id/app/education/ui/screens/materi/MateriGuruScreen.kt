package id.app.education.ui.screens.materi

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.ui.components.AppBottomSheet
import id.app.education.ui.components.AppDialog
import id.app.education.ui.components.DetailScaffold
import id.app.education.ui.components.EmptyState
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.MateriViewModel

/** "Materi Saya" — everything the signed-in teacher has uploaded, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriGuruScreen(
    onBack: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenMateri: (MateriItem) -> Unit,
    onUpload: () -> Unit,
    onEdit: (MateriItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val materials by viewModel.materiList.collectAsStateWithLifecycle()
    val subjects by viewModel.teacherSubjects.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()

    var subjectFilter by remember { mutableStateOf<Int?>(null) }
    var classFilter by remember { mutableStateOf<Int?>(null) }
    var openSheet by remember { mutableStateOf<GuruFilter?>(null) }
    var kebabFor by remember { mutableStateOf<MateriItem?>(null) }
    var deleteFor by remember { mutableStateOf<MateriItem?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchTeacherRequirements() }
    LaunchedEffect(subjectFilter, classFilter) {
        viewModel.getMateriTeacher(subjectId = subjectFilter, classId = classFilter)
    }

    DetailScaffold(
        title = "Materi",
        onBack = onBack,
        modifier = modifier,
        actions = {
            Text(
                "Mata Pelajaran",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenSubjects).padding(Spacing.md),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FilterField(
                        label = "KELAS",
                        value = classes.find { it.id == classFilter }?.name ?: "Semua",
                        onClick = { openSheet = GuruFilter.Kelas },
                        modifier = Modifier.weight(1f),
                    )
                    FilterField(
                        label = "MATA PELAJARAN",
                        value = subjects.find { it.id == subjectFilter }?.name ?: "Semua",
                        onClick = { openSheet = GuruFilter.Mapel },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (materials.isEmpty()) {
                    EmptyState(
                        title = "Belum ada materi",
                        description = "Unggah materi pertama Anda lewat tombol Tambah.",
                        icon = Icons.Rounded.UploadFile,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        item {
                            Text(
                                "Materi yang Anda unggah · urut terbaru",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(materials, key = { it.id }) { item ->
                            MateriGuruListItem(
                                item = item,
                                onClick = { onOpenMateri(item) },
                                onMoreClick = { kebabFor = item },
                            )
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
    }

    openSheet?.let { filter ->
        val title = if (filter == GuruFilter.Kelas) "Pilih Kelas" else "Pilih Mata Pelajaran"
        val options = buildList {
            add(null to "Semua")
            if (filter == GuruFilter.Kelas) classes.forEach { add(it.id to it.name) }
            else subjects.forEach { add(it.id to it.name) }
        }
        val selected = if (filter == GuruFilter.Kelas) classFilter else subjectFilter
        AppBottomSheet(title = title, onDismiss = { openSheet = null }) {
            Column {
                options.forEach { (id, label) ->
                    SheetOptionRow(label = label, selected = id == selected) {
                        if (filter == GuruFilter.Kelas) classFilter = id else subjectFilter = id
                        openSheet = null
                    }
                }
            }
        }
    }

    kebabFor?.let { item ->
        AppBottomSheet(title = item.name.ifBlank { "Materi" }, onDismiss = { kebabFor = null }) {
            Column {
                KebabRow(icon = Icons.Rounded.Edit, label = "Edit Materi", destructive = false) {
                    kebabFor = null
                    onEdit(item)
                }
                KebabRow(icon = Icons.Rounded.Delete, label = "Hapus Materi", destructive = true) {
                    kebabFor = null
                    deleteFor = item
                }
            }
        }
    }

    deleteFor?.let { item ->
        AppDialog(
            onDismiss = { deleteFor = null },
            title = "Hapus Materi",
            body = "Anda yakin akan menghapus materi ${item.name}? Materi akan hilang dari daftar siswa.",
            primaryButtonText = "Hapus",
            onPrimaryClick = {
                viewModel.deleteMateri(item.id.toLong())
                deleteFor = null
            },
            secondaryButtonText = "Batal",
            onSecondaryClick = { deleteFor = null },
        )
    }
}

internal enum class GuruFilter { Kelas, Mapel }

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
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                maxLines = 1,
            )
        }
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SheetOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary)
        }
    }
}

@Composable
private fun KebabRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean,
    onClick: () -> Unit,
) {
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
