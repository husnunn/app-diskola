package id.diskola.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing

/** One choice in a select. [id] is null for "no value" rows such as the filters' "Semua". */
data class SelectOption(val id: Int?, val label: String)

/**
 * The app's select sheet: a search field on top that filters the (in-memory) [options] as you type,
 * then the list with the current choice checked. Used by [SimpleDropdown] and the Kelas/Mapel
 * filters. For server-paged lists use [PagedPickerSheet] instead — this one never fetches.
 * Search state lives here, so every opening starts with an empty query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableOptionSheet(
    title: String,
    options: List<SelectOption>,
    selectedId: Int?,
    onSelect: (SelectOption) -> Unit,
    onDismiss: () -> Unit,
    searchPlaceholder: String = "Cari",
    emptyText: String = "Data tidak ditemukan",
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(options, query) {
        val needle = query.trim()
        if (needle.isEmpty()) options else options.filter { it.label.contains(needle, ignoreCase = true) }
    }

    AppBottomSheet(
        title = title,
        onDismiss = onDismiss,
        // Open straight at content height: half-expanded would hide the lower part of a long list off-screen.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = ScreenHorizontalPadding)) {
            AppTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = searchPlaceholder,
                trailing = {
                    if (query.isEmpty()) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (visible.isEmpty()) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xl),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = Spacing.sm),
                ) {
                    itemsIndexed(visible, key = { index, option -> "${option.id}_$index" }) { _, option ->
                        OptionRow(
                            label = option.label,
                            selected = option.id == selectedId,
                            onClick = { onSelect(option) },
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}
