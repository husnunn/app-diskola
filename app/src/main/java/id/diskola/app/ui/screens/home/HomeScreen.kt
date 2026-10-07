package id.diskola.app.ui.screens.home

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
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import id.diskola.app.utils.JurnalRow
import id.diskola.app.utils.JurnalRules
import id.diskola.app.ui.components.AdaptiveTileGrid
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.BuildConfig
import id.diskola.app.repository.FeatureGateRepository
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.FeatureUnavailableSheet
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.components.CounterBadge
import id.diskola.app.ui.components.iconFor
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import id.diskola.app.utils.session.SessionStore
import id.diskola.app.viewmodel.HomeViewModel
import id.diskola.app.viewmodel.formatScheduleClock

private data class HomeUser(val schoolName: String, val schoolCity: String, val name: String, val avatarUrl: String)

private fun readHomeUser(sessionStore: SessionStore): HomeUser {
    val school = sessionStore.school
    val user = sessionStore.user
    return HomeUser(
        schoolName = school.name.ifBlank { "Sekolah" },
        schoolCity = school.cityName,
        name = user.name.ifBlank { "Pengguna" },
        avatarUrl = user.avatar,
    )
}

/** Doc §1.8: routing keyed by role/feature identity, not grid position — the legacy app's
 * position-based routing silently breaks for a user who is somehow both `is_student` and
 * `is_teacher` (doc §1.8's ❓, not reproduced here since `MenuKey` never relies on position). */
enum class MenuKey { MATERI, TUGAS, PRESENSI, JURNAL, AGENDA_MINGGUAN, ASESMEN, POIN, MAGANG }

private data class MenuTileData(val key: MenuKey, val icon: String, val label: String, val implemented: Boolean, val locked: Boolean)

/** Doc §1.5's exact per-role item list — no "Agenda" (harian) tile (that was never in the doc's
 * table, only "Agenda Mingguan" for teachers) and no "Perpus" tile (also not in the table for any
 * role); both existed in this screen's pre-rebuild version and are dropped here. */
private fun menuTilesFor(sessionStore: SessionStore): List<MenuTileData> {
    val locked = !sessionStore.isHavingClass
    fun tile(key: MenuKey, icon: String, label: String, implemented: Boolean) =
        MenuTileData(key, icon, label, implemented, locked)
    return when {
        sessionStore.isStudent -> listOf(
            tile(MenuKey.MATERI, "auto_stories", "Materi", true),
            tile(MenuKey.TUGAS, "assignment", "Tugas", true),
            tile(MenuKey.PRESENSI, "how_to_reg", "Presensi", true),
            tile(MenuKey.JURNAL, "edit_note", "Jurnal", true),
            tile(MenuKey.ASESMEN, "quiz", "Asesmen", true),
            tile(MenuKey.POIN, "workspace_premium", "Poin", true),
            tile(MenuKey.MAGANG, "business_center", "Magang", false),
        )
        sessionStore.isTeacher -> listOf(
            tile(MenuKey.MATERI, "auto_stories", "Materi", true),
            tile(MenuKey.TUGAS, "assignment", "Tugas", true),
            tile(MenuKey.PRESENSI, "how_to_reg", "Presensi", true),
            tile(MenuKey.JURNAL, "edit_note", "Jurnal", true),
            tile(MenuKey.AGENDA_MINGGUAN, "event_note", "Agenda Mingguan", true),
            tile(MenuKey.POIN, "workspace_premium", "Poin", true),
        )
        else -> listOf(
            tile(MenuKey.MATERI, "auto_stories", "Materi", true),
            tile(MenuKey.TUGAS, "assignment", "Tugas", true),
            tile(MenuKey.PRESENSI, "how_to_reg", "Presensi", true),
            tile(MenuKey.JURNAL, "edit_note", "Jurnal", true),
            tile(MenuKey.POIN, "workspace_premium", "Poin", true),
        )
    }
}

