package id.diskola.app.ui.screens.tugas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/** One row in the "Belum Dikerjakan" tab — grey end-date label, or red "Terlambat dari" when
 * overdue (doc §5.4). */
@Composable
fun TugasBacklogItem(item: HomeworkTable, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    TugasCard(onClick = onClick, modifier = modifier) {
        TugasCardHeader(item)
        if (item.end_at_label.isNotBlank()) {
            Text(
                if (item.is_overdue) "Terlambat dari ${item.end_at_label}" else "Berakhir pada ${item.end_at_label}",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.is_overdue) scheme.error else scheme.onSurfaceVariant,
            )
        }
    }
}

/** "Sudah Dikerjakan" tab — server's own `information_label` text, verbatim. */
@Composable
fun TugasDoneItem(item: HomeworkTable, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TugasCard(onClick = onClick, modifier = modifier) {
        TugasCardHeader(item)
        if (item.information_label.isNotBlank()) {
            Text(item.information_label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Nilai" tab — score ring + `information_label`. */
@Composable
fun TugasScoredItem(item: HomeworkTable, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(scheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.score.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = scheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(item.title.ifBlank { "Tanpa judul" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            if (item.information_label.isNotBlank()) {
                Text(item.information_label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/** Teacher's "List Tugas" own-item card — overflow menu for edit/delete instead of a status badge. */
@Composable
fun TugasGuruListItem(item: HomeworkTable, onClick: () -> Unit, onMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(start = Spacing.lg, top = Spacing.lg, end = Spacing.sm, bottom = Spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(item.title.ifBlank { "Tanpa judul" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            if (item.subject_name.isNotBlank()) {
                Text(item.subject_name, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (item.upload_at_label.isNotBlank()) {
                Text("Upload: ${item.upload_at_label}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Opsi tugas", tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TugasCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) { content() }
}

@Composable
private fun TugasCardHeader(item: HomeworkTable) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title.ifBlank { "Tanpa judul" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            if (item.subject_name.isNotBlank()) {
                Text(item.subject_name, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (item.teacher_name.isNotBlank()) {
                Text("Guru · ${item.teacher_name}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        if (item.message_label.isNotBlank()) {
            Text(
                item.message_label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .background(scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge)
                    .padding(horizontal = Spacing.sm, vertical = 3.dp),
            )
        }
    }
}
