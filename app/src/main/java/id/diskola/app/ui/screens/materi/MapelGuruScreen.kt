package id.diskola.app.ui.screens.materi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.MateriViewModel

/** Subjects the signed-in teacher is assigned to. Legacy hides this tab entirely
 * (`MapelTeacherPage` is unreachable) — kept reachable here on purpose, a genuine improvement, not
 * a silent deviation (doc §3.3 marks the legacy tab dead). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapelGuruScreen(
    onBack: () -> Unit,
    onOpenSubject: (MapelTable) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val loading by viewModel.subjectsLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.openSubjectPicker(isTeacher = true) }

    DetailScaffold(title = "Mata Pelajaran", onBack = onBack, modifier = modifier) { padding ->
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { viewModel.refreshSubjects(isTeacher = true) },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item {
                    Text(
                        "Mata pelajaran yang Anda ampu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                items(subjects, key = { it.id }) { subject ->
                    SubjectRow(
                        name = subject.name,
                        icon = subject.image,
                        onClick = { onOpenSubject(subject) },
                    )
                }
            }
        }
    }
}
