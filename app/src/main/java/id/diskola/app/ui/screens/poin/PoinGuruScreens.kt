package id.diskola.app.ui.screens.poin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.dataclass.ResponData.PoinFormMode
import id.diskola.app.dataclass.ResponData.SearchPoinStudentItem
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ErrorText
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.PoinAccess
import id.diskola.app.viewmodel.PoinGuruViewModel
import id.diskola.app.viewmodel.PoinSearchState

/** "Poin Siswa" — teacher search (`PoinForm`, doc `05` §9.4). Also the flow root: it owns the
 * access gate and the selected student shared with the result and form screens. */
@Composable
fun PoinCariScreen(
    onBack: () -> Unit,
    onStudentReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PoinGuruViewModel = hiltViewModel(),
) {
    val access by viewModel.access.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val blockedMessage by viewModel.blockedMessage.collectAsStateWithLifecycle()

    DetailScaffold(title = "Poin Siswa", onBack = onBack, modifier = modifier) { padding ->
        when (access) {
            PoinAccess.Checking -> Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(
                    "Memeriksa akses...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            // The denial dialog below is the only thing shown for a blocked account.
            PoinAccess.Denied -> Box(modifier = Modifier.padding(padding).fillMaxSize())
            PoinAccess.Allowed -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SectionLabel("MASUKKAN DATA SISWA", modifier = Modifier.padding(top = Spacing.md))
                AppTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Ketikkan Nama atau NISN/NIS/NIK",
                    trailing = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                if (errorMessage.isNotBlank()) {
                    BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
                }
                SearchResults(
                    state = search,
                    onPick = { student -> viewModel.pick(student, onStudentReady) },
                )
            }
        }
    }

    if (access == PoinAccess.Denied) {
        AppDialog(
            onDismiss = onBack,
            title = "Akses Ditolak",
            body = "Anda tidak memiliki akses untuk membuka halaman ini, Silahkan hubungi pihak sekolah",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }
    if (loading) AppLoadingDialog(message = "Sedang mencari data pengguna...")
    if (blockedMessage.isNotBlank()) {
        AppDialog(
            onDismiss = viewModel::dismissBlocked,
            title = "Tidak Dapat Diproses",
            body = blockedMessage,
            primaryButtonText = "Tutup",
            onPrimaryClick = viewModel::dismissBlocked,
        )
    }
}

@Composable
private fun SearchResults(state: PoinSearchState, onPick: (SearchPoinStudentItem) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    when (state) {
        PoinSearchState.Idle -> Unit
        PoinSearchState.Loading -> Text("mencari siswa ...", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        is PoinSearchState.Error -> ErrorText(state.message)
        is PoinSearchState.Result ->
            if (state.items.isEmpty()) {
                Text(
                    "Data siswa tidak ditemukan\nPastikan nama atau NISN/NIS/NIK sudah benar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.items, key = { "${it.id}|${it.nisn}|${it.name}" }) { student ->
                        StudentRow(student = student, onClick = { onPick(student) })
                    }
                }
            }
    }
}

@Composable
private fun StudentRow(student: SearchPoinStudentItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudentAvatar(url = student.user_avatar_image, size = 44)
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
            Text(student.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            if (student.student_class.isNotBlank()) {
                Text(student.student_class, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            if (student.nisn.isNotBlank()) {
                Text(student.nisn, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StudentAvatar(url: String, size: Int) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(scheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Person, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size((size / 2).dp))
        if (url.isNotBlank()) {
            AsyncImage(
                model = resolveAssetUrl(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** "Hasil" (`PoinProcess`) — the picked student's totals and the three "Tambah Data" actions. */
@Composable
fun PoinHasilScreen(
    onBack: () -> Unit,
    onOpenForm: (PoinFormMode) -> Unit,
    guruViewModel: PoinGuruViewModel,
    modifier: Modifier = Modifier,
) {
    val selected by guruViewModel.selected.collectAsStateWithLifecycle()
    val current = selected
    // Process death while on this screen drops the in-memory selection; there is nothing to show.
    LaunchedEffect(current == null) { if (current == null) onBack() }
    if (current == null) return
    val student = current.student
    val scheme = MaterialTheme.colorScheme

    DetailScaffold(title = "Poin Siswa", onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionLabel("Hasil")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                    .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudentAvatar(url = student.user_avatar_image, size = 56)
                Column(modifier = Modifier.weight(1f).padding(start = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(student.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
                    if (student.role.isNotBlank()) {
                        Text(student.role, style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                    }
                    if (student.student_class.isNotBlank()) {
                        Text(student.student_class, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                    if (student.nisn.isNotBlank()) {
                        Text("NISN ${student.nisn}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScoreCard("Pelanggaran", student.violation_score ?: 0, scheme.error, Modifier.weight(1f))
                ScoreCard("Prestasi", student.achievement_score ?: 0, MaterialTheme.extendedColors.success, Modifier.weight(1f))
            }

            SectionLabel("Tambah Data")
            AppButton(text = "Pelanggaran", onClick = { onOpenForm(PoinFormMode.VIOLATION) }, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            AppButton(text = "Prestasi", onClick = { onOpenForm(PoinFormMode.ACHIEVEMENT) }, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            AppButton(text = "Pemanggilan Siswa", onClick = { onOpenForm(PoinFormMode.HANDLING) }, variant = ButtonVariant.Filled, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScoreCard(label: String, score: Int, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Text(score.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = accent)
    }
}
