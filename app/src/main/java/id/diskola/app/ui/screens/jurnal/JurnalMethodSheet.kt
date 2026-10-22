package id.diskola.app.ui.screens.jurnal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing

/**
 * "Pilih Metode Presensi" (legacy `dialog_confirm_subject`): "Verifikasi Jurnal" always, "Scan QR" only when [onQr] is given.
 * [qrHint] explains why there is no QR option (the Jurnal list does; the Hub card, like legacy, has no QR at all).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurnalMethodSheet(
    onVerifikasi: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onQr: (() -> Unit)? = null,
    qrHint: Boolean = false,
) {
    AppBottomSheet(title = "Pilih Metode Presensi", onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                "Silakan pilih metode yang ingin digunakan untuk presensi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
            AppButton(text = "Verifikasi Jurnal", onClick = onVerifikasi, modifier = Modifier.fillMaxWidth())
            if (onQr != null) {
                AppButton(text = "Scan QR", onClick = onQr, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            } else if (qrHint) {
                Text(
                    "Scan QR hanya tersedia saat jam pelajaran ini berlangsung.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