@Composable
fun HomeScreen(
    onNavigateToMateri: () -> Unit,
    onNavigateToTugas: () -> Unit,
    onNavigateToPresensi: () -> Unit,
    onNavigateToPoin: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onOpenJurnal: (action: String, attendanceId: Int, plotId: Int) -> Unit,
    onNavigateToAsesmen: () -> Unit,
    onNavigateToNotifikasi: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val sessionStore = viewModel.sessionStore
    val sessionVersion by viewModel.sessionVersion.collectAsStateWithLifecycle()
    // Not `remember`ed, and re-evaluated whenever `sessionVersion` bumps (after `writeCheckAccount`
    // actually lands — `sessionStore` itself is a plain SharedPreferences reader, not an observable,
    // so nothing else would tell Compose to recompute this after the async response arrives).
    val menuTiles = remember(sessionVersion) { menuTilesFor(sessionStore) }
    var user by remember { mutableStateOf(readHomeUser(sessionStore)) }
    var comingSoonDialog by remember { mutableStateOf(false) }
    var lockedDialog by remember { mutableStateOf(false) }
    var verifyDialog by remember { mutableStateOf(false) }
    var guestPopupOpen by remember { mutableStateOf(false) }
    var guestPopupChecked by remember { mutableStateOf(false) }
    val ongoingClass by viewModel.ongoingClass.collectAsStateWithLifecycle()
    val scheduleLoaded by viewModel.scheduleLoaded.collectAsStateWithLifecycle()
    val accountInactive by viewModel.accountInactive.collectAsStateWithLifecycle()
    val agendaMissing by viewModel.agendaMissing.collectAsStateWithLifecycle()
    val featureChecking by viewModel.featureChecking.collectAsStateWithLifecycle()
    val featureUnavailable by viewModel.featureUnavailable.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun refreshAccount() {
        viewModel.refresh()
        user = readHomeUser(sessionStore)
        if (!guestPopupChecked) {
            guestPopupChecked = true
            if (sessionStore.isGuest) guestPopupOpen = true
        }
    }

    // Doc §1.4: `loadData()` re-runs on every onResume, not just the first landing — a single
    // lifecycle observer covers both, since `Lifecycle.addObserver` replays the current state
    // (including `ON_RESUME`) to a newly-added observer. A separate `LaunchedEffect(Unit)` here
    // would double-fire `check-account` on first launch (confirmed via device logcat).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshAccount()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showVerifyButton = sessionStore.isGuest || !sessionStore.isHavingClass

    fun onTileClick(tile: MenuTileData) {
        when {
            tile.locked -> lockedDialog = true
            tile.key == MenuKey.MATERI -> onNavigateToMateri()
            tile.key == MenuKey.TUGAS -> onNavigateToTugas()
            tile.key == MenuKey.PRESENSI -> viewModel.openFeature(FeatureGateRepository.PRESENSI, onNavigateToPresensi)
            tile.key == MenuKey.ASESMEN -> onNavigateToAsesmen()
            tile.key == MenuKey.POIN -> onNavigateToPoin()
            tile.key == MenuKey.JURNAL -> viewModel.openFeature(FeatureGateRepository.JURNAL_KBM) { onOpenJurnal("", 0, 0) }
            tile.key == MenuKey.AGENDA_MINGGUAN -> onNavigateToAgenda()
            else -> comingSoonDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHero(user = user, onNotifClick = onNavigateToNotifikasi, onRefresh = { refreshAccount() })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp)
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            Box(modifier = Modifier.offset(y = -HeroOverlap)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    if (showVerifyButton) {
                        VerifyDataButton(onClick = { verifyDialog = true })
                    } else {
                        OngoingClassCard(
                            item = ongoingClass,
                            loaded = scheduleLoaded,
                            isStudent = sessionStore.isStudent,
                            onHadiri = { row -> onOpenJurnal("hadiri", row.attendanceId ?: 0, row.plotId) },
                            onIsiJurnal = { row -> onOpenJurnal("isi", row.attendanceId ?: 0, row.plotId) },
                        )
                    }

                    Text("Menu Pembelajaran", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                    AdaptiveTileGrid(items = menuTiles) { tile, tileModifier ->
                        MenuGridTile(
                            tile = tile,
                            badge = if (tile.key == MenuKey.AGENDA_MINGGUAN) agendaMissing else 0,
                            onClick = { onTileClick(tile) },
                            modifier = tileModifier,
                        )
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

    if (featureChecking) AppLoadingDialog(message = "Memeriksa ketersediaan fitur…")
    featureUnavailable?.let { unavailable ->
        FeatureUnavailableSheet(
            isTeacher = sessionStore.isTeacher,
            serverMessage = unavailable.message,
            onOpenWebsite = {
                val path = when (unavailable.name) {
                    FeatureGateRepository.PRESENSI -> "/presensi/presensi-guru-staff/presensi"
                    FeatureGateRepository.JURNAL_KBM -> "/jurnal-kbm/presensi-kelas"
                    else -> ""
                }
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BuildConfig.PORTAL_URL + path)),
                    )
                }
                viewModel.dismissFeatureUnavailable()
            },
            onDismiss = viewModel::dismissFeatureUnavailable,
        )
    }
    if (comingSoonDialog) {
        AppDialog(
            onDismiss = { comingSoonDialog = false },
            title = "Fitur dalam pengembangan",
            body = "Menu ini sedang dalam proses pembangunan, ditunggu updatenya yah...",
            primaryButtonText = "Ok Deh",
            onPrimaryClick = { comingSoonDialog = false },
        )
    }
    if (lockedDialog) {
        AppDialog(
            onDismiss = { lockedDialog = false },
            title = "Fitur ini terkunci",
            body = "Oops, nampaknya anda belum terdaftar di kelas manapun. Hubungi admin sekolah anda untuk mengakses fitur ini",
            primaryButtonText = "Ok Deh",
            onPrimaryClick = { lockedDialog = false },
        )
    }
    if (guestPopupOpen && sessionStore.isGuest) {
        AppDialog(
            onDismiss = { guestPopupOpen = false },
            title = "Anda Masuk sebagai Tamu",
            body = "Beberapa fitur mungkin terbatas. Lakukan verifikasi data Jika Anda ingin menjadi guru atau siswa, untuk medapatkan akses penuh",
            primaryButtonText = "Oke, Terimakasi",
            onPrimaryClick = { guestPopupOpen = false },
        )
    }
    if (verifyDialog) {
        AppDialog(
            onDismiss = { verifyDialog = false },
            title = "VERIFIKASI DATA PENGGUNA",
            body = "Apakah NISN/NIS/NIK anda telah terdaftar disekolah anda?",
            primaryButtonText = "Sudah",
            onPrimaryClick = {
                verifyDialog = false
                comingSoonDialog = true
            },
            secondaryButtonText = "Belum",
            onSecondaryClick = {
                verifyDialog = false
                comingSoonDialog = true
            },
        )
    }
    if (accountInactive) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Peringatan",
            body = "Akun anda tidak aktif, hubungi admin sekolah untuk aktivasi",
            primaryButtonText = "Baik",
            onPrimaryClick = { viewModel.confirmInactiveLogout(onComplete = onLoggedOut) },
        )
    }
}

