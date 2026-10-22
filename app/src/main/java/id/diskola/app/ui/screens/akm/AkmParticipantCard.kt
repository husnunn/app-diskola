package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AkmRules
import id.diskola.app.utils.session.SessionStore

/**
 * The rows of the participant table, in the legacy order (doc `06` §5.3): ID Ujian, **Status** ("Dibatalkan"/"Aktif", added in 2.1.41),
 * Peserta, NIS, Kelas, Kategori, Periode, Sekolah. Nama/NIS/Kelas/Sekolah are never part of the Asesmen API response — they come from the
 * session saved at login (`asesmen-akm-daftar-detail-nilai.md:220-226`); Kategori/Periode are derived from `exams[]`.
 */
internal fun akmParticipantRows(
    sessionStore: SessionStore,
    examId: Int,
    category: String,
    period: String,
    watermark: AkmRules.AkmWatermark,
): List<Pair<String, String>> = listOf(
    "ID Ujian" to examId.toString(),
    "Status" to if (watermark.enabled) "Dibatalkan" else "Aktif",
    "Peserta" to sessionStore.user.name.ifBlank { "-" },
    "NIS" to sessionStore.user.nisNik.ifBlank { sessionStore.student?.nis.orEmpty() }.ifBlank { "-" },
    "Kelas" to (sessionStore.student?.className.orEmpty()).ifBlank { "-" },
    "Kategori" to category.ifBlank { "-" },
    "Periode" to period.ifBlank { "-" },
    "Sekolah" to sessionStore.school.name.ifBlank { "-" },
)

@Composable
internal fun AkmParticipantCard(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest, DiskolaExtraShapes.card)
            .border(1.dp, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(horizontal = Spacing.lg),
    ) {
        rows.forEachIndexed { index, (label, value) ->
            val cancelled = label == "Status" && value == "Dibatalkan"
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (cancelled) scheme.error else scheme.onSurface,
                )
            }
            if (index != rows.lastIndex) HorizontalDivider()
        }
    }
}
