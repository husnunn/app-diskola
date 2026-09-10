package id.app.education.ui.screens.notifikasi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.dataclass.mock.NotificationItem
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.PillTabRow
import id.app.education.ui.components.iconFor
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.extendedColors
import id.app.education.viewmodel.NotifikasiViewModel

@Composable
fun NotifikasiScreen(
    onBack: () -> Unit,
    onOpenDetail: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotifikasiViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }
    val unreadCount = items.count { it.unread }
    val visibleItems = if (tabIndex == 1) items.filter { it.unread } else items

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(MaterialTheme.extendedColors.let { androidx.compose.ui.graphics.Brush.verticalGradient(listOf(it.brandGradientDark, it.brandGradientLight)) }),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text("Notifikasi", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                Text("$unreadCount belum dibaca", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            PillTabRow(options = listOf("Semua", "Belum dibaca"), selectedIndex = tabIndex, onSelect = { tabIndex = it })
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(visibleItems, key = { it.id }) { item ->
                NotificationRow(item = item, onClick = { onOpenDetail(item) })
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Memuat notifikasi lama…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.unread) scheme.surfaceContainerLow else scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).background(scheme.surfaceContainerLowest, DiskolaExtraShapes.iconBox),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(iconFor(item.icon), contentDescription = null, tint = scheme.primary)
            }
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.unread) {
                        Box(modifier = Modifier.size(8.dp).background(scheme.primary, CircleShape))
                    }
                }
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.size(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.timeLabel, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    item.actionLabel?.let { label ->
                        Spacer(modifier = Modifier.size(Spacing.sm))
                        Box(
                            modifier = Modifier.background(scheme.secondaryContainer, DiskolaExtraShapes.chipBadge).padding(horizontal = Spacing.sm, vertical = 2.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}
