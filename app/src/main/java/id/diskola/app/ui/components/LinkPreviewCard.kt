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
import androidx.compose.material.icons.rounded.ImageNotSupported
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
        val hasImage = !data?.imageUrl.isNullOrBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // a missing image needs only enough room for its caption
                .height(if (loading || hasImage) 104.dp else 72.dp)
                .background(scheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.padding(Spacing.lg))
                hasImage -> AsyncImage(
                    model = data?.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(104.dp),
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Icon(Icons.Rounded.ImageNotSupported, contentDescription = null, tint = scheme.onSurfaceVariant)
                    Text("Tidak ada gambar pratinjau", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
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