@Composable
private fun HomeHero(user: HomeUser, onNotifClick: () -> Unit, onRefresh: () -> Unit) {
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
            // Doc §1.4: the badge itself needs a `payment/wallet`→`notification/summary` chain that
            // doesn't exist anywhere in this app yet (confirmed by a full-repo search) — deferred,
            // see doc `05a`. The bell still opens Notifikasi.
            IconButton(onClick = onNotifClick) { Icon(Icons.Rounded.Notifications, contentDescription = "Notifikasi", tint = Color.White) }
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

/** Doc §1.4/§1.7's simplified "button active" cases (`role_label=Guest` or `!is_having_class`) —
 * the full `APPROVED`/`CLEAR`/`IN_REVIEW`/`REJECTED` approval-progress state machine needs a
 * `sosmedViewModel.checkUser()`-equivalent endpoint this app doesn't call anywhere yet, so the
 * greyed/red disabled-with-status-label states aren't reproduced (deferred, see doc `05a`). */
@Composable
private fun VerifyDataButton(onClick: () -> Unit) {
    AppButton(text = "verifikasi data", onClick = onClick, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun OngoingClassCard(
    item: JurnalRow?,
    loaded: Boolean,
    isStudent: Boolean,
    onHadiri: (JurnalRow) -> Unit,
    onIsiJurnal: (JurnalRow) -> Unit,
) {
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
                    val iconUrl = item?.iconUrl
                    if (!iconUrl.isNullOrBlank()) {
                        AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(modifier = Modifier.padding(start = Spacing.md)) {
                    Text(
                        item?.subjectName?.takeIf { it.isNotBlank() } ?: "Tidak terdapat jadwal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item != null) {
                        Text(
                            "${formatScheduleClock(item.start)} – ${formatScheduleClock(item.end)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (loaded) {
                        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Only students get the shortcuts, and only the one their row allows (legacy `PembelajaranPage`).
            val action = if (item != null && isStudent) JurnalRules.studentUi(item).action else JurnalRules.StudentAction.NONE
            if (item != null && action != JurnalRules.StudentAction.NONE) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (action == JurnalRules.StudentAction.HADIRI) {
                        AppButton(text = "Hadiri", onClick = { onHadiri(item) }, modifier = Modifier.weight(1f))
                    } else {
                        AppButton(text = "Isi Jurnal", onClick = { onIsiJurnal(item) }, variant = ButtonVariant.Tonal, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuGridTile(tile: MenuTileData, badge: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        if (!tile.locked && badge > 0) {
            CounterBadge(count = badge, max = 9, modifier = Modifier.align(Alignment.TopEnd))
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
