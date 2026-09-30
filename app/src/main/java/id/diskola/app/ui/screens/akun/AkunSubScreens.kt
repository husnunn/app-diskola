package id.diskola.app.ui.screens.akun

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.diskola.app.dataclass.mock.MockAkun
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BadgeTone
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.StatusBadge
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@Composable
fun AkunSettingScreen(session: AkunSessionInfo, onBack: () -> Unit) {
    var nama by remember { mutableStateOf(session.name) }
    DetailScaffold(title = "Pengaturan akun", onBack = onBack, actions = {
        AppButton(text = "Simpan", onClick = {}, enabled = false, variant = ButtonVariant.Text)
    }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                "Perubahan nama dan email akan tampil di seluruh layanan Diskola.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusBadge(label = session.roleSub, tone = BadgeTone.Neutral)
            AppTextField(value = nama, onValueChange = { nama = it }, label = "Nama")
            AppTextField(value = session.email, onValueChange = {}, label = "Email", readOnly = true)
            if (session.email == "-" || true) {
                Text("Belum terverifikasi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
            AppButton(text = "Verifikasi email", onClick = {}, variant = ButtonVariant.Outlined)
            AppTextField(value = "", onValueChange = {}, label = "Nomor telepon", placeholder = "Belum diisi")
        }
    }
}

@Composable
fun AkunKontakScreen(session: AkunSessionInfo, onBack: () -> Unit) {
    DetailScaffold(title = "Kontak", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                "Email dipakai untuk pemulihan akun dan notifikasi penting dari sekolah.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(session.email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Belum terverifikasi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                        Text("Ubah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nomor telepon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Belum diisi", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Ubah", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            AppButton(text = "Kirim verifikasi email", onClick = {}, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
            AppButton(text = "Perbarui kontak", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AkunDevicesScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Perangkat", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                "Perangkat yang pernah masuk ke akun ini. Keluarkan perangkat yang tidak kamu kenali.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MockAkun.devices.forEach { device ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(id.diskola.app.ui.components.iconFor("devices"), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (device.isCurrent) {
                                    Icon(Icons.Rounded.Circle, contentDescription = null, tint = MaterialTheme.extendedColors.success, modifier = Modifier.size(8.dp))
                                    Spacer(modifier = Modifier.size(Spacing.xs))
                                }
                                Text(
                                    if (device.isCurrent) "Perangkat ini · ${device.lastUsedLabel}" else "Terakhir dipakai ${device.lastUsedLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (!device.isCurrent) {
                            AppButton(text = "Keluar", onClick = {}, variant = ButtonVariant.Outlined, modifier = Modifier.size(width = 96.dp, height = 40.dp))
                        }
                    }
                }
            }
            AppButton(text = "Keluar dari perangkat lain", onClick = {}, variant = ButtonVariant.Outlined, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AkunPassScreen(onBack: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    DetailScaffold(title = "Ubah kata sandi", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AppCard(variant = CardVariant.Filled) {
                Text(
                    "Jangan bagikan kata sandi kepada siapa pun, termasuk pihak yang mengaku dari sekolah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PasswordField("Kata sandi lama", old, { old = it }, oldVisible, { oldVisible = !oldVisible })
            PasswordField("Kata sandi baru", new, { new = it }, newVisible, { newVisible = !newVisible }, placeholder = "Minimal 8 karakter")
            PasswordField("Konfirmasi kata sandi baru", confirm, { confirm = it }, confirmVisible, { confirmVisible = !confirmVisible }, placeholder = "Ulangi kata sandi baru")
            AppButton(
                text = "simpan perubahan",
                onClick = {},
                enabled = new.length >= 8 && new == confirm && old.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    placeholder: String = "",
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            IconButton(onClick = onToggleVisible) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        },
    )
}

@Composable
fun AkunCardScreen(session: AkunSessionInfo, onBack: () -> Unit) {
    DetailScaffold(title = "Kartu Pelajar", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(96.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(session.name.firstOrNull()?.uppercase().orEmpty(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text(session.name, style = MaterialTheme.typography.titleMedium)
                StatusBadge(label = "Kelas 10 · Informatika", tone = BadgeTone.Neutral)
            }
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DataRow("NIS", "2410 0032")
                    DataRow("NISN", "0051234567")
                    DataRow("Nama lengkap", session.name)
                    DataRow("Tempat & tanggal lahir", "Jombang, 15 Mei 2009")
                    DataRow("Jenis kelamin", "Laki-laki")
                    DataRow("Angkatan", "2024")
                    DataRow("Alamat", "Jl. Merdeka No. 24, Tambakberas, Jombang")
                }
            }
            Text(
                "Data pribadi dikelola oleh sekolah. Hubungi operator sekolah bila ada yang perlu diperbaiki.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
fun AkunAboutScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Tentang Diskola", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(56.dp))
                Text("DISKOLA", style = MaterialTheme.typography.titleLarge)
                StatusBadge(label = "Versi 2.2.0", tone = BadgeTone.Neutral)
            }
            AppCard {
                Column {
                    DataRow("Dukungan", "support@diskola.id")
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    DataRow("Telepon", "(0321) 123456")
                }
            }
            Text("IKUTI KAMI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(text = "YouTube", onClick = {}, variant = ButtonVariant.Outlined, modifier = Modifier.weight(1f))
                AppButton(text = "Website", onClick = {}, variant = ButtonVariant.Outlined, modifier = Modifier.weight(1f))
            }
            AppCard(variant = CardVariant.Filled) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Yang Baru", style = MaterialTheme.typography.titleSmall)
                    BulletLine(MockAkun.CHANGELOG_1)
                    BulletLine(MockAkun.CHANGELOG_2)
                    BulletLine(MockAkun.CHANGELOG_3)
                }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
