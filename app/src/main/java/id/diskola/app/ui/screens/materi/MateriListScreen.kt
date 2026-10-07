package id.diskola.app.ui.screens.materi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.MateriViewModel

/** Materials inside one subject (`MateriPage`, doc §2.4) — same screen for both roles, only the
 * fetch endpoint differs (`isTeacher`, passed in via the route rather than re-derived here). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriListScreen(
    subjectId: Int,
    subjectName: String,
    isTeacher: Boolean,
    onBack: () -> Unit,
    onOpenMateri: (MateriTable) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val materials by viewModel.materiOfSubject.collectAsStateWithLifecycle()
    val loading by viewModel.materiLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(subjectId, isTeacher) { viewModel.openSubjectMateri(subjectId, isTeacher) }

    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            materials.isNotEmpty() && lastVisible >= materials.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMoreMateri() }

    DetailScaffold(title = subjectName.ifBlank { "Materi" }, onBack = onBack, modifier = modifier) { padding ->
        if (materials.isEmpty() && !loading) {
            EmptyState(
                title = "Belum terdapat materi",
                description = "Silahkan tunggu guru mengupdate materi",
                icon = Icons.Rounded.LibraryBooks,
                modifier = Modifier.padding(padding),
            )
        } else {
            PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refreshMateri() }, modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(materials, key = { it.id }) { item ->
                        MateriListItem(item = item, onClick = { onOpenMateri(item) })
                    }
                }
            }
        }
    }
}
