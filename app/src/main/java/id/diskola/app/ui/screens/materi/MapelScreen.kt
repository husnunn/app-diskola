package id.diskola.app.ui.screens.materi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.MateriViewModel

/**
 * Subject picker — the student's entry point into Materi (`TheoryPage`, doc
 * `05-pembelajaran-materi-tugas.md` §2.4): local-only search (debounced 400ms in the ViewModel),
 * 20/page infinite scroll from the Room cache, real pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapelScreen(
    onBack: () -> Unit,
    onOpenSubject: (MapelTable) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val loading by viewModel.subjectsLoading.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.openSubjectPicker(isTeacher = false) }
    LaunchedEffect(query) { viewModel.onSubjectQueryChange(query) }

    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            subjects.isNotEmpty() && lastVisible >= subjects.size - 3
        }
    }
    LaunchedEffect(reachedEnd) { if (reachedEnd) viewModel.loadMoreSubjects(isTeacher = false) }

    DetailScaffold(title = "Materi", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SubjectSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            )

            if (subjects.isEmpty() && query.isNotBlank()) {
                EmptyState(
                    title = "Mata pelajaran tidak ditemukan",
                    description = "Coba gunakan kata kunci lain",
                    icon = Icons.Rounded.SearchOff,
                )
            } else if (subjects.isEmpty() && loading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Loading...", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refreshSubjects(isTeacher = false) }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xxl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(subjects, key = { it.id }) { subject ->
                            SubjectRow(
                                name = subject.name,
                                subtitle = subject.teacher_name.takeIf { it.isNotBlank() } ?: "Guru belum ditetapkan",
                                label = subject.label.takeIf { it.isNotBlank() },
                                icon = subject.image,
                                onClick = { onOpenSubject(subject) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubjectSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.searchPill)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.searchPill)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(Spacing.md))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Cari mata pelajaran", style = MaterialTheme.typography.bodyLarge, color = scheme.outline)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SubMaterialIcon(url: String, tint: androidx.compose.ui.graphics.Color) {
    var failed by remember(url) { mutableStateOf(url.isBlank()) }
    if (failed) {
        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            onError = { failed = true },
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Subject card shared by the student and teacher subject lists. */
@Composable
internal fun SubjectRow(
    name: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    label: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(scheme.surfaceContainerLow, DiskolaExtraShapes.iconBox),
            contentAlignment = Alignment.Center,
        ) {
            // The API returns an icon URL, not a Material symbol name, so this is an image with
            // a book fallback for subjects that have no icon set.
            SubMaterialIcon(url = icon, tint = scheme.primary)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!label.isNullOrBlank()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = scheme.primary,
                        modifier = Modifier
                            .padding(start = Spacing.sm)
                            .background(scheme.secondaryContainer, DiskolaExtraShapes.chipBadge)
                            .padding(horizontal = Spacing.sm, vertical = 2.dp),
                    )
                }
            }
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}
