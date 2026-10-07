package id.diskola.app.ui.screens.tugas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.Assignment
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.ScoringFilter
import id.diskola.app.viewmodel.TugasTerkumpulViewModel

private val FILTER_LABELS = mapOf(
    ScoringFilter.SEMUA to "Semua",
    ScoringFilter.BELUM_DINILAI to "Belum Dinilai",
    ScoringFilter.SUDAH_DINILAI to "Sudah Dinilai",
)

/** `HomeworkSubmittedPage` (doc §6.4) — collapsible "Detail" panel + per-student score rows, local
 * Semua/Belum/Sudah Dinilai filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasTerkumpulScreen(
    collectedId: Int,
    onBack: () -> Unit,
    onOpenTugasReadonly: (Int) -> Unit,
    onScoreStudent: (Assignment) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TugasTerkumpulViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val assignments by viewModel.filteredAssignments.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var detailExpanded by remember { mutableStateOf(false) }
    var filterSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(collectedId) { viewModel.load(collectedId) }

    DetailScaffold(title = "Tugas Terkumpul", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (errorMessage.isNotBlank()) {
                BannerError(message = errorMessage, onDismiss = { viewModel.clearError() }, modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm))
            }
            val current = detail
            if (current != null) {
                Column(modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { detailExpanded = !detailExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Detail", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        val rotation by animateFloatAsState(if (detailExpanded) 180f else 0f, label = "detailChevron")
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.graphicsLayer { rotationZ = rotation })
                    }
                    AnimatedVisibility(visible = detailExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(top = Spacing.sm)) {
                            DetailRow("Terkumpul", "${current.count_assignment_collected}/${current.count_assignment_collected_all}")
                            DetailRow("Kelas", current.`class`.name)
                            DetailRow("Mata Pelajaran", current.subject.name)
                            DetailRow("Tugas Diupload", current.upload_at_label)
                            DetailRow("Tugas Berakhir", current.end_at_label)
                            AppButton(
                                text = "Lihat Tugas",
                                onClick = { onOpenTugasReadonly(current.id) },
                                variant = ButtonVariant.Outlined,
                                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.md).clickable { filterSheetOpen = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(FILTER_LABELS[filter].orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(assignments, key = { it.id }) { assignment ->
                    AssignmentScoreRow(
                        assignment = assignment,
                        onOpenReadonly = { detail?.let { onOpenTugasReadonly(it.id) } },
                        onScore = { onScoreStudent(assignment) },
                    )
                }
            }
        }
    }

    if (filterSheetOpen) {
        AppBottomSheet(title = "Filter", onDismiss = { filterSheetOpen = false }) {
            Column {
                ScoringFilter.entries.forEach { option ->
                    Text(
                        FILTER_LABELS[option].orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (option == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setFilter(option); filterSheetOpen = false }
                            .padding(vertical = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value.ifBlank { "-" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AssignmentScoreRow(assignment: Assignment, onOpenReadonly: () -> Unit, onScore: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val scored = assignment.scored == 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(if (scored) scheme.primaryContainer else scheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (scored) assignment.score.toString() else "--",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = if (scored) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(assignment.student.name.ifBlank { "Tanpa nama" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = scheme.onSurface)
                if (assignment.upload_at.isNotBlank()) {
                    Text("Dikumpulkan ${assignment.upload_at}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                if (!scored) {
                    Text("Belum dinilai", style = MaterialTheme.typography.labelSmall, color = scheme.error)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppButton(text = "Lihat Jawaban", onClick = onOpenReadonly, variant = ButtonVariant.Outlined, modifier = Modifier.weight(1f))
            AppButton(text = if (scored) "Ubah Nilai" else "Beri Nilai", onClick = onScore, modifier = Modifier.weight(1f))
        }
    }
}
