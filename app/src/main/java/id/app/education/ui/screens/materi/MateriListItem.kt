package id.app.education.ui.screens.materi

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
import androidx.compose.material.icons.rounded.AttachFile
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
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Spacing

/** Two-letter initials for the teacher avatar, e.g. "Dedi Kurniawan" -> "DK". */
fun initialsOf(name: String): String = name
    .split(' ')
    .filter { it.isNotBlank() && it.first().isLetter() }
    .take(2)
    .map { it.first().uppercaseChar() }
    .joinToString("")
    .ifBlank { "?" }

/** Moshi leaves the class/major relations untyped; pull a display name out when one is present. */
private fun Any?.relationName(): String? = ((this as? Map<*, *>)?.get("name") as? String)?.takeIf { it.isNotBlank() }

/** Who the material was published to. Null when the payload carries no usable relation. */
fun MateriItem.targetLabel(): String? = school_class.relationName()?.let { "Kelas $it" }
    ?: school_major.relationName()?.let { "Jurusan $it" }
    ?: grade?.takeIf { it != 0 }?.let { "Jenjang $it" }

/** Material card for the per-subject list — leads with the teacher's initials avatar. */
@Composable
fun MateriListItem(
    item: MateriItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val teacher = item.teacher?.name.orEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(scheme.surfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initialsOf(teacher),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                item.name.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
            )
            if (teacher.isNotBlank()) {
                Text(teacher, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            MateriMetaRow(item)
        }
    }
}

/** Teacher's own-material card — no avatar, with an overflow menu for edit/delete. */
@Composable
fun MateriGuruListItem(
    item: MateriItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                item.name.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
            )
            MateriMetaRow(item)
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Opsi materi", tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MateriMetaRow(item: MateriItem) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item.targetLabel()?.let { target ->
            Text(
                target,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .background(scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge)
                    .padding(horizontal = Spacing.sm, vertical = 3.dp),
            )
        }
        if (item.file_name.isNotBlank()) {
            Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                item.file_format.ifBlank { "Berkas" }.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        if (item.created_at_label.isNotBlank()) {
            Text(item.created_at_label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}
