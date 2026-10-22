package id.diskola.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.SecurityDialogRules
import kotlinx.coroutines.launch

// Texts below are the legacy ones (doc `02-auth-login-sesi.md` §8.2 and `08` §5.3), typos included.

/** "Aktivasi Wallet Lebih Dahulu" — shown when a wallet-only item is tapped while the wallet is inactive. */
@Composable
fun ActivateWalletFirstDialog(onActivate: () -> Unit, onLater: () -> Unit) {
    AppDialog(
        onDismiss = onLater,
        icon = Icons.Rounded.AccountBalanceWallet,
        title = "Aktivasi Wallet Lebih Dahulu",
        body = "Kamu belum mengaktifkan Wallet Payment, silahkan aktifkan Wallet Payment terlebih dahulu",
        primaryButtonText = "Aktivasi Sekarang",
        onPrimaryClick = onActivate,
        secondaryButtonText = "Nanti Saja",
        onSecondaryClick = onLater,
    )
}

/**
 * "Masukkan Email": the address can only be changed through the Google account picker ([onPickEmail] returns the chosen address or
 * null), like legacy — the field itself is read-only.
 */
@Composable
fun EmailSettingDialog(
    initialEmail: String,
    onPickEmail: suspend () -> String?,
    onVerify: (String) -> Unit,
    onLater: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var picking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AppDialog(
        onDismiss = onLater,
        icon = Icons.Rounded.MarkEmailRead,
        title = "Masukkan Email",
        body = "Daftarkan email Anda, fitur Pembayaran hanya akan berjalan dengan email yang terverifikasi",
        primaryButtonText = "Verifikasi Sekarang",
        onPrimaryClick = { onVerify(email.trim()) },
        primaryEnabled = SecurityDialogRules.canSendVerification(email.trim()),
        secondaryButtonText = "Nanti Saja",
        onSecondaryClick = onLater,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppTextField(
                value = email,
                onValueChange = {},
                label = "Masukkan Email",
                placeholder = "Pilih email lewat akun Google",
                readOnly = true,
                keyboardType = KeyboardType.Email,
                trailing = {
                    IconButton(
                        enabled = !picking,
                        onClick = {
                            picking = true
                            scope.launch {
                                onPickEmail()?.let { email = it }
                                picking = false
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Pilih email dari akun Google")
                    }
                },
            )
            Text(
                "Silahkan menghubungi admin untuk memperabarui email anda!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
fun EmailConfirmDialog(onOk: () -> Unit) {
    AppDialog(
        onDismiss = onOk,
        icon = Icons.Rounded.MarkEmailRead,
        title = "Verifikasi Email Sekarang",
        body = "Email verifikasi telah dikirimkan, silahkan cek email yang telah didaftarkan",
        primaryButtonText = "Ok Saya Konfirmasi Dulu",
        onPrimaryClick = onOk,
    )
}

/** The e-mail was refused (already used by another account) or the verification mail could not be sent. */
@Composable
fun EmailUsedDialog(message: String, onClose: () -> Unit) {
    AppDialog(
        onDismiss = onClose,
        dismissible = false,
        title = "PEMBERITAHUAN",
        body = "Email sudah digunakan oleh akun:\n$message",
        primaryButtonText = "Tutup",
        onPrimaryClick = onClose,
    )
}

/**
 * "Ubah Password" for accounts still on the default password. [cancelable] is false while the wallet is active: "Nanti Saja"
 * disappears and the dialog cannot be dismissed. [onInvalid] receives the validation text (legacy showed it as a toast).
 */
@Composable
fun ChangePasswordDialog(
    cancelable: Boolean,
    onSubmit: (old: String, new: String) -> Unit,
    onInvalid: (String) -> Unit,
    onLater: () -> Unit,
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }

    AppDialog(
        onDismiss = onLater,
        dismissible = cancelable,
        icon = Icons.Rounded.Lock,
        title = "Ubah Password",
        body = "Password belum diperbarui, untuk keamanan akun Anda, silahkan ganti password lama Anda",
        primaryButtonText = "Buat Password Baru",
        onPrimaryClick = {
            val problem = SecurityDialogRules.validatePasswordChange(old, new, confirmation)
            if (problem != null) onInvalid(problem) else confirming = true
        },
        primaryEnabled = SecurityDialogRules.canSubmitPassword(old, new, confirmation),
        secondaryButtonText = if (cancelable) "Nanti Saja" else null,
        onSecondaryClick = if (cancelable) onLater else null,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PasswordField(old, { old = it }, "Password lama", "Masukkan password lama")
            PasswordField(new, { new = it }, "Password baru", "Masukkan password baru")
            PasswordField(confirmation, { confirmation = it }, "Konfirmasi password", "Konfirmasi password baru")
        }
    }

    if (confirming) {
        AppDialog(
            onDismiss = { confirming = false },
            title = "Ubah Password Sekarang?",
            body = "Perubahan password baru akan segera diproses, Anda yakin untuk memperbarui password Anda?",
            primaryButtonText = "Saya Yakin",
            onPrimaryClick = {
                confirming = false
                onSubmit(old, new)
            },
            secondaryButtonText = "Ketik Ulang",
            onSecondaryClick = { confirming = false },
        )
    }
}

@Composable
fun PasswordChangedDialog(onOk: () -> Unit) {
    AppDialog(
        onDismiss = onOk,
        dismissible = false,
        icon = Icons.Rounded.CheckCircle,
        title = "Password Diperbarui",
        body = "Selamat, passwordmu berhasil diperbarui, sekarang kamu bisa login dengan password baru",
        primaryButtonText = "Ok, Kembali",
        onPrimaryClick = onOk,
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    var visible by remember { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Sembunyikan password" else "Tampilkan password",
                )
            }
        },
    )
}
