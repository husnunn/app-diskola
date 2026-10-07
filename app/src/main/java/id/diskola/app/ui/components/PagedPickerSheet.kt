package id.diskola.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing

data class PickerOption(val id: Int, val title: String, val subtitle: String = "")

/**
 * Searchable, endlessly-scrolling choice sheet — [SimpleDropdown] has neither search nor paging,
 * which the Poin "jenis" lists (server-paged, 20 at a time) need. Searching is the caller's job
 * ([onQueryChange]); this only renders what it is given and reports reaching the end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagedPickerSheet(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    options: List<PickerOption>,
    selectedId: Int?,
    loading: Boolean,
    onSelect: (PickerOption) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    searchPlaceholder: String = "Cari",
    emptyText: String = "Data tidak ditemukan",
) {
    AppBottomSheet(
        title = title,
        onDismiss = onDismiss,
        // Open straight at content height: half-expanded would hide the lower part of a long list off-screen.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = ScreenHorizontalPadding)) {
            AppTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = searchPlaceholder,
                trailing = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (loading) {
                Text(
                    "sedang mengambil data",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }

            val listState = rememberLazyListState()
            val reachedEnd by remember(options.size) {
                derivedStateOf {
                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    options.isNotEmpty() && last >= options.size - 3
                }
            }
            LaunchedEffect(reachedEnd) { if (reachedEnd) onLoadMore() }

            if (options.isEmpty() && !loading) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xl),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = Spacing.sm),
                ) {
                    items(options, key = { it.id }) { option ->
                        PickerRow(option = option, selected = option.id == selectedId, onClick = { onSelect(option) })
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Composable
private fun PickerRow(option: PickerOption, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(option.title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            if (option.subtitle.isNotBlank()) {
                Text(option.subtitle, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary)
    }
}
