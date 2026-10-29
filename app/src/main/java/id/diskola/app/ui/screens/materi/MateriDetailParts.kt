package id.diskola.app.ui.screens.materi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.ui.screens.tugas.InfoRow
import id.diskola.app.ui.screens.tugas.NO_VALUE
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors

/**
 * Top card of "Detail Materi": subject tag and title, then who published it, when and to whom.
 * It is the only place the title lives, so the page reads title → facts → content.
 */
@Composable
internal fun MateriHeaderCard(item: MateriTable, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (item.subject_name.isNotBlank()) SubjectTag(item.subject_name)
            Text(
                item.name.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
            )
        }

        HorizontalDivider(color = scheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            TeacherRow(item.teacher_name)
            InfoRow(Icons.Rounded.Event, "Diunggah", item.created_at_label)
            InfoRow(Icons.Rounded.Groups, "Ditampilkan ke", item.targetLabel().orEmpty())
        }
    }
}

/** The subject the material belongs to, as a small tinted tag above the title. */
@Composable
private fun SubjectTag(name: String, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.extendedColors
    Row(
        modifier = modifier
            .background(extended.infoContainer, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = extended.onInfoContainer, modifier = Modifier.size(14.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelLarge,
            color = extended.onInfoContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** An [InfoRow] whose leading slot is the teacher's initials, so the author stands out from the plain facts below it. */
@Composable
private fun TeacherRow(name: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(32.dp).background(scheme.surfaceContainerLow, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialsOf(name), style = MaterialTheme.typography.labelMedium, color = scheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Guru", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            val missing = name.isBlank()
            Text(
                if (missing) NO_VALUE else name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (missing) FontWeight.Normal else FontWeight.Medium),
                color = if (missing) scheme.onSurfaceVariant else scheme.onSurface,
            )
        }
    }
}

/**
 * A titled block of the page: a primary-tinted icon and a heading that is clearly larger than the
 * body text under it, so "Deskripsi", "File Materi" and the rest are told apart from their content.
 */
@Composable
internal fun MateriSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        content()
    }
}
