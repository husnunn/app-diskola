package id.diskola.app.ui.screens.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.dataclass.ResponData.AgendaUiStatus
import id.diskola.app.dataclass.ResponData.StaffAgendaGate
import id.diskola.app.dataclass.ResponData.StaffAgendaItem
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.SectionLabel
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AgendaDetailViewModel

/** "Detail Agenda" — read-only timeline of one session (doc `05` §8.4). */
@Composable
fun AgendaDetailScreen(
    date: String,
    agendaId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgendaDetailViewModel = hiltViewModel(),
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val day by viewModel.day.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start(date, agendaId) }

    DetailScaffold(title = "Detail Agenda", onBack = onBack, modifier = modifier) { padding ->
        val agenda = item
        if (agenda != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Header(agenda)
                AgendaPolicySection(policy = agenda.policy)
                Timeline(agenda)
                Footer(agenda, day?.gate)
            }
        }
    }

    if (notFound) {
        AppDialog(
            onDismiss = onBack,
            title = "Agenda Tidak Tersedia",
            body = "Data agenda ini tidak ditemukan. Buka kembali daftar agenda lalu coba lagi.",
            primaryButtonText = "Tutup",
            onPrimaryClick = onBack,
            dismissible = false,
        )
    }
}

@Composable
private fun Header(agenda: StaffAgendaItem) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(agenda.kind, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            StatusBadge(label = agenda.uiStatus.label, tone = agenda.uiStatus.tone())
        }
        Text(agenda.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
        Text("Jadwal ${agenda.windowLabel}", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun Timeline(agenda: StaffAgendaItem) {
    val event = agenda.event
    val needsCheckout = agenda.uiStatus == AgendaUiStatus.NEED_CHECKOUT
    val scheme = MaterialTheme.colorScheme

    Section("Check-in") {
        Text(event?.time?.ifBlank { "-" } ?: "-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
        Text(
            when {
                event == null -> "Belum check-in. Tap Lapor masuk untuk absensi agenda ini."
                needsCheckout -> "Sudah masuk. Belum lapor pulang untuk agenda ini."
                else -> "Status: ${event.status} · Via: ${event.via}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }

    val showCheckout = !event?.checkout_time.isNullOrBlank() || needsCheckout || agenda.policy?.checkoutRequired == true
    if (showCheckout) {
        Section("Check-out") {
            Text(event?.checkout_time?.ifBlank { "-" } ?: "-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            Text(
                when {
                    !event?.checkout_status.isNullOrBlank() -> "Status pulang: ${event?.checkout_status}"
                    needsCheckout -> "Sudah masuk. Belum lapor pulang untuk agenda ini."
                    else -> "Status pulang: -"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Footer(agenda: StaffAgendaItem, gate: StaffAgendaGate?) {
    val scheme = MaterialTheme.colorScheme
    Section(null) {
        listOf(
            "ID Agenda: ${agenda.agenda_id}",
            "Status: ${agenda.uiStatus.label}",
            "Via: ${agenda.event?.via?.ifBlank { "-" } ?: "-"}",
            "Catatan: ${agenda.note?.takeIf { it.isNotBlank() } ?: "Tidak ada catatan"}",
        ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface) }

        val gateIn = gate?.gateIn?.time?.takeIf { it.isNotBlank() }
        val gateOut = gate?.out?.time?.takeIf { it.isNotBlank() }
        if (gateIn == null && gateOut == null) {
            Text("Belum ada data gate", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        } else {
            gateIn?.let { Text("Gate masuk: $it", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
            gateOut?.let { Text("Gate pulang: $it", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Section(title: String?, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (title != null) SectionLabel(title)
        content()
    }
}
