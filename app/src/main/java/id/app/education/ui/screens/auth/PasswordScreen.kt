package id.app.education.ui.screens.auth

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.app.education.R
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.AppDialog
import id.app.education.ui.components.AppTextField
import id.app.education.ui.components.BadgeTone
import id.app.education.ui.components.BannerError
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.CardVariant
import id.app.education.ui.components.StatusBadge
import id.app.education.ui.navigation.Route
import id.app.education.ui.theme.ScreenHorizontalPadding
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.contentContainer
import id.app.education.viewmodel.AuthViewModel

@Composable
fun PasswordScreen(
    route: Route.Auth.Password,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onResetPassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mustChangePasswordDialog by remember { mutableStateOf(false) }

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val loginAccountResult by viewModel.loginAccountResult.collectAsStateWithLifecycle()
    val checkAccountResult by viewModel.checkAccountResult.collectAsStateWithLifecycle()

    LaunchedEffect(loginAccountResult) {
        loginAccountResult?.let { result ->
            val mustChange = viewModel.saveSession(result, checkAccountResult)
            if (mustChange) {
                mustChangePasswordDialog = true
            } else {
                onLoginSuccess()
            }
        }
    }

    val roleLabel = when {
        route.isStudent -> "SISWA"
        route.isTeacher -> "GURU"
        route.ruleLabel.isNotBlank() -> route.ruleLabel.uppercase()
        else -> "PENGGUNA"
    }
    val subLabel = when {
        route.isStudent -> "Siswa"
        route.isTeacher -> "Guru Mapel"
        else -> route.ruleLabel
    }
    val identityLine = route.userNisNik.takeIf { it.isNotBlank() }?.let { "NISN $it" }.orEmpty()

    id.app.education.ui.components.AmbientGradientBackground(modifier = modifier.fillMaxSize()) {
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
                        "MASUK SEBAGAI",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    if (route.userAvatarImage.isNotBlank()) {
                        AsyncImage(
                            model = route.userAvatarImage,
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
                                route.userName.firstOrNull()?.uppercase().orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text(route.userName.ifBlank { "-" }, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.size(Spacing.xs))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        StatusBadge(label = roleLabel, tone = BadgeTone.Neutral)
                        if (subLabel.isNotBlank()) {
                            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (identityLine.isNotBlank()) {
                        Text(identityLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (errorMessage.isNotBlank()) {
                    BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
                }

                AppCard(variant = CardVariant.Outlined) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        AppTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
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
                            text = "Login",
                            onClick = { viewModel.loginAccount(route.userUuid, password) },
                            enabled = password.length >= 4,
                            loading = loading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                AppCard(variant = CardVariant.Filled) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Hubungi Kami", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("WhatsApp operator · 0812-3456-7890", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = Spacing.sm))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Telepon sekolah · (0321) 123456", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = Spacing.sm))
                        }
                    }
                }
            }
        }
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
