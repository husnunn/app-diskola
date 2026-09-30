package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.akm.AkmItem
import id.diskola.app.dataclass.akm.AkmStatus
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.viewmodel.AkmViewModel

/**
 * Pre-exam briefing. The bottom bar is the whole point of this screen: it reflects exactly one of
 * the assessment's lifecycle states, and is the only place an exam can be started from.
 */
@Composable
fun AkmDetailScreen(
    akmId: Int,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onViewScore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AkmViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val item = items.find { it.id == akmId }
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val strictMode by viewModel.strictMode.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

    var dialog by rememberSaveable { mutableStateOf<AkmStartDialog?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var checkingPassword by remember { mutableStateOf(false) }

    if (item == null) {
        DetailScaffold(title = "Detail Asesmen", onBack = onBack, modifier = modifier) { padding ->
            LoadingState(modifier = Modifier.padding(padding))
        }
        return
    }

    val participantRows = remember(item) { readAkmParticipant(viewModel.sessionStore, item) }
    val status = statuses[item.id] ?: AkmStatus.BelumSinkron

    // Re-hydrates status/progress from Room on every screen open — so a process restart mid-sync
    // (or after a full app kill) picks up already-downloaded data instead of forcing a re-sync.
    LaunchedEffect(item.id) { viewModel.observeSyncState(item.id) }

    DetailScaffold(
        title = "Detail Asesmen",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            AkmDetailActionBar(
                status = status,
                started = item.hasStarted(),
                syncProgress = syncProgress[item.id],
                onSync = { viewModel.syncQuestions(item.id) },
                onStart = {
                    passwordError = null
                    dialog = if (item.needsPassword) AkmStartDialog.Password else AkmStartDialog.Terms
                },
                onViewScore = onViewScore,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AkmChip("Asesmen Sekolah", accent = true)
                    AkmChip(status.label)
                }
                Text(
                    "Berakhir ${item.endLabel} · ${item.questionCount} soal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.needsPassword) {
                InfoBanner(
                    icon = Icons.Rounded.Key,
                    text = "Asesmen ini memerlukan password ujian dari pengawas. Password hanya berlaku di satu perangkat.",
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionLabel("INFORMASI PESERTA")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
                        .padding(horizontal = Spacing.lg),
                ) {
                    participantRows.forEachIndexed { index, (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                value,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (index != participantRows.lastIndex) HorizontalDivider()
                    }
                }
            }

            if (status == AkmStatus.MengunduhSoal) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DiskolaExtraShapes.card)
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Column(modifier = Modifier.padding(start = Spacing.md)) {
                        Text(
                            "Mengunduh soal…",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Jangan tutup aplikasi sampai unduhan selesai.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    AkmStartDialogs(
        dialog = dialog,
        strictMode = strictMode,
        checking = checkingPassword,
        passwordError = passwordError,
        onCheckPassword = { password ->
            checkingPassword = true
            passwordError = null
            viewModel.checkPassword(item.id, password) { result ->
                checkingPassword = false
                result
                    .onSuccess { checked ->
                        if (checked) {
                            passwordError = null
                            dialog = AkmStartDialog.Terms
                        } else {
                            passwordError = "Password ujian tidak sesuai."
                        }
                    }
                    .onFailure { passwordError = it.message ?: "Password ujian belum bisa diperiksa. Silahkan ulangi beberapa saat lagi." }
            }
        },
        onDismiss = { dialog = null },
        onTermsAccepted = {
            dialog = null
            onStart()
        },
        onSimulateOffline = { dialog = AkmStartDialog.Offline },
    )
}

/**
 * The seven rows and their sources are specified in
 * `docs/repo lama/asesmen/asesmen-akm-daftar-detail-nilai.md:220-226`: Nama/NIS/Kelas/Sekolah are
 * never part of the Asesmen API response — they come from the session saved at login — while
 * ID Ujian is the schedule's own id and Kategori/Periode are derived from `exams[]`.
 */
private fun readAkmParticipant(sessionStore: id.diskola.app.utils.session.SessionStore, item: AkmItem): List<Pair<String, String>> {
    return listOf(
        "ID Ujian" to item.id.toString(),
        "Nama" to sessionStore.user.name.ifBlank { "-" },
        "NIS" to sessionStore.user.nisNik.ifBlank { sessionStore.student?.nis.orEmpty() }.ifBlank { "-" },
        "Kelas" to (sessionStore.student?.className.orEmpty()).ifBlank { "-" },
        "Kategori" to item.category.ifBlank { "-" },
        "Periode" to item.period.ifBlank { "-" },
        "Sekolah" to sessionStore.school.name.ifBlank { "-" },
    )
}

@Composable
private fun AkmDetailActionBar(
    status: AkmStatus,
    started: Boolean,
    syncProgress: Pair<Int, Int>?,
    onSync: () -> Unit,
    onStart: () -> Unit,
    onViewScore: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest)
            .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
    ) {
        when {
            status == AkmStatus.BelumSinkron -> AppButton(
                text = "Sinkronisasi Soal",
                onClick = onSync,
                leadingIcon = { Icon(Icons.Rounded.CloudDownload, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )

            status == AkmStatus.MengunduhSoal -> AppButton(
                text = syncProgress?.let { (progress, total) -> "Mengunduh soal… ($progress/$total)" } ?: "Mengunduh soal…",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )

            status == AkmStatus.SiapDikerjakan && started -> AppButton(
                text = "Mulai Asesmen",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )

            status == AkmStatus.SiapDikerjakan -> StaticBar(
                text = "Asesmen belum dimulai",
                background = scheme.surfaceContainerLow,
                foreground = scheme.onSurfaceVariant,
            )

            status == AkmStatus.SudahDinilai -> AppButton(
                text = "Lihat Nilai",
                onClick = onViewScore,
                modifier = Modifier.fillMaxWidth(),
            )

            else -> StaticBar(
                text = "Asesmen telah dikumpulkan",
                background = scheme.secondaryContainer,
                foreground = scheme.onSecondaryContainer,
                icon = Icons.Rounded.TaskAlt,
            )
        }
    }
}

@Composable
private fun StaticBar(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(background, DiskolaExtraShapes.button),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = foreground, modifier = Modifier.size(20.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = foreground,
            modifier = if (icon != null) Modifier.padding(start = Spacing.sm) else Modifier,
        )
    }
}

@Composable
internal fun InfoBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.extendedColors.warning, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.md),
        )
    }
}
