package id.app.education.ui.screens.materi

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import id.app.education.dataclass.ResponData.MapelItem
import id.app.education.ui.components.DetailScaffold
import id.app.education.ui.components.EmptyState
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.MateriViewModel

/**
 * Subject picker — the student's entry point into Materi. The handoff also shows a material-count
 * chip and a Kejuruan/Umum label per subject; neither is returned by `studentSubjects`, so they
 * are omitted rather than faked.
 */
@Composable
fun MapelScreen(
    onBack: () -> Unit,
    onOpenSubject: (MapelItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val subjects by viewModel.studentSubjects.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.fetchStudentSubjects() }

    val filtered = remember(subjects, query) {
        if (query.isBlank()) subjects
        else subjects.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    DetailScaffold(title = "Materi", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SubjectSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            )

            if (filtered.isEmpty() && query.isNotBlank()) {
                EmptyState(
                    title = "Mata pelajaran tidak ditemukan",
                    description = "Coba kata kunci lain atau periksa ejaannya.",
                    icon = Icons.Rounded.SearchOff,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(filtered, key = { it.id }) { subject ->
                        SubjectRow(
                            name = subject.name,
                            subtitle = subject.teacher?.name?.takeIf { it.isNotBlank() } ?: "Guru belum ditetapkan",
                            icon = subject.icon_image,
                            onClick = { onOpenSubject(subject) },
                        )
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
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}
