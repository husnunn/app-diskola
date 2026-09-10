package id.app.education.ui.screens.auth

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppTextField
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.theme.DiskolaExtraShapes
import id.app.education.ui.theme.Spacing
import androidx.compose.ui.unit.sp
import id.app.education.viewmodel.AuthViewModel

/** Dialog 7 — Syarat & Ketentuan: scrollable WebView loading `GET policy`'s HTML content. */
@Composable
fun TermsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
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

/** Dialog 8 — Konfirmasi Lanjut Sebagai Tamu: phrase-match gate before continuing as guest. */
@Composable
fun GuestConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val phrase = "SAYA MENGERTI"
    val canContinue = input.trim().uppercase() == phrase

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, DiskolaExtraShapes.dialog)
                .padding(Spacing.xxl),
        ) {
            Column {
                Text("Konfirmasi", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(Spacing.md))
                Text(
                    "Akun Google ini belum terhubung ke sekolah manapun. Anda akan melanjutkan sebagai tamu dengan akses terbatas. Ketik frasa berikut untuk melanjutkan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(Spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(phrase, style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.sp), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.size(Spacing.md))
                AppTextField(value = input, onValueChange = { input = it }, placeholder = "Ketik frasa di atas")
                Spacer(modifier = Modifier.size(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AppButton(text = "Batal", onClick = onDismiss, variant = ButtonVariant.Outlined, modifier = Modifier.weight(1f))
                    AppButton(text = "Lanjut", onClick = onConfirm, enabled = canContinue, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Dialog 9 — Update Wajib: non-dismissible, opens Play Store. */
@Composable
fun UpdateRequiredDialog(onUpdateClick: () -> Unit) {
    id.app.education.ui.components.AppDialog(
        onDismiss = {},
        dismissible = false,
        icon = Icons.Rounded.SystemUpdate,
        title = "Update Tersedia",
        body = "Versi baru aplikasi Diskola sudah tersedia. Perbarui aplikasi untuk melanjutkan.",
        primaryButtonText = "Update",
        onPrimaryClick = onUpdateClick,
    )
}
