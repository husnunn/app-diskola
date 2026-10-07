package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import id.diskola.app.repository.LinkPreviewData
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/**
 * Stateless — the caller (`MateriDetailViewModel`, `UploadMateriViewModel`) owns fetching via
 * `LinkPreviewRepository` and just hands the result here. Doc §4.5: failure still shows the raw
 * URL with a broken-image icon rather than hiding the card.
 */
@Composable
fun LinkPreviewCard(
    url: String,
    loading: Boolean,
    data: LinkPreviewData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .background(scheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.padding(Spacing.lg))
                !data?.imageUrl.isNullOrBlank() -> AsyncImage(
                    model = data?.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(104.dp),
                )
                else -> Icon(Icons.Rounded.BrokenImage, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val title = data?.title?.takeIf { it.isNotBlank() }
                if (title != null) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, maxLines = 2)
                }
                Text(url, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = scheme.primary)
        }
    }
}
