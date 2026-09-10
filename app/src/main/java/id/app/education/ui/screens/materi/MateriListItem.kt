package id.app.education.ui.screens.materi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.CardVariant
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Spacing

/** Compose port of `item_materi.xml` + `MateriAdapter.bind()`. */
@Composable
fun MateriListItem(
    item: MateriItem,
    isStudent: Boolean,
    onMoreClick: (MateriItem) -> Unit,
    onItemClick: (MateriItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        variant = CardVariant.Elevated,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick(item) },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconUrl = item.subject?.icon_image.orEmpty()
                AsyncImage(
                    model = iconUrl.ifBlank { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(DiskolaExtraShapes.iconBox),
                )
                Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                    Text(
                        text = item.name.ifBlank { "Tanpa Judul" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                    Text(
                        text = item.created_at_label.ifBlank { "-" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!isStudent) {
                    IconButton(onClick = { onMoreClick(item) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.subject?.name.orEmpty().ifBlank { "Mapel" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(DiskolaExtraShapes.chipBadge)
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = Spacing.md, vertical = 5.dp),
                )
                Text(
                    text = item.teacher?.name.orEmpty().ifBlank { "-" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }
        }
    }
}
