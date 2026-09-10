package id.app.education.ui.screens.absensi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.app.education.dataclass.ResponData.AttendanceScheduleItem
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.BadgeTone
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.StatusBadge
import id.app.education.ui.theme.Spacing

/** Compose port of `item_attendance_schedule.xml` + `AttendanceAdapter.bind()` — same status logic. */
@Composable
fun AttendanceScheduleCard(
    item: AttendanceScheduleItem,
    onDetailClick: (AttendanceScheduleItem) -> Unit,
    onActionClick: (AttendanceScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusStr = item.status.orEmpty().lowercase()
    val tone = when (statusStr) {
        "ongoing" -> BadgeTone.Success
        "toward" -> BadgeTone.Warning
        else -> BadgeTone.Neutral
    }
    val actionLabel = when (statusStr) {
        "ongoing" -> if (item.attendAt.isNullOrBlank()) "Absen Masuk" else "Absen Keluar"
        "toward" -> "Belum Mulai"
        "passed" -> "Selesai"
        else -> item.status.orEmpty().ifBlank { "Absen" }
    }
    val actionEnabled = statusStr == "ongoing" &&
        (item.attendAt.isNullOrBlank() || item.leaveAt.isNullOrBlank())

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.subjectName.orEmpty().ifBlank { "Mapel Tidak Ada" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(label = item.status.orEmpty().ifBlank { "n/a" }, tone = tone)
            }
            Spacer(modifier = Modifier.size(Spacing.xs))
            Text(
                "Guru: ${item.teacherName.orEmpty().ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Kelas: ${item.className.orEmpty().ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(Spacing.sm))
            Text(
                "${item.timePlotStartAt.orEmpty().ifBlank { "--:--" }} - ${item.timePlotEndAt.orEmpty().ifBlank { "--:--" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jam masuk", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.attendAt.orEmpty().ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jam keluar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.leaveAt.orEmpty().ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.size(Spacing.sm))
            Text(
                text = if (item.lateAt.isNullOrBlank()) "Presensi belum dibuka" else "Batas terlambat: ${item.lateAt}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(text = "Detail", onClick = { onDetailClick(item) }, variant = ButtonVariant.Outlined, modifier = Modifier.weight(1f))
                AppButton(text = actionLabel, onClick = { onActionClick(item) }, enabled = actionEnabled, modifier = Modifier.weight(1f))
            }
        }
    }
}
