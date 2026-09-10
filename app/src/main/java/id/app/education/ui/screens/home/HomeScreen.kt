package id.app.education.ui.screens.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.app.education.ui.components.AdaptiveTileGrid
import id.app.education.ui.components.AppButton
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.ButtonVariant
import id.app.education.ui.components.CardVariant
import id.app.education.ui.components.iconFor
import id.app.education.ui.theme.HeroOverlap
import id.app.education.ui.theme.Spacing
import id.app.education.ui.theme.extendedColors
import androidx.compose.ui.graphics.Brush

private data class HomeUser(val schoolName: String, val schoolCity: String, val name: String, val avatarUrl: String)

private fun readHomeUser(context: Context): HomeUser {
    val session = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    return HomeUser(
        schoolName = session.getString("school_name", "").orEmpty().ifBlank { "Sekolah" },
        schoolCity = "",
        name = session.getString("user_name", "").orEmpty().ifBlank { "Pengguna" },
        avatarUrl = session.getString("user_avatar_image", "").orEmpty(),
    )
}

private data class MenuTileData(val icon: String, val label: String, val locked: Boolean, val implemented: Boolean)

/** Asesmen is a student-only module; teachers get Agenda Mingguan in that slot instead. */
private fun menuTilesFor(isTeacher: Boolean) = listOf(
    MenuTileData("auto_stories", "Materi", locked = false, implemented = true),
    MenuTileData("assignment", "Tugas", locked = false, implemented = false),
    MenuTileData("how_to_reg", "Presensi", locked = false, implemented = true),
    MenuTileData("edit_note", "Jurnal", locked = false, implemented = false),
    MenuTileData("event_note", "Agenda", locked = false, implemented = false),
    if (isTeacher) {
        MenuTileData("event_available", "Agenda Mingguan", locked = false, implemented = false)
    } else {
        MenuTileData("quiz", "Asesmen", locked = false, implemented = true)
    },
    MenuTileData("workspace_premium", "Poin", locked = false, implemented = false),
    MenuTileData("business_center", "Magang", locked = true, implemented = false),
    MenuTileData("menu_book", "Perpus", locked = true, implemented = false),
)

@Composable
fun HomeScreen(
    onNavigateToMateri: () -> Unit,
    onNavigateToAbsensi: () -> Unit,
    onNavigateToAsesmen: () -> Unit,
    onNavigateToNotifikasi: () -> Unit,
    modifier: Modifier = Modifier,
    isTeacher: Boolean = false,
) {
    val menuTiles = remember(isTeacher) { menuTilesFor(isTeacher) }
    val context = LocalContext.current
    var user by remember { mutableStateOf(readHomeUser(context)) }
    var comingSoonDialog by remember { mutableStateOf(false) }
    var lockedDialog by remember { mutableStateOf(false) }
    val unreadCount = 3 // TODO: source from NotifikasiViewModel once wired to a shared instance.

    fun onTileClick(tile: MenuTileData) {
        when {
            tile.label == "Materi" -> onNavigateToMateri()
            tile.label == "Presensi" -> onNavigateToAbsensi()
            tile.label == "Asesmen" -> onNavigateToAsesmen()
            tile.locked -> lockedDialog = true
            else -> comingSoonDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHero(user = user, unreadCount = unreadCount, onNotifClick = onNavigateToNotifikasi, onRefresh = { user = readHomeUser(context) })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp)
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            Box(modifier = Modifier.offset(y = -HeroOverlap)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    OngoingClassCard(onHadiri = onNavigateToAbsensi, onIsiJurnal = { comingSoonDialog = true })

                    Text("Menu Pembelajaran", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                    AdaptiveTileGrid(items = menuTiles) { tile, tileModifier ->
                        MenuGridTile(tile = tile, onClick = { onTileClick(tile) }, modifier = tileModifier)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(iconFor("image"), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Carousel banner · 3 slide", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }

    if (comingSoonDialog) {
        AlertDialog(
            onDismissRequest = { comingSoonDialog = false },
            title = { Text("Belum dibuat di mockup ini") },
            text = { Text("Modul Pembelajaran ini masuk fase berikutnya. Yang sudah jadi: Auth, Akun, Notifikasi, dan Pembayaran/Klaspay.") },
            confirmButton = { AppButton(text = "Mengerti", onClick = { comingSoonDialog = false }, variant = ButtonVariant.Text) },
        )
    }
    if (lockedDialog) {
        AlertDialog(
            onDismissRequest = { lockedDialog = false },
            title = { Text("Menu Belum Terbuka") },
            text = { Text("Anda belum tergabung di kelas manapun, jadi menu ini masih terkunci. Hubungi operator sekolah untuk penempatan kelas.") },
            confirmButton = { AppButton(text = "Mengerti", onClick = { lockedDialog = false }, variant = ButtonVariant.Text) },
        )
    }
}

@Composable
private fun HomeHero(user: HomeUser, unreadCount: Int, onNotifClick: () -> Unit, onRefresh: () -> Unit) {
    val extended = MaterialTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .background(
                Brush.verticalGradient(
                    0f to extended.brandGradientDark,
                    0.6f to extended.brandGradientMid,
                    1f to extended.brandGradientLight,
                ),
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(38.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    user.schoolName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.brandGradientDark,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.sm)) {
                Text(user.schoolName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = Color.White)
                if (user.schoolCity.isNotBlank()) {
                    Text(user.schoolCity, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.82f))
                }
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White) }
            Box {
                IconButton(onClick = onNotifClick) { Icon(Icons.Rounded.Notifications, contentDescription = "Notifikasi", tint = Color.White) }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(unreadCount.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(user.name.firstOrNull()?.uppercase().orEmpty(), style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
            Column(modifier = Modifier.padding(start = Spacing.md)) {
                Text("Assalamualaikum,", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.82f))
                Text(user.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun OngoingClassCard(onHadiri: () -> Unit, onIsiJurnal: () -> Unit) {
    AppCard(variant = CardVariant.Elevated) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.extendedColors.success, CircleShape))
                Text(
                    "KELAS BERLANGSUNG",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.extendedColors.success,
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor("code"), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.padding(start = Spacing.md)) {
                    Text("Informatika", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("07.30 – 09.00 · Ruang Lab 2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(text = "Hadiri", onClick = onHadiri, modifier = Modifier.weight(1f))
                AppButton(text = "Isi Jurnal", onClick = onIsiJurnal, variant = ButtonVariant.Tonal, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MenuGridTile(tile: MenuTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(88.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(iconFor(tile.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(tile.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        if (tile.locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = "Terkunci", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
            }
        }
    }
}
