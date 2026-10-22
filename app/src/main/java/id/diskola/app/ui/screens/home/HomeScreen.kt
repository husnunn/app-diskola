package id.diskola.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.BuildConfig
import id.diskola.app.dataclass.ResponData.CurrentSubjectData
import id.diskola.app.repository.FeatureGateRepository
import id.diskola.app.ui.components.AdaptiveTileGrid
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.FeatureUnavailableSheet
import id.diskola.app.ui.screens.jurnal.JurnalMethodSheet
import id.diskola.app.ui.theme.HeroOverlap
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.HubRules
import id.diskola.app.utils.JurnalRules
import id.diskola.app.viewmodel.HomeUiState
import id.diskola.app.viewmodel.HomeViewModel

/**
 * Pembelajaran hub (legacy `PembelajaranPage`). Data comes from [HomeViewModel] (check-account → class/verification, session
 * guard, bell, agenda badge, teacher banner); this composable only lays it out and routes taps. Student → "Kelas Berlangsung",
 * teacher/staff → attendance banner, guest or class-less → "Verifikasi Data".
 */
@Composable
fun HomeScreen(
    onNavigateToMateri: () -> Unit,
    onNavigateToTugas: () -> Unit,
    onNavigateToPresensi: () -> Unit,
    onNavigateToPoin: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onOpenJurnal: () -> Unit,
    onOpenVerifikasiJurnal: (attendanceId: Int, title: String) -> Unit,
    onOpenJurnalForm: (plotId: Int, plotLabel: String) -> Unit,
    onNavigateToAsesmen: () -> Unit,
    onNavigateToNotifikasi: () -> Unit,
    onOpenKlaspayActivation: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accountInactive by viewModel.accountInactive.collectAsStateWithLifecycle()
    val deviceConflict by viewModel.deviceConflict.collectAsStateWithLifecycle()
    val guestPopup by viewModel.guestPopup.collectAsStateWithLifecycle()
    val logoutBlocked by viewModel.logoutBlocked.collectAsStateWithLifecycle()
    val featureChecking by viewModel.featureChecking.collectAsStateWithLifecycle()
    val featureUnavailable by viewModel.featureUnavailable.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val profile = state.profile
    val menuTiles = remember(profile) { menuTilesFor(profile) }

    var comingSoonDialog by rememberSaveable { mutableStateOf(false) }
    var lockedDialog by rememberSaveable { mutableStateOf(false) }
    var verifyDialog by rememberSaveable { mutableStateOf(false) }
    var checkInSheet by rememberSaveable { mutableStateOf(false) }

    // A failed check-account is a toast with the reason, as in legacy; the stored profile keeps being shown.
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Legacy `loadData()` runs on every onResume, first landing included (the observer replays the current state), so no
    // separate LaunchedEffect — that would fire check-account twice on launch.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onBell() {
        if (profile.klaspayActive) {
            onNavigateToNotifikasi()
        } else {
            Toast.makeText(context, HubRules.BELL_NEEDS_WALLET, Toast.LENGTH_LONG).show()
            onOpenKlaspayActivation()
        }
    }

    fun onTileClick(tile: MenuTileData) {
        when {
            tile.locked -> lockedDialog = true
            tile.key == MenuKey.MATERI -> onNavigateToMateri()
            tile.key == MenuKey.TUGAS -> onNavigateToTugas()
            tile.key == MenuKey.PRESENSI -> viewModel.openFeature(FeatureGateRepository.PRESENSI, onNavigateToPresensi)
            tile.key == MenuKey.ASESMEN -> onNavigateToAsesmen()
            tile.key == MenuKey.POIN -> onNavigateToPoin()
            tile.key == MenuKey.JURNAL -> viewModel.openFeature(FeatureGateRepository.JURNAL_KBM, onOpenJurnal)
            tile.key == MenuKey.AGENDA_MINGGUAN -> onNavigateToAgenda()
            else -> comingSoonDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHeader(
            profile = profile,
            bellCount = state.bellCount,
            loading = state.loading,
            onRefresh = viewModel::refresh,
            onBell = ::onBell,
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {
            Box(modifier = Modifier.offset(y = -HeroOverlap)) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    CardArea(
                        state = state,
                        onVerify = { verifyDialog = true },
                        onCheckIn = { checkInSheet = true },
                        onIsiJurnal = { subject ->
                            val label = "${JurnalRules.shortClock(subject.plot_start_at)} - ${JurnalRules.shortClock(subject.plot_end_at)}"
                            onOpenJurnalForm(subject.id ?: 0, label)
                        },
                        onPresensi = { viewModel.openFeature(FeatureGateRepository.PRESENSI, onNavigateToPresensi) },
                    )

                    Text(
                        "Menu Pembelajaran",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    AdaptiveTileGrid(items = menuTiles) { tile, tileModifier ->
                        MenuGridTile(
                            tile = tile,
                            badge = if (tile.key == MenuKey.AGENDA_MINGGUAN) state.agendaMissing else 0,
                            onClick = { onTileClick(tile) },
                            modifier = tileModifier,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.huge))
                }
            }
        }
    }

    if (checkInSheet) {
        val subject = state.currentSubject
        JurnalMethodSheet(
            onDismiss = { checkInSheet = false },
            onVerifikasi = {
                checkInSheet = false
                subject?.let { onOpenVerifikasiJurnal(it.attendance_id ?: 0, HubRules.className(it)) }
            },
        )
    }

    if (featureChecking) AppLoadingDialog(message = "Memeriksa ketersediaan fitur…")
    featureUnavailable?.let { unavailable ->
        FeatureUnavailableSheet(
            isTeacher = profile.isTeacher,
            serverMessage = unavailable.message,
            onOpenWebsite = {
                val path = when (unavailable.name) {
                    FeatureGateRepository.PRESENSI -> "/presensi/presensi-guru-staff/presensi"
                    FeatureGateRepository.JURNAL_KBM -> "/jurnal-kbm/presensi-kelas"
                    else -> ""
                }
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PORTAL_URL + path))) }
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
    if (guestPopup && profile.isGuest) {
        AppDialog(
            onDismiss = viewModel::dismissGuestPopup,
            title = "Anda Masuk sebagai Tamu",
            body = "Beberapa fitur mungkin terbatas. Lakukan verifikasi data Jika Anda ingin menjadi guru atau siswa, untuk medapatkan akses penuh",
            primaryButtonText = "Oke, Terimakasi",
            onPrimaryClick = viewModel::dismissGuestPopup,
        )
    }
    if (verifyDialog) {
        // Both answers lead to `VerificationPage` in legacy (with `requestApproval` for "Belum"); that page is a later phase.
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
            onPrimaryClick = { viewModel.confirmLogout(onComplete = onLoggedOut) },
        )
    }
    if (deviceConflict) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Perangkat Lain Terdeteksi",
            body = "Akun Anda sedang aktif di perangkat lain. Silakan logout dari perangkat tersebut terlebih dahulu.\n\n" +
                "Catatan: Silahkan hubungi admin sekolah untuk menjalankan reset perangkat pengguna",
            primaryButtonText = "Mengerti",
            onPrimaryClick = { viewModel.confirmLogout(onComplete = onLoggedOut) },
        )
    }
    if (logoutBlocked) {
        AppDialog(
            onDismiss = viewModel::consumeLogoutBlocked,
            title = "Perhatian",
            body = "Masih terdapat ujian yang belum diselesaikan, silahkan kumpulkan ujian terlebih dahulu agar nilai ujian terproses",
            primaryButtonText = "OK",
            onPrimaryClick = {
                viewModel.consumeLogoutBlocked()
                onNavigateToAsesmen()
            },
        )
    }
}

