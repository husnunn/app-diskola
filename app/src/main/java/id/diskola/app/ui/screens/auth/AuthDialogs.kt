package id.diskola.app.ui.screens.auth

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import id.diskola.app.viewmodel.LoginViewModel

/** Dialog 7 — Syarat & Ketentuan: scrollable WebView loading `GET policy`'s HTML content. */
@Composable
fun TermsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val policy by viewModel.policy.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.fetchPolicy() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl)
                .background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog)
                .padding(Spacing.xl),
        ) {
            Column {
                Text("Syarat & Ketentuan", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(Spacing.md))
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    val content = policy?.data?.content
                    if (content.isNullOrBlank()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        AndroidView(
                            factory = { context -> WebView(context) },
                            update = { webView -> webView.loadData(content, "text/html", "UTF-8") },
                            modifier = Modifier.fillMaxWidth().height(360.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.size(Spacing.md))
                AppButton(text = "SAYA PAHAM", onClick = onAccept, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Dialog 8 — "Pemberitahuan": guest-confirmation gate before `SsoScreen` (doc
 * `02-auth-login-sesi.md` §5.2, decision Q1 — shown exactly when `data == null || data.school ==
 * null`, replicated as-is including the original race condition; see [id.diskola.app.viewmodel.SsoViewModel]).
 * The match is case-sensitive on purpose (same as the legacy app) — [KeyboardCapitalization.None]
 * on the input is the one deliberate deviation, so the keyboard doesn't fight that match by
 * auto-capitalizing the first letter the way the legacy XML `EditText` did.
 */
@Composable
fun GuestConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val phrase = "lanjutkan sebagai tamu dulu bukan siswa/guru"
    val canContinue = input == phrase

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog)
                .padding(Spacing.xxl),
        ) {
            Column {
                Text("Pemberitahuan", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(Spacing.md))
                Text(
                    "Email Anda belum terdaftar di sistem Diskola.\nJika dilanjutkan, akun akan dibuat dengan level Guest (bukan siswa/guru).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(Spacing.md))
                Text(
                    "Untuk melanjutkan, ketik teks berikut secara sama persis:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(phrase, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.size(Spacing.md))
                AppTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Ketik teks konfirmasi",
                    capitalization = KeyboardCapitalization.None,
                )
                Spacer(modifier = Modifier.size(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AppButton(
                        text = "Tutup",
                        onClick = onDismiss,
                        variant = ButtonVariant.Outlined,
                        modifier = Modifier.weight(1f),
                    )
                    AppButton(text = "Lanjutkan", onClick = onConfirm, enabled = canContinue, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Dialog — "Perangkat Lain Terdeteksi": `DEVICE_CONFLICT` (409), doc §4.3/§9. */
@Composable
fun DeviceConflictDialog(message: String, onDismiss: () -> Unit) {
    id.diskola.app.ui.components.AppDialog(
        onDismiss = onDismiss,
        title = "Perangkat Lain Terdeteksi",
        body = message,
        primaryButtonText = "Mengerti",
        onPrimaryClick = onDismiss,
        content = {
            Text(
                "Catatan: Silahkan hubungi admin sekolah untuk menjalankan reset perangkat pengguna",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
    )
}

/** Dialog 9 — Update Wajib: non-dismissible, opens Play Store. */
@Composable
fun UpdateRequiredDialog(onUpdateClick: () -> Unit) {
    id.diskola.app.ui.components.AppDialog(
        onDismiss = {},
        dismissible = false,
        icon = Icons.Rounded.SystemUpdate,
        title = "Update Tersedia",
        body = "Versi baru aplikasi Diskola sudah tersedia. Perbarui aplikasi untuk melanjutkan.",
        primaryButtonText = "Update",
        onPrimaryClick = onUpdateClick,
    )
}
