package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing

/**
 * Non-cancelable "Mohon Tunggu" progress dialog — replaces the legacy `ProgressDialog` (flagged as
 * deprecated in `docs/migrasi/02-auth-login-sesi.md` §13) with a design-system equivalent, used for
 * every blocking auth network call ("Sedang mencari data pengguna...", "Proses login akun", …).
 */
@Composable
fun AppLoadingDialog(message: String, modifier: Modifier = Modifier) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = Spacing.md))
        }
    }
}
