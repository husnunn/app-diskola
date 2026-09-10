package id.app.education.ui.screens.materi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.ui.components.DetailScaffold
import id.app.education.ui.components.EmptyState
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.MateriViewModel

/** Materials inside one subject. Same screen for both roles — only the fetch differs. */
@Composable
fun MateriListScreen(
    subjectId: Int,
    subjectName: String,
    onBack: () -> Unit,
    onOpenMateri: (MateriItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val isTeacher = rememberIsTeacher()
    val materials by viewModel.materiList.collectAsStateWithLifecycle()

    LaunchedEffect(subjectId, isTeacher) {
        if (isTeacher) {
            viewModel.getMateriTeacher(subjectId = subjectId)
        } else {
            viewModel.getMateriStudent(subjectId = subjectId)
        }
    }

    DetailScaffold(title = subjectName.ifBlank { "Materi" }, onBack = onBack, modifier = modifier) { padding ->
        if (materials.isEmpty()) {
            EmptyState(
                title = "Belum terdapat materi",
                description = "Materi yang diunggah guru akan muncul di sini.",
                icon = Icons.Rounded.LibraryBooks,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
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
