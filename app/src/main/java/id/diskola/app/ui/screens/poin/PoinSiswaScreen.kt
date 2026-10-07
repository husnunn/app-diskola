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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.repository.PoinCall
import id.diskola.app.repository.PoinEvent
import id.diskola.app.repository.PoinRecap
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AutoTimeGate
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.EmptyState
import id.diskola.app.ui.components.PillTabRow
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.resolveAssetUrl
import id.diskola.app.viewmodel.PoinSiswaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAB_LABELS = listOf("Pelanggaran", "Prestasi")

/** "Poin Saya" (`PoinStudentPage`, doc `05` §10) — pelanggaran / prestasi tabs fed by one fetch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoinSiswaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PoinSiswaViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var callsDialog by remember { mutableStateOf(false) }
    var recapDialog by remember { mutableStateOf(false) }
    var imageDialog by remember { mutableStateOf<PoinEvent?>(null) }

    AutoTimeGate(
        title = "Peringatan",
        body = "Harap atur tanggal dan waktu ponsel ke \"Otomatis\"",
        onCancel = onBack,
    )

    val isViolation = tab == 0
    val data = summary
    val events = if (isViolation) data?.violations.orEmpty() else data?.achievements.orEmpty()
    val recap = if (isViolation) data?.violationRecap else data?.achievementRecap
    val calls = if (isViolation) data?.pendingCalls.orEmpty() else emptyList()
    val hasHeader = data != null && (events.isNotEmpty() || calls.isNotEmpty() || recap != null && (recap.totalScore != 0 || recap.rows.isNotEmpty()))

    DetailScaffold(title = "Poin Saya", onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PillTabRow(
                options = TAB_LABELS,
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.sm),
            )
            if (errorMessage.isNotBlank()) {
                BannerError(
                    message = errorMessage,
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
                )
            }
            PullToRefreshBox(isRefreshing = loading, onRefresh = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = Spacing.md, bottom = Spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (hasHeader) {
                        item {
                            PoinHeader(
                                recap = recap,
                                isViolation = isViolation,
                                callLabel = calls.firstOrNull()?.name,
                                onOpenCalls = { callsDialog = true },
                                onOpenRecap = { recapDialog = true },
                            )
                        }
                    }
                    if (events.isEmpty()) {
                        if (!loading) {
                            item {
                                EmptyState(
                                    title = "Data Kosong",
                                    description = "",
                                    icon = Icons.Rounded.EmojiEvents,
                                    modifier = Modifier.fillParentMaxHeight(0.6f),
                                )
                            }
                        }
                    } else {
                        items(events, key = { it.key }) { event ->
                            PoinEventCard(event = event, onOpenImage = { imageDialog = event })
                        }
                    }
                }
            }
        }
    }

    if (callsDialog) {
        AppDialog(
            onDismiss = { callsDialog = false },
            title = "Detail Penanganan Siswa",
            body = "",
            primaryButtonText = "Tutup",
            onPrimaryClick = { callsDialog = false },
        ) {
            ScrollableDialogColumn {
                data?.pendingCalls.orEmpty().forEach { CallRow(it) }
            }
        }
    }

    if (recapDialog && recap != null) {
        AppDialog(
            onDismiss = { recapDialog = false },
            title = if (isViolation) "Detail Pelanggaran Semester" else "Detail Prestasi Semester",
            body = "",
            primaryButtonText = "Tutup",
            onPrimaryClick = { recapDialog = false },
        ) {
            RecapContent(recap = recap, isViolation = isViolation)
        }
    }

    imageDialog?.let { event ->
        AppDialog(
            onDismiss = { imageDialog = null },
            title = event.name,
            body = "${event.date} || ${event.time}",
            primaryButtonText = "Tutup",
            onPrimaryClick = { imageDialog = null },
        ) {
            AsyncImage(
                model = resolveAssetUrl(event.image),
                contentDescription = event.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(DiskolaExtraShapes.card),
            )
        }
    }
}

@Composable
private fun PoinHeader(
    recap: PoinRecap?,
    isViolation: Boolean,
    callLabel: String?,
    onOpenCalls: () -> Unit,
    onOpenRecap: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val month = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (!callLabel.isNullOrBlank()) {
            AppButton(
                text = callLabel,
                onClick = onOpenCalls,
                variant = ButtonVariant.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
                .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreRing(score = recap?.totalScore ?: 0, isViolation = isViolation, size = 56)
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text("Detail (semester)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
                Text("Bulan $month", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            AppButton(text = "Lihat", onClick = onOpenRecap, variant = ButtonVariant.Outlined)
        }
    }
}

@Composable
private fun ScoreRing(score: Int, isViolation: Boolean, size: Int) {
    val color = if (isViolation) MaterialTheme.colorScheme.error else MaterialTheme.extendedColors.success
    Box(
        modifier = Modifier.size(size.dp).border(5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            score.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PoinEventCard(event: PoinEvent, onOpenImage: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(event.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
        Row {
            Text("Jumlah Poin", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(event.score.toString(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
        }
        if (event.creatorName.isNotBlank()) {
            Row {
                Text("Dibuat oleh", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(event.creatorName, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
            }
        }
        if (event.message.isNotBlank()) {
            Text(event.message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        if (event.image.isNotBlank()) {
            Text(
                "Lihat Dokumentasi",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.primary,
                modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onOpenImage).padding(vertical = Spacing.sm),
            )
        }
    }
}

@Composable
private fun CallRow(call: PoinCall) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(call.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
        Text("Jadwal", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Text(call.at, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        if (call.message.isNotBlank()) Text(call.message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

@Composable
private fun RecapContent(recap: PoinRecap, isViolation: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ScoreRing(score = recap.totalScore, isViolation = isViolation, size = 88)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Nama Poin", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text("Total Poin", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface, textAlign = TextAlign.End)
        }
        ScrollableDialogColumn {
            recap.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isViolation) recapLabel(row.name) else row.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(row.score.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = scheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ScrollableDialogColumn(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())) { content() }
}

/** Server recap names that the legacy app translated client-side (`PoinStudentViolationPage.kt:271-276`). */
private fun recapLabel(raw: String): String = when (raw) {
    "Late" -> "Masuk Terlambat"
    "Not Out" -> "Tidak absen pulang"
    "Not Attending Class" -> "Tidak Hadir Mapel"
    else -> raw
}
