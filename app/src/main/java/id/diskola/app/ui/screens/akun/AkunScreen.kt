package id.diskola.app.ui.screens.akun

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.screens.auth.clearGoogleCredentialState
import kotlinx.coroutines.launch
import id.diskola.app.ui.components.ListRow
import id.diskola.app.ui.components.iconFor
import id.diskola.app.ui.navigation.Route
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.AkunViewModel

data class AkunSessionInfo(
    val name: String,
    val email: String,
    val roleSub: String,
    val avatarUrl: String,
    val isStudent: Boolean,
)

fun readAkunSession(context: Context, viewModel: AkunViewModel): AkunSessionInfo {
    val session = viewModel.sessionStore
    val isTeacher = session.isTeacher
    val isStudent = session.isStudent || !isTeacher
    return AkunSessionInfo(
        name = session.user.name.ifBlank { "-" },
        email = session.user.email.ifBlank { "-" },
        roleSub = if (isTeacher) "Guru Mapel" else "Siswa",
        avatarUrl = session.user.avatar,
        isStudent = isStudent,
    )
}

@Composable
fun AkunScreen(
    onLoggedOut: () -> Unit,
    onNavigate: (Route.AkunSub) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAkm: () -> Unit = {},
    viewModel: AkunViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val session = remember { readAkunSession(context, viewModel) }
    var logoutDialog by remember { mutableStateOf(false) }
    var termsDialog by remember { mutableStateOf(false) }
    val logoutBlocked by viewModel.logoutBlocked.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Akun",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (session.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = session.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(session.name.firstOrNull()?.uppercase().orEmpty(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(
                    modifier = Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor("photo_camera"), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.size(Spacing.md))
            Text(session.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("@${session.name.substringBefore(" ").lowercase()} · ${session.roleSub}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.size(Spacing.lg))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(14.dp))
                .clickable { }
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "Email belum terverifikasi. Verifikasi untuk mengamankan akun.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm),
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionLabel("AKUN")
            AppCard(variant = CardVariant.Elevated, contentPadding = PaddingValues(0.dp)) {
                Column {
                    ListRow(title = "Pengaturan akun", subtitle = "Nama, email, nomor telepon", leadingIcon = { Icon(iconFor("manage_accounts"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { onNavigate(Route.AkunSub.Setting) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    ListRow(title = "Kontak", subtitle = "Verifikasi email & nomor telepon", leadingIcon = { Icon(iconFor("alternate_email"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { onNavigate(Route.AkunSub.Kontak) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    ListRow(title = "Ubah kata sandi", subtitle = "Terakhir diubah 4 bulan lalu", leadingIcon = { Icon(iconFor("lock_reset"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { onNavigate(Route.AkunSub.Pass) })
                    if (session.isStudent) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                        ListRow(title = "Kartu pelajar", subtitle = "Data pribadi siswa", leadingIcon = { Icon(iconFor("badge"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { onNavigate(Route.AkunSub.Card) })
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    ListRow(title = "Perangkat", subtitle = "3 sesi aktif", leadingIcon = { Icon(iconFor("devices"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { onNavigate(Route.AkunSub.Devices) })
                }
            }

            Spacer(modifier = Modifier.size(Spacing.lg))
            SectionLabel("TENTANG")
            AppCard(variant = CardVariant.Elevated, contentPadding = PaddingValues(0.dp)) {
                Column {
                    ListRow(title = "Syarat & ketentuan", leadingIcon = { Icon(iconFor("gavel"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { termsDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    ListRow(title = "Tentang Diskola", subtitle = "Versi 2.2.0", leadingIcon = { Icon(iconFor("info"), null, tint = MaterialTheme.colorScheme.primary) }, onClick = { onNavigate(Route.AkunSub.About) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
                    ListRow(
                        title = "Keluar",
                        leadingIcon = {
                            Box(modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(iconFor("logout"), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = { logoutDialog = true },
                        trailing = {},
                    )
                }
            }
            Spacer(modifier = Modifier.size(Spacing.huge))
        }
    }

    if (logoutDialog) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { logoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Anda yakin akan keluar dari aplikasi?") },
            confirmButton = {
                AppButton(text = "Logout", variant = ButtonVariant.Text, onClick = {
                    logoutDialog = false
                    scope.launch { clearGoogleCredentialState(context) }
                    viewModel.logout(onComplete = onLoggedOut)
                })
            },
            dismissButton = { AppButton(text = "Batal", variant = ButtonVariant.Text, onClick = { logoutDialog = false }) },
        )
    }

    if (logoutBlocked) {
        AppDialog(
            onDismiss = { viewModel.consumeLogoutBlocked() },
            icon = Icons.Rounded.Warning,
            title = "Perhatian",
            body = "Masih terdapat ujian yang belum diselesaikan, silahkan kumpulkan ujian terlebih dahulu agar nilai ujian terproses",
            primaryButtonText = "OK",
            onPrimaryClick = {
                viewModel.consumeLogoutBlocked()
                onOpenAkm()
            },
        )
    }

    if (termsDialog) {
        AlertDialog(
            onDismissRequest = { termsDialog = false },
            title = { Text("Syarat & Ketentuan") },
            text = { Text("Lihat kebijakan lengkap di Tentang Diskola.") },
            confirmButton = { AppButton(text = "Tutup", variant = ButtonVariant.Text, onClick = { termsDialog = false }) },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs),
    )
}
