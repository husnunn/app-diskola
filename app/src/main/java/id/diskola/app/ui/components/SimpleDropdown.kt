package id.diskola.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A single value picked from a short, server-provided option list (Kelas, Hari, Mapel, Jenjang,
 * Jurusan — the upload forms' cascading dropdowns). Same tap-target-into-sheet pattern as the
 * login flow's "Pilih Sekolah" field (`LoginScreen`/`SchoolPickerSheet`): a read-only [AppTextField]
 * opens a [SearchableOptionSheet] (search field + list), rather than a Material "exposed dropdown
 * menu" — one select style for the whole app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(
    options: List<Pair<Int, String>>,
    selected: Int?,
    placeholder: String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    /** Off for a field that cannot be used yet (e.g. Mapel before Kelas and Hari are chosen) or a read-only detail screen. */
    enabled: Boolean = true,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second

    AppTextField(
        value = selectedLabel.orEmpty(),
        onValueChange = {},
        label = label,
        placeholder = placeholder,
        readOnly = true,
        enabled = enabled,
        onClick = { if (enabled) sheetVisible = true },
        trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier,
    )

    if (sheetVisible) {
        SearchableOptionSheet(
            title = placeholder,
            options = options.map { (id, text) -> SelectOption(id, text) },
            selectedId = selected,
            onSelect = { option ->
                option.id?.let(onSelected)
                sheetVisible = false
            },
            onDismiss = { sheetVisible = false },
            searchPlaceholder = "Cari ${placeholder.removePrefix("Pilih ").lowercase()}",
        )
    }
}
