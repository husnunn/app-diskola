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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.EventBusy
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
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
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.LoadingState
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.components.StampedIf
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.AkmSessionRules
import id.diskola.app.viewmodel.AkmListViewModel

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
    viewModel: AkmListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val item = items.find { it.id == akmId }
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var dialog by rememberSaveable { mutableStateOf<AkmStartDialog?>(null) }
    var strict by rememberSaveable { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var checkingPassword by remember { mutableStateOf(false) }

    if (item == null) {
        DetailScaffold(title = "Detail Asesmen", onBack = onBack, modifier = modifier) { padding ->
            if (loading) {
                LoadingState(modifier = Modifier.padding(padding))
            } else {
                // The list loaded (or failed) and does not contain this exam: say so instead of an endless spinner.
                EmptyState(
                    title = "Asesmen tidak ditemukan",
                    description = "Asesmen ini tidak ada di daftar. Kembali ke daftar lalu muat ulang.",
                    icon = Icons.Rounded.EventBusy,
                    modifier = Modifier.padding(padding),
                    actionLabel = "Muat ulang",
                    onAction = viewModel::refresh,
                )
            }
        }
        return
    }

    val participantRows = remember(item) {
        akmParticipantRows(viewModel.sessionStore, item.id, item.category, item.period, item.watermark)
    }
    val status = statuses[item.id] ?: AkmStatus.BelumSinkron

    val context = LocalContext.current
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    DetailScaffold(
        title = "Detail Asesmen",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            AkmDetailActionBar(
                status = status,
                started = item.hasStarted(),
                syncProgress = syncProgress[item.id],
                preparing = preparing,
                onSync = { viewModel.syncQuestions(item.id) },
                onStart = {
                    passwordError = null
                    if (viewModel.passwordRequired(item)) {
                        dialog = AkmStartDialog.Password
                    } else {
                        preparing = true
                        viewModel.prepareTerms { isStrict ->
                            preparing = false
                            strict = isStrict
                            dialog = AkmStartDialog.Terms
                        }
                    }
                },
                onViewScore = onViewScore,
            )
        },
    ) { padding ->
        // The big "Dibatalkan" stamp sits behind the content when the schedule carries the mark (doc 06 §4.4); it takes no touches.
        StampedIf(item.watermark, Modifier.padding(padding).fillMaxSize()) {
        Column(
            modifier = Modifier
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
                AkmParticipantCard(participantRows)
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
                            "Unduhan berjalan di latar belakang dan dilanjutkan bila terputus.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        }
    }

    AkmStartDialogs(
        dialog = dialog,
        checking = checkingPassword,
        passwordError = passwordError,
        strict = strict,
        onCheckPassword = { password ->
            checkingPassword = true
            passwordError = null
            viewModel.checkPassword(item.id, password) { result ->
                checkingPassword = false
                result
                    .onSuccess { checked ->
                        if (checked) {
                            passwordError = null
                            dialog = null
                            preparing = true
                            viewModel.prepareTerms { isStrict ->
                                preparing = false
                                strict = isStrict
                                dialog = AkmStartDialog.Terms
                            }
                        } else {
                            passwordError = "Password ujian tidak sesuai."
                        }
                    }
                    .onFailure { passwordError = it.message ?: "Password ujian belum bisa diperiksa. Silahkan ulangi beberapa saat lagi." }
            }
        },
        onDismiss = { dialog = null },
        onTermsAccepted = {
            // A strict exam starts online only: no real internet (a captive portal included) means the start is refused here, not later.
            if (AkmSessionRules.mayStart(strict, viewModel.isOnlineValidated())) {
                dialog = null
                onStart()
            } else {
                dialog = AkmStartDialog.NeedInternet
            }
        },
    )
}

@Composable
private fun AkmDetailActionBar(
    status: AkmStatus,
    started: Boolean,
    syncProgress: Pair<Int, Int>?,
    preparing: Boolean,
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
                text = if (preparing) "Menyiapkan…" else "Mulai Asesmen",
                onClick = onStart,
                enabled = !preparing,
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

            status == AkmStatus.ProsesUpload -> StaticBar(
                text = "Proses upload jawaban",
                background = scheme.secondaryContainer,
                foreground = scheme.onSecondaryContainer,
                icon = Icons.Rounded.CloudUpload,
            )

            // The server's own verdicts for an exam this student never took — not "dikumpulkan".
            status == AkmStatus.TidakMengerjakan || status == AkmStatus.SesiTerlewat -> StaticBar(
                text = status.label,
                background = scheme.surfaceContainerLow,
                foreground = scheme.onSurfaceVariant,
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
