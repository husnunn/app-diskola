package id.diskola.app.ui.screens.tugas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.components.fileIcon
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

// Shown wherever the server sent nothing, so a blank field reads as "there is none" rather than a layout bug.
internal const val NO_VALUE = "Tidak tersedia"
internal const val NO_DEADLINE = "Tidak ditentukan"
internal const val NO_DESCRIPTION = "Tidak ada deskripsi"
internal const val NO_QUESTION_FILE = "Tidak ada file soal"
internal const val NO_QUESTION_LINK = "Tidak ada tautan soal"
internal const val NO_ANSWER_FILE = "Tidak ada file jawaban"
internal const val NO_ANSWER_LINK = "Tidak ada tautan jawaban"
internal const val NO_EXPLANATION = "Belum ada pembahasan"

/**
 * Top card of "Detail Tugas": status (students), title, and the facts about the task. Every row is always
 * present; a missing value is spelled out ([NO_VALUE] / [NO_DEADLINE]) instead of the row disappearing.
 */
@Composable
internal fun TugasHeaderCard(item: HomeworkTable, type: Int, isTeacher: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val finished = type != HomeworkTable.TYPE_BACKLOG
    val scored = !isTeacher && type == HomeworkTable.TYPE_SCORED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (!isTeacher) {
                    when {
                        type == HomeworkTable.TYPE_SCORED -> StatusBadge("Sudah dinilai", BadgeTone.Success)
                        finished -> StatusBadge("Sudah dikerjakan", BadgeTone.Success)
                        item.is_overdue -> StatusBadge("Terlambat", BadgeTone.Error)
                        else -> StatusBadge("Belum dikerjakan", BadgeTone.Warning)
                    }
                }
                Text(
                    item.title.ifBlank { "Tanpa judul" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                )
            }
            if (scored) ScoreBadge(score = item.score)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            InfoRow(Icons.Rounded.MenuBook, "Mata Pelajaran", item.subject_name)
            InfoRow(Icons.Rounded.Person, "Guru", item.teacher_name)
            if (isTeacher) InfoRow(Icons.Rounded.Groups, "Kelas", item.class_name)
            InfoRow(
                icon = Icons.Rounded.Event,
                label = "Batas Pengumpulan",
                value = when {
                    item.end_at_label.isBlank() -> ""
                    !isTeacher && !finished && item.is_overdue -> "Terlambat dari ${item.end_at_label}"
                    else -> item.end_at_label
                },
                placeholder = NO_DEADLINE,
                emphasize = !isTeacher && !finished && item.is_overdue && item.end_at_label.isNotBlank(),
            )
            if (isTeacher) {
                InfoRow(Icons.Rounded.Upload, "Diunggah", item.upload_at_label)
            } else if (finished) {
                InfoRow(Icons.Rounded.TaskAlt, "Status Pengumpulan", item.information_label)
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).background(scheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(score.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = scheme.onPrimaryContainer)
        }
        Text("Nilai", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
    }
}

/** Icon + caption + value. A blank [value] shows [placeholder] in the muted colour. */
@Composable
internal fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = NO_VALUE,
    emphasize: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val missing = value.isBlank()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(32.dp).background(scheme.surfaceContainerLow, DiskolaExtraShapes.chipBadge),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(
                if (missing) placeholder else value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (missing) FontWeight.Normal else FontWeight.Medium),
                color = when {
                    emphasize -> scheme.error
                    missing -> scheme.onSurfaceVariant
                    else -> scheme.onSurface
                },
            )
        }
    }
}

/** Section title (+ optional note on the right) over its content, spaced on the shared scale. */
@Composable
internal fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeTone: BadgeTone = BadgeTone.Warning,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(title)
            Spacer(modifier = Modifier.weight(1f))
            badge?.let { StatusBadge(it, badgeTone) }
        }
        content()
    }
}

/** Plain text content (e.g. the description) on the same card surface as the file cards. */
@Composable
internal fun TextCard(text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
    )
}

/** "Nothing here" placeholder: a quiet, borderless-tone row that says what is missing. */
@Composable
internal fun EmptyNote(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(scheme.surfaceContainerLow, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

/**
 * A file attachment: type icon, name (wraps to 2 lines), size, then "read" and "download" actions.
 * Also used by "Detail Materi", which words the download action differently ([downloadLabel]).
 */
@Composable
internal fun TugasFileCard(
    fileName: String,
    fileSize: String,
    loading: Boolean,
    readLabel: String,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    downloadLabel: String = "Download",
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                modifier = Modifier.size(44.dp).background(scheme.primaryContainer, DiskolaExtraShapes.iconBox),
                contentAlignment = Alignment.Center,
            ) {
                Icon(fileIcon(fileName), contentDescription = null, tint = scheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fileSize.isNotBlank()) {
                    Text(fileSize, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppButton(
                text = readLabel,
                onClick = onOpen,
                loading = loading,
                leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = downloadLabel,
                onClick = onDownload,
                variant = ButtonVariant.Outlined,
                leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