/** What fills the slot under the header — decided by [HubRules.verifyState] and the role (legacy `PembelajaranPage.kt:156-208`). */
@Composable
private fun CardArea(
    state: HomeUiState,
    onVerify: () -> Unit,
    onCheckIn: () -> Unit,
    onIsiJurnal: (CurrentSubjectData) -> Unit,
    onPresensi: () -> Unit,
) {
    val profile = state.profile
    when (val verify = state.verify) {
        HubRules.VerifyState.VerifyAvailable -> VerifyDataButton(onClick = onVerify)
        is HubRules.VerifyState.VerifyBlocked -> VerifyBlockedCard(verify.message, verify.rejected)
        HubRules.VerifyState.StudentNoClass -> StudentNoClassCard()
        HubRules.VerifyState.ShowContent -> if (profile.isStudent) {
            StudentClassCard(
                subject = state.currentSubject,
                schoolLogo = profile.schoolLogo,
                action = HubRules.classAction(state.currentSubject, profile.isStudent, profile.isHavingClass),
                onCheckIn = onCheckIn,
                onIsiJurnal = { state.currentSubject?.let(onIsiJurnal) },
            )
        } else {
            AttendanceBannerCard(banner = state.banner, loading = state.loading, onPresensi = onPresensi)
        }
    }
}
