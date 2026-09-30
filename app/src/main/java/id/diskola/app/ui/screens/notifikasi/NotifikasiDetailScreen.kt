package id.diskola.app.ui.screens.notifikasi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.mock.NotificationItem
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.iconFor
import id.diskola.app.ui.theme.Spacing

@Composable
fun NotifikasiDetailScreen(item: NotificationItem?, onBack: () -> Unit) {
    DetailScaffold(title = "Detail Notifikasi", onBack = onBack) { padding ->
        if (item == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Notifikasi tidak ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@DetailScaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(76.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor(item.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.md))
                Text(item.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(item.timeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                item.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                "Tombol aksi merutekan ke modul tujuan sesuai payload page.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            item.actionLabel?.let { label ->
                AppButton(text = label, onClick = { /* TODO: route per item.actionPage once real deep-linking exists */ }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
