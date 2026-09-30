package id.diskola.app.ui.screens.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.R
import id.diskola.app.ui.components.AmbientGradientBackground
import id.diskola.app.ui.components.AppBottomSheet
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.contentContainer
import id.diskola.app.utils.session.AuthError
import id.diskola.app.viewmodel.LoginOutcome
import id.diskola.app.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

private const val CONTACT_WHATSAPP_NUMBER = "6287887219649"

@Composable
fun PasswordScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onResetPassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mustChangePasswordDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val account by viewModel.checkAccountResult.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val loginOutcome by viewModel.loginOutcome.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
        delay(200)
        keyboardController?.show()
    }

    LaunchedEffect(loginOutcome) {
        when (loginOutcome) {
            LoginOutcome.MustChangePassword -> mustChangePasswordDialog = true
            LoginOutcome.Success -> onLoginSuccess()
            null -> Unit
        }
        viewModel.consumeLoginOutcome()
    }

    val roleLabel = account?.roles?.firstOrNull()?.name?.uppercase() ?: "PENGGUNA"
    val className = account?.student?.student_class?.class_room?.name?.takeIf { it.isNotBlank() }
    val identityLine = account?.nis_nik?.takeIf { it.isNotBlank() }?.let { "NISN $it" }.orEmpty()

    AmbientGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                Spacer(modifier = Modifier.weight(1f))
                Image(painter = painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(30.dp).padding(end = Spacing.lg))
            }

            Column(
                modifier = Modifier
                    .contentContainer()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "masuk sebagai".uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    val avatar = account?.user_avatar_image.orEmpty()
                    if (avatar.isNotBlank()) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(84.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                account?.name?.firstOrNull()?.uppercase().orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text(account?.name.orEmpty().ifBlank { "-" }, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.size(Spacing.xs))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        StatusBadge(label = roleLabel, tone = BadgeTone.Neutral)
                        if (!className.isNullOrBlank()) {
                            Text(className, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (identityLine.isNotBlank()) {
                        Text(identityLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                AppCard(variant = CardVariant.Outlined) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        AppTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Masukkan Password",
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            capitalization = KeyboardCapitalization.None,
                            modifier = Modifier.focusRequester(focusRequester),
                            trailing = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Lupa password? ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Reset",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onResetPassword),
                            )
                        }
                        AppButton(
                            text = "login",
                            onClick = { viewModel.loginAccount(password) },
                            enabled = password.isNotBlank(),
                            loading = loading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                AppCard(variant = CardVariant.Filled) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("NISN/NIK terdaftar bukan milik Anda?", style = MaterialTheme.typography.bodySmall)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { openWhatsAppOrCall(context, CONTACT_WHATSAPP_NUMBER) },
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                "Hubungi Kami",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }
                }
            }
        }
    }

    if (loading) {
        AppLoadingDialog(message = "Proses login akun")
    }

    when (val error = authError) {
        is AuthError.DeviceConflict -> DeviceConflictDialog(message = error.message, onDismiss = { viewModel.clearAuthError() })
        is AuthError.InvalidPassword -> LoginFailedSheet(message = error.message, onDismiss = { viewModel.clearAuthError() })
        is AuthError.Message -> LoginFailedSheet(message = error.message, onDismiss = { viewModel.clearAuthError() })
        null -> Unit
    }

    if (mustChangePasswordDialog) {
        AppDialog(
            onDismiss = { mustChangePasswordDialog = false; onLoginSuccess() },
            title = "Ganti Password",
            body = "Anda masih menggunakan password default. Segera ganti password Anda demi keamanan akun.",
            primaryButtonText = "Mengerti",
            onPrimaryClick = { mustChangePasswordDialog = false; onLoginSuccess() },
        )
    }
}

/** "Hubungi Kami" — doc §4.3: WhatsApp first, falls back to a phone call if WhatsApp isn't installed. */
private fun openWhatsAppOrCall(context: Context, phone: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.whatsapp")
                data = Uri.parse("https://wa.me/$phone")
            }
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginFailedSheet(message: String, onDismiss: () -> Unit) {
    AppBottomSheet(title = "Login Gagal", onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppButton(text = "Coba Lagi", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
