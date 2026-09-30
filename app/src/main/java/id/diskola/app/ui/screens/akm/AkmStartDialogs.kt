package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors

/** The gates a student passes through between tapping "Mulai" and the exam actually opening. */
enum class AkmStartDialog { Password, Terms, Offline }

@Composable
fun AkmStartDialogs(
    dialog: AkmStartDialog?,
    strictMode: Boolean,
    checking: Boolean,
    passwordError: String?,
    onCheckPassword: (String) -> Unit,
    onDismiss: () -> Unit,
    onTermsAccepted: () -> Unit,
    onSimulateOffline: () -> Unit,
) {
    when (dialog) {
        AkmStartDialog.Password -> PasswordDialog(
            onDismiss = onDismiss,
            checking = checking,
            error = passwordError,
            onCheck = onCheckPassword,
        )

        AkmStartDialog.Terms -> AppDialog(
            onDismiss = onDismiss,
            title = "Ketentuan Mulai Asesmen",
            body = if (strictMode) {
                "Mode ketat aktif: layar akan dikunci selama ujian dan kamu wajib online saat memulai. Keluar dari aplikasi atau membuka aplikasi lain akan dikenai sanksi."
            } else {
                "Mode ketat mati: pengerjaan boleh offline dan layar tidak dikunci. Keluar dari aplikasi tetap dapat dikenai sanksi bila penalti aktif."
            },
            primaryButtonText = "Saya Mengerti, Mulai",
            onPrimaryClick = onTermsAccepted,
            secondaryButtonText = "Simulasi mulai saat offline",
            onSecondaryClick = onSimulateOffline,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TermsRow(Icons.Rounded.Warning, "Keluar dari aplikasi atau berpindah aplikasi dikenai sanksi penalti.")
                    TermsRow(Icons.Rounded.Wifi, "Pastikan koneksi internet stabil saat mengunggah jawaban.")
                    TermsRow(Icons.Rounded.PhoneDisabled, "Jangan mengangkat telepon selama asesmen berlangsung.")
                }
            },
        )

        AkmStartDialog.Offline -> AppDialog(
            onDismiss = onDismiss,
            title = "Butuh Koneksi Internet",
            body = "Mode ketat aktif, jadi asesmen wajib dimulai saat online. Sambungkan internet lalu coba lagi — setelah mulai, pengerjaan boleh offline.",
            primaryButtonText = "Mengerti",
            onPrimaryClick = onDismiss,
            icon = Icons.Rounded.WifiOff,
            dismissible = false,
        )

        null -> Unit
    }
}

@Composable
private fun PasswordDialog(
    onDismiss: () -> Unit,
    checking: Boolean,
    error: String?,
    onCheck: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }

    AppDialog(
        onDismiss = onDismiss,
        title = "Password Ujian",
        body = "Masukkan password yang dibagikan pengawas di ruang ujian.",
        primaryButtonText = "Lanjut",
        onPrimaryClick = { if (password.isNotBlank()) onCheck(password) },
        primaryLoading = checking,
        secondaryButtonText = "Batal",
        onSecondaryClick = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password ujian",
                    enabled = !checking,
                )
                if (error != null) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
                if (checking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "Memeriksa password…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun TermsRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.extendedColors.warning,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
