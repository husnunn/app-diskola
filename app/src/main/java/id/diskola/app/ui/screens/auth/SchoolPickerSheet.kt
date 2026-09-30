package id.diskola.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.SchoolItem
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/**
 * `ListSekolahPage` (doc `02-auth-login-sesi.md` §4.2): local-only search (decision Q3 — the query
 * only filters what's already cached, never a server `name` param), 500ms debounce, infinite
 * scroll + pull-to-refresh both driven by `LoginViewModel`/`SchoolRepository`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolPickerSheet(
    schools: List<SchoolItem>,
    selected: SchoolItem?,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (SchoolItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(query) {
        kotlinx.coroutines.delay(500)
        onQueryChange(query)
    }

    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            schools.isNotEmpty() && lastVisible >= schools.size - 3
        }
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd) onLoadMore()
    }

    AppBottomSheet(title = "Pilih Sekolah", onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md)) {
            AppTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Cari sekolah",
                trailing = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.md))
        }

        if (loading && schools.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Loading...", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = Spacing.sm))
            }
        } else if (schools.isEmpty()) {
            EmptyState(title = "Sekolah tidak ditemukan", description = "Coba kata kunci lain.")
        } else {
            PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.height(420.dp)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {
                    items(schools, key = { it.id }) { school ->
                        SchoolListRow(school = school, isSelected = school.uuid == selected?.uuid, onClick = { onSelect(school) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolListRow(school: SchoolItem, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.iconBox),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = school.name.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
            Text(school.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            school.city_name?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}
