package id.diskola.app.ui.screens.tugas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.UnderlineTabRow
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.TugasSiswaViewModel

private val TAB_LABELS = listOf("Belum Dikerjakan", "Sudah Dikerjakan", "Nilai")

/** `HomeWorkPage`'s 3-tab shell (doc `05-pembelajaran-materi-tugas.md` §5.3/§5.5). Each tab pages
 * independently; list-fetch failures show a banner (decision: deviate from the legacy app, which
 * is silent here). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasSiswaScreen(
    onBack: () -> Unit,
    onOpenTugas: (HomeworkTable) -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TugasSiswaViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var scoreDetailId by remember { mutableStateOf<Int?>(null) }
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.ensureBacklog()
        viewModel.ensureDone()
        viewModel.ensureScored()
    }

    DetailScaffold(title = "Tugas", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            UnderlineTabRow(
                options = TAB_LABELS,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
            )
            if (errorMessage.isNotBlank()) {
                BannerError(
                    message = errorMessage,
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
                )
            }
            when (selectedTab) {
                0 -> BacklogTab(viewModel, onOpenTugas)
                1 -> DoneTab(viewModel, onOpenTugas)
                else -> ScoredTab(viewModel, onOpenScoreDetail = { scoreDetailId = it.id.toInt() })
            }
        }
    }

    scoreDetailId?.let { id ->
        TugasNilaiDetailSheet(tugasId = id, onDismiss = { scoreDetailId = null }, onOpenPdf = onOpenPdf)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BacklogTab(viewModel: TugasSiswaViewModel, onOpenTugas: (HomeworkTable) -> Unit) {
    val items by viewModel.backlog.collectAsStateWithLifecycle()
    val loading by viewModel.backlogLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            items.isNotEmpty() && lastVisible >= items.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMoreBacklog() }

    if (items.isEmpty() && !loading) {
        EmptyState(title = "Belum terdapat tugas", description = "", icon = Icons.Rounded.Assignment)
    } else {
        PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refreshBacklog() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items, key = { it.id }) { item -> TugasBacklogItem(item = item, onClick = { onOpenTugas(item) }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoneTab(viewModel: TugasSiswaViewModel, onOpenTugas: (HomeworkTable) -> Unit) {
    val items by viewModel.done.collectAsStateWithLifecycle()
    val loading by viewModel.doneLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            items.isNotEmpty() && lastVisible >= items.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMoreDone() }

    if (items.isEmpty() && !loading) {
        EmptyState(title = "Belum terdapat rekap tugas", description = "", icon = Icons.Rounded.Assignment)
    } else {
        PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refreshDone() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items, key = { it.id }) { item -> TugasDoneItem(item = item, onClick = { onOpenTugas(item) }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoredTab(viewModel: TugasSiswaViewModel, onOpenScoreDetail: (HomeworkTable) -> Unit) {
    val items by viewModel.scored.collectAsStateWithLifecycle()
    val loading by viewModel.scoredLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            items.isNotEmpty() && lastVisible >= items.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMoreScored() }

    if (items.isEmpty() && !loading) {
        EmptyState(title = "Belum terdapat rekap tugas", description = "", icon = Icons.Rounded.Assignment)
    } else {
        PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refreshScored() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items, key = { it.id }) { item -> TugasScoredItem(item = item, onClick = { onOpenScoreDetail(item) }) }
            }
        }
    }
}
