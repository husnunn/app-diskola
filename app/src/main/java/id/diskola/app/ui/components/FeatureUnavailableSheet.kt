package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing

/**
 * "Fitur Belum Tersedia di Aplikasi" — shown when `check-feature-availability` says a feature is off.
 * Students always get the fixed "being updated" text; a teacher gets the server's own message and a
 * shortcut to the web portal where the feature still lives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureUnavailableSheet(
    isTeacher: Boolean,
    serverMessage: String,
    onOpenWebsite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = if (isTeacher) {
        serverMessage.ifBlank { "Fitur ini belum tersedia di aplikasi. Silakan gunakan website terlebih dahulu." }
    } else {
        "Fitur ini sedang diperbarui. Silakan coba lagi nanti."
    }
    AppBottomSheet(title = "Fitur Belum Tersedia di Aplikasi", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                "Sedang diperbarui",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .background(MaterialTheme.colorScheme.secondaryContainer, DiskolaExtraShapes.chipBadge)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isTeacher) {
                AppButton(text = "Buka Website", onClick = onOpenWebsite, modifier = Modifier.fillMaxWidth())
            }
            AppButton(text = "Kembali", onClick = onDismiss, variant = ButtonVariant.Text, modifier = Modifier.fillMaxWidth())
        }
    }
}
