package id.diskola.app.ui.screens.agenda

import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.ResponData.StaffAgendaPolicy
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

private data class PolicyLine(val label: String, val description: String)

private fun policyLines(policy: StaffAgendaPolicy): List<PolicyLine> = buildList {
    if (policy.checkoutRequired) {
        add(PolicyLine("Wajib absen Pulang", "Setelah absen masuk, Anda harus absen pulang untuk sesi ini."))
        if (policy.earlyLeaveEnabled) {
            add(PolicyLine("Boleh pulang lebih awal", "Absen pulang sebelum jendela pulang tetap diterima dengan status Pulang Cepat."))
        } else {
            add(PolicyLine("Pulang sesuai jadwal", "Absen pulang sebelum jendela akan ditolak. Tunggu mendekati jam selesai sesi."))
        }
    } else {
        add(PolicyLine("Tanpa absen Pulang", "Cukup absen masuk saja. Tidak perlu absen pulang."))
    }
    if (policy.lateEnabled) {
        add(PolicyLine("Ada status Terlambat", "Absen masuk setelah batas toleransi akan tercatat sebagai Terlambat."))
    } else {
        add(PolicyLine("Tanpa status Terlambat", "Absen masuk kapan pun di hari yang sama tetap tercatat sebagai Hadir."))
    }
}

/** "Kebijakan absensi" — collapsible (starts closed), shared by the check and detail screens. */
@Composable
fun AgendaPolicySection(policy: StaffAgendaPolicy?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // Honour the system "remove animations" setting like the legacy binder did.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val toggleLabel = if (expanded) "Tutup kebijakan absensi" else "Buka kebijakan absensi"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .animateContentSize(animationSpec = if (reduceMotion) snap() else tween(300)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { expanded = !expanded }
                .semantics { contentDescription = toggleLabel }
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Kebijakan absensi",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (policy == null) {
                    Text(
                        "Tidak ada kebijakan atau pengaturan terkait absen agenda mingguan untuk sesi ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                } else {
                    policyLines(policy).forEach { line ->
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                line.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSecondaryContainer,
                                modifier = Modifier.background(scheme.secondaryContainer, DiskolaExtraShapes.chipBadge)
                                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            )
                            Text(line.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
