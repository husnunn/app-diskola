package id.diskola.app.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.diskola.app.dataclass.mock.NotificationItem
import java.time.LocalDate
import id.diskola.app.dataclass.mock.formatRupiah
import id.diskola.app.ui.screens.jurnal.JurnalDetailScreen
import id.diskola.app.ui.screens.jurnal.JurnalFormScreen
import id.diskola.app.ui.screens.jurnal.JurnalScreen
import id.diskola.app.ui.screens.jurnal.VerifikasiJurnalScreen
import id.diskola.app.ui.screens.presensi.IzinAddScreen
import id.diskola.app.ui.screens.presensi.IzinDetailScreen
import id.diskola.app.ui.screens.presensi.IzinListScreen
import id.diskola.app.ui.screens.presensi.PresensiMasukScreen
import id.diskola.app.ui.screens.presensi.CaptureCameraConfig
import id.diskola.app.ui.screens.presensi.CaptureCameraScreen
import id.diskola.app.ui.screens.presensi.PresensiOffsiteScreen
import id.diskola.app.ui.screens.presensi.PresensiScreen
import id.diskola.app.ui.screens.agenda.AgendaCheckScreen
import id.diskola.app.ui.screens.agenda.AgendaDetailScreen
import id.diskola.app.ui.screens.agenda.AgendaMingguanScreen
import id.diskola.app.ui.screens.akun.AkunAboutScreen
import id.diskola.app.ui.screens.akun.AkunCardScreen
import id.diskola.app.ui.screens.akun.AkunDevicesScreen
import id.diskola.app.ui.screens.akun.AkunKontakScreen
import id.diskola.app.ui.screens.akun.AkunPassScreen
import id.diskola.app.ui.screens.akun.AkunScreen
import id.diskola.app.ui.screens.akun.AkunSettingScreen
import id.diskola.app.ui.screens.akun.readAkunSession
import id.diskola.app.ui.screens.home.HomeScreen
import id.diskola.app.ui.screens.akm.AkmDetailScreen
import id.diskola.app.ui.screens.akm.AkmExplainScreen
import id.diskola.app.ui.screens.akm.AkmQuestionScreen
import id.diskola.app.ui.screens.akm.AkmResumeScreen
import id.diskola.app.ui.screens.akm.AkmScoreScreen
import id.diskola.app.ui.screens.akm.AkmScreen
import id.diskola.app.ui.screens.materi.MapelGuruScreen
import id.diskola.app.ui.screens.materi.MapelScreen
import id.diskola.app.ui.screens.materi.MateriDetailScreen
import id.diskola.app.ui.screens.materi.MateriGuruScreen
import id.diskola.app.ui.screens.materi.MateriListScreen
import id.diskola.app.ui.screens.materi.PdfViewerScreen
import id.diskola.app.ui.screens.materi.UploadMateriScreen
import id.diskola.app.ui.screens.materi.rememberIsTeacher
import id.diskola.app.ui.screens.poin.PoinCariScreen
import id.diskola.app.ui.screens.poin.PoinFormScreen
import id.diskola.app.ui.screens.poin.PoinHasilScreen
import id.diskola.app.ui.screens.poin.PoinSiswaScreen
import id.diskola.app.viewmodel.PoinGuruViewModel
import id.diskola.app.ui.screens.notifikasi.NotifikasiDetailScreen
import id.diskola.app.ui.screens.notifikasi.NotifikasiScreen
import id.diskola.app.ui.screens.tugas.TugasDetailScreen
import id.diskola.app.ui.screens.tugas.TugasGuruScreen
import id.diskola.app.ui.screens.tugas.TugasScoringScreen
import id.diskola.app.ui.screens.tugas.TugasSiswaScreen
import id.diskola.app.ui.screens.tugas.TugasTerkumpulScreen
import id.diskola.app.ui.screens.tugas.UploadTugasScreen
import id.diskola.app.ui.screens.pembayaran.CheckoutScreen
import id.diskola.app.ui.screens.pembayaran.PaymentSuccessScreen
import id.diskola.app.ui.screens.pembayaran.PembayaranScreen
import id.diskola.app.ui.screens.pembayaran.PinScreen
import id.diskola.app.ui.screens.pembayaran.QrScreen
import id.diskola.app.ui.screens.pembayaran.RiwayatScreen
import id.diskola.app.ui.screens.pembayaran.SppScreen
import id.diskola.app.ui.screens.pembayaran.TopUpScreen
import id.diskola.app.ui.screens.pembayaran.TopUpVaScreen
import id.diskola.app.ui.screens.pembayaran.TransferScreen
import id.diskola.app.ui.theme.LocalWindowWidth
import id.diskola.app.ui.theme.WindowWidth
import id.diskola.app.ui.theme.contentContainer
import id.diskola.app.viewmodel.AkmViewModel
import id.diskola.app.viewmodel.AkunViewModel
import id.diskola.app.viewmodel.MateriGuruViewModel
import id.diskola.app.viewmodel.NotifikasiViewModel
import id.diskola.app.viewmodel.TugasGuruViewModel
import id.diskola.app.viewmodel.TugasTerkumpulViewModel
import id.diskola.app.viewmodel.PembayaranViewModel

private data class TabItem(
    val route: Route.MainTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isSelected: (androidx.navigation.NavDestination) -> Boolean,
)

private val tabs = listOf(
    TabItem(Route.MainTab.Pembelajaran, "Pembelajaran", Icons.Rounded.School) { it.hasRoute<Route.MainTab.Pembelajaran>() },
    TabItem(Route.MainTab.Pembayaran, "Pembayaran", Icons.Rounded.AccountBalanceWallet) { it.hasRoute<Route.MainTab.Pembayaran>() },
    TabItem(Route.MainTab.Akun, "Akun", Icons.Rounded.AccountCircle) { it.hasRoute<Route.MainTab.Akun>() },
)

/**
 * Bottom-nav shell for Pembelajaran/Pembayaran/Akun (Fase 2 redesign — replaces the old
 * Dashboard/Materi/Absensi/Profile 4-tab shell). Materi, Absensi, Notifikasi, and every
 * Akun/Pembayaran subscreen are pushed routes reachable from their tab, not tabs themselves;
 * the bottom bar hides whenever one of those is on top of the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(onLoggedOut: () -> Unit, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showNav = tabs.any { tab -> currentRoute?.hierarchy?.any(tab.isSelected) == true }
    // Tablets/landscape put the same three destinations in a side rail instead of a bottom bar,
    // which is the M3 convention and avoids a 3-item bar stranded across ~1000dp of empty width.
    val useRail = LocalWindowWidth.current != WindowWidth.Compact

    fun selectTab(tab: TabItem) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showNav && !useRail) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.hierarchy?.any(tab.isSelected) == true,
                            onClick = { selectTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showNav && useRail) {
                NavigationRail {
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentRoute?.hierarchy?.any(tab.isSelected) == true,
                            onClick = { selectTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = Route.MainTab.Pembelajaran,
                modifier = Modifier.weight(1f).contentContainer(),
            ) {
                composable<Route.MainTab.Pembelajaran> {
                    val isTeacher = rememberIsTeacher()
                    HomeScreen(
                        onNavigateToMateri = {
                            navController.navigate(if (isTeacher) Route.MateriGuru else Route.Mapel)
                        },
                        onNavigateToTugas = {
                            navController.navigate(if (isTeacher) Route.TugasGuru else Route.TugasSiswa)
                        },
                        onNavigateToPresensi = { navController.navigate(Route.Presensi) },
                        onOpenJurnal = { action, attendanceId, plotId -> navController.navigate(Route.Jurnal(action, attendanceId, plotId)) },
                        onNavigateToAgenda = { navController.navigate(Route.AgendaMingguan) },
                        onNavigateToPoin = {
                            navController.navigate(if (isTeacher) Route.PoinGuru else Route.PoinSiswa)
                        },
                        onNavigateToAsesmen = { navController.navigate(Route.Akm.List) },
                        onNavigateToNotifikasi = { navController.navigate(Route.Notifikasi) },
                        onLoggedOut = onLoggedOut,
                    )
                }
                composable<Route.MainTab.Akun> {
                    AkunScreen(
                        onLoggedOut = onLoggedOut,
                        onNavigate = { sub -> navController.navigate(sub) },
                        onOpenAkm = { navController.navigate(Route.Akm.List) },
                    )
                }
                composable<Route.MainTab.Pembayaran> {
                    val viewModel: PembayaranViewModel = hiltViewModel()
                    PembayaranScreen(
                        viewModel = viewModel,
                        onTopUp = { navController.navigate(Route.PembayaranSub.TopUp) },
                        onTransfer = { navController.navigate(Route.PembayaranSub.Transfer) },
                        onQr = { navController.navigate(Route.PembayaranSub.Qr) },
                        onRiwayat = { navController.navigate(Route.PembayaranSub.Riwayat) },
                        onSpp = { navController.navigate(Route.PembayaranSub.Spp) },
                    )
                }
    
                composable<Route.Mapel> {
                    MapelScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSubject = { navController.navigate(Route.MateriList(it.id.toInt(), it.name, isTeacher = false)) },
                    )
                }
                composable<Route.MateriGuru> { backStackEntry ->
                    val vm: MateriGuruViewModel = hiltViewModel(backStackEntry)
                    val materiChanged by backStackEntry.savedStateHandle.getStateFlow("materiChanged", false)
                        .collectAsStateWithLifecycle()
                    LaunchedEffect(materiChanged) {
                        if (materiChanged) {
                            vm.onMateriChanged()
                            backStackEntry.savedStateHandle["materiChanged"] = false
                        }
                    }
                    MateriGuruScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSubjects = { navController.navigate(Route.MapelGuru) },
                        onOpenMateri = { navController.navigate(Route.MateriDetail(it.id.toInt(), it.subject_id, isTeacher = true)) },
                        onUpload = { navController.navigate(Route.UploadMateri()) },
                        onEdit = { navController.navigate(Route.UploadMateri(isEdit = true, materiId = it.id.toInt(), subjectId = it.subject_id)) },
                        viewModel = vm,
                    )
                }
                composable<Route.MapelGuru> {
                    MapelGuruScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSubject = { navController.navigate(Route.MateriList(it.id.toInt(), it.name, isTeacher = true)) },
                    )
                }
                composable<Route.MateriList> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.MateriList>()
                    MateriListScreen(
                        subjectId = route.subjectId,
                        subjectName = route.subjectName,
                        isTeacher = route.isTeacher,
                        onBack = { navController.popBackStack() },
                        onOpenMateri = { navController.navigate(Route.MateriDetail(it.id.toInt(), it.subject_id, route.isTeacher)) },
                    )
                }
                composable<Route.Presensi> {
                    PresensiScreen(
                        onBack = { navController.popBackStack() },
                        onOpenMasuk = { navController.navigate(Route.PresensiMasuk) },
                        onOpenOffsite = { navController.navigate(Route.PresensiOffsite) },
                        onOpenIzin = { navController.navigate(Route.IzinList()) },
                    )
                }
                composable<Route.PresensiMasuk> {
                    PresensiMasukScreen(
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }
                composable<Route.PresensiOffsite> { backStackEntry ->
                    val photoPath by backStackEntry.savedStateHandle.getStateFlow<String?>("offsitePhoto", null).collectAsStateWithLifecycle()
                    PresensiOffsiteScreen(
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                        onTakePhoto = { address, lat, lng -> navController.navigate(Route.PresensiOffsiteCamera(address, lat.toString(), lng.toString())) },
                        capturedPhotoPath = photoPath,
                        onPhotoHandled = { backStackEntry.savedStateHandle["offsitePhoto"] = null },
                    )
                }
                composable<Route.PresensiOffsiteCamera> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.PresensiOffsiteCamera>()
                    CaptureCameraScreen(
                        address = route.address,
                        lat = route.lat.toDoubleOrNull(),
                        lng = route.lng.toDoubleOrNull(),
                        config = CaptureCameraConfig.OffsiteSelfie,
                        onClose = { navController.popBackStack() },
                        onCaptured = { path ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("offsitePhoto", path)
                            navController.popBackStack()
                        },
                    )
                }
                composable<Route.Jurnal> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Jurnal>()
                    JurnalScreen(
                        initialAction = route.action,
                        initialAttendanceId = route.attendanceId,
                        initialPlotId = route.plotId,
                        onBack = { navController.popBackStack() },
                        onOpenVerifikasi = { id, title -> navController.navigate(Route.VerifikasiJurnal(id, title)) },
                        onOpenForm = { plotId, label -> navController.navigate(Route.JurnalForm(plotId, label)) },
                        onOpenDetail = { id, createdAt, plot -> navController.navigate(Route.JurnalDetail(id, createdAt, plot)) },
                    )
                }
                composable<Route.VerifikasiJurnal> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.VerifikasiJurnal>()
                    VerifikasiJurnalScreen(
                        attendanceId = route.attendanceId,
                        title = route.title,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }
                composable<Route.JurnalForm> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.JurnalForm>()
                    val photoPath by backStackEntry.savedStateHandle.getStateFlow<String?>("jurnalPhoto", null).collectAsStateWithLifecycle()
                    JurnalFormScreen(
                        plotId = route.plotId,
                        plotLabel = route.plotLabel,
                        capturedPhotoPath = photoPath,
                        onPhotoHandled = { backStackEntry.savedStateHandle["jurnalPhoto"] = null },
                        onBack = { navController.popBackStack() },
                        onTakePhoto = { address, lat, lng -> navController.navigate(Route.JurnalCapture(address, lat?.toString().orEmpty(), lng?.toString().orEmpty())) },
                        onFinished = { openId ->
                            navController.popBackStack()
                            // 307 "journal exists": land on that journal, dated today like legacy
                            if (openId != null) navController.navigate(Route.JurnalDetail(openId, LocalDate.now().toString(), ""))
                        },
                    )
                }
                composable<Route.JurnalCapture> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.JurnalCapture>()
                    CaptureCameraScreen(
                        address = route.address.ifBlank { null },
                        lat = route.lat.toDoubleOrNull(),
                        lng = route.lng.toDoubleOrNull(),
                        config = CaptureCameraConfig.JurnalScene,
                        onClose = { navController.popBackStack() },
                        onCaptured = { path ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("jurnalPhoto", path)
                            navController.popBackStack()
                        },
                    )
                }
                composable<Route.JurnalDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.JurnalDetail>()
                    JurnalDetailScreen(
                        attendanceId = route.attendanceId,
                        createdAt = route.createdAt,
                        plot = route.plot,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable<Route.IzinList> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.IzinList>()
                    val changed by backStackEntry.savedStateHandle.getStateFlow("izinChanged", false).collectAsStateWithLifecycle()
                    IzinListScreen(
                        initialFilter = route.filter,
                        onBack = { navController.popBackStack() },
                        onAdd = { navController.navigate(Route.IzinAdd) },
                        onOpenDetail = { navController.navigate(Route.IzinDetail(it)) },
                        changed = changed,
                        onChangedHandled = { backStackEntry.savedStateHandle["izinChanged"] = false },
                    )
                }
                composable<Route.IzinAdd> {
                    IzinAddScreen(
                        onBack = { navController.popBackStack() },
                        onDone = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("izinChanged", true)
                            navController.popBackStack()
                        },
                    )
                }
                composable<Route.IzinDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.IzinDetail>()
                    IzinDetailScreen(uuid = route.uuid, onBack = { navController.popBackStack() })
                }
                composable<Route.MateriDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.MateriDetail>()
                    MateriDetailScreen(
                        materiId = route.materiId,
                        subjectId = route.subjectId,
                        isTeacher = route.isTeacher,
                        onBack = { navController.popBackStack() },
                        onOpenPdf = { filePath, title -> navController.navigate(Route.PdfViewer(filePath, title)) },
                    )
                }
                composable<Route.UploadMateri> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.UploadMateri>()
                    UploadMateriScreen(
                        route = route,
                        onDone = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("materiChanged", true)
                            navController.popBackStack()
                        },
                    )
                }
                composable<Route.PdfViewer> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.PdfViewer>()
                    PdfViewerScreen(filePath = route.filePath, title = route.title, onBack = { navController.popBackStack() })
                }

                composable<Route.AgendaMingguan> {
                    AgendaMingguanScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { date, id -> navController.navigate(Route.AgendaDetail(date, id)) },
                        onOpenCheck = { date, id, mode -> navController.navigate(Route.AgendaCheck(date, id, mode)) },
                        onOpenPresensi = { navController.navigate(Route.Presensi) },
                    )
                }
                composable<Route.AgendaCheck> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.AgendaCheck>()
                    AgendaCheckScreen(
                        date = route.date,
                        agendaId = route.agendaId,
                        mode = route.mode,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                        // Presensi replaces this check page; coming back lands on the agenda list, which
                        // refreshes today's gate on resume.
                        onOpenPresensi = {
                            navController.popBackStack()
                            navController.navigate(Route.Presensi)
                        },
                    )
                }
                composable<Route.AgendaDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.AgendaDetail>()
                    AgendaDetailScreen(
                        date = route.date,
                        agendaId = route.agendaId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.PoinSiswa> {
                    PoinSiswaScreen(onBack = { navController.popBackStack() })
                }
                composable<Route.PoinGuru> { backStackEntry ->
                    val vm: PoinGuruViewModel = hiltViewModel(backStackEntry)
                    PoinCariScreen(
                        onBack = { navController.popBackStack() },
                        onStudentReady = { navController.navigate(Route.PoinHasil) },
                        viewModel = vm,
                    )
                }
                composable<Route.PoinHasil> { backStackEntry ->
                    val guruEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.PoinGuru) }
                    val vm: PoinGuruViewModel = hiltViewModel(guruEntry)
                    PoinHasilScreen(
                        onBack = { navController.popBackStack() },
                        onOpenForm = { navController.navigate(Route.PoinForm(it)) },
                        guruViewModel = vm,
                    )
                }
                composable<Route.PoinForm> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.PoinForm>()
                    val guruEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.PoinGuru) }
                    val vm: PoinGuruViewModel = hiltViewModel(guruEntry)
                    PoinFormScreen(
                        mode = route.mode,
                        guruViewModel = vm,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }

                composable<Route.TugasSiswa> {
                    TugasSiswaScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTugas = { navController.navigate(Route.TugasDetail(it.id.toInt(), it.type, isTeacher = false)) },
                        onOpenPdf = { filePath, title -> navController.navigate(Route.PdfViewer(filePath, title)) },
                    )
                }
                composable<Route.TugasDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.TugasDetail>()
                    TugasDetailScreen(
                        tugasId = route.tugasId, type = route.type, isTeacher = route.isTeacher,
                        onBack = { navController.popBackStack() },
                        onOpenPdf = { filePath, title -> navController.navigate(Route.PdfViewer(filePath, title)) },
                    )
                }
                composable<Route.TugasGuru> { backStackEntry ->
                    val vm: TugasGuruViewModel = hiltViewModel(backStackEntry)
                    val tugasChanged by backStackEntry.savedStateHandle.getStateFlow("tugasChanged", false)
                        .collectAsStateWithLifecycle()
                    LaunchedEffect(tugasChanged) {
                        if (tugasChanged) {
                            vm.onTugasChanged()
                            backStackEntry.savedStateHandle["tugasChanged"] = false
                        }
                    }
                    TugasGuruScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTugas = { navController.navigate(Route.TugasDetail(it.id.toInt(), it.type, isTeacher = true)) },
                        onUpload = { navController.navigate(Route.UploadTugas()) },
                        onEdit = { navController.navigate(Route.UploadTugas(isEdit = true, tugasId = it.id.toInt())) },
                        onOpenScoredGroup = { navController.navigate(Route.TugasTerkumpul(it.id.toInt())) },
                        viewModel = vm,
                    )
                }
                composable<Route.UploadTugas> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.UploadTugas>()
                    UploadTugasScreen(
                        route = route,
                        onDone = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("tugasChanged", true)
                            navController.popBackStack()
                        },
                    )
                }
                composable<Route.TugasTerkumpul> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.TugasTerkumpul>()
                    val vm: TugasTerkumpulViewModel = hiltViewModel(backStackEntry)
                    val scoreSaved by backStackEntry.savedStateHandle.getStateFlow("scoreSaved", false)
                        .collectAsStateWithLifecycle()
                    LaunchedEffect(scoreSaved) {
                        if (scoreSaved) {
                            vm.refresh(route.collectedId)
                            backStackEntry.savedStateHandle["scoreSaved"] = false
                        }
                    }
                    TugasTerkumpulScreen(
                        collectedId = route.collectedId,
                        onBack = { navController.popBackStack() },
                        onOpenTugasReadonly = { navController.navigate(Route.TugasDetail(it, isTeacher = true)) },
                        onScoreStudent = { assignment -> navController.navigate(Route.TugasScoring(route.collectedId, assignment.id)) },
                        viewModel = vm,
                    )
                }
                composable<Route.TugasScoring> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.TugasScoring>()
                    val terkumpulEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.TugasTerkumpul(route.collectedId)) }
                    val terkumpulVm: TugasTerkumpulViewModel = hiltViewModel(terkumpulEntry)
                    TugasScoringScreen(
                        collectedId = route.collectedId,
                        assignmentId = route.assignmentId,
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("scoreSaved", true)
                            navController.popBackStack()
                        },
                        terkumpulViewModel = terkumpulVm,
                    )
                }
    
                composable<Route.Akm.List> { backStackEntry ->
                    // This destination IS the shared-ViewModel anchor — scope to its own entry
                    // directly instead of looking it up again (see note below on why re-lookup crashes).
                    val vm: AkmViewModel = hiltViewModel(backStackEntry)
                    AkmScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { navController.navigate(Route.Akm.Detail(it)) },
                        onOpenScore = { navController.navigate(Route.Akm.Score(it)) },
                        showTimeGate = true,
                        viewModel = vm,
                    )
                }
                composable<Route.Akm.Detail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Akm.Detail>()
                    // `remember`ed so this is only resolved once per visit to this destination —
                    // resolving it fresh on every recomposition can crash mid pop-transition, once
                    // the user has already backed out past Route.Akm.List and it leaves the back stack.
                    val listEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Akm.List) }
                    val vm: AkmViewModel = hiltViewModel(listEntry)
                    AkmDetailScreen(
                        akmId = route.id,
                        onBack = { navController.popBackStack() },
                        onStart = { navController.navigate(Route.Akm.Resume(route.id)) },
                        onViewScore = { navController.navigate(Route.Akm.Score(route.id)) },
                        viewModel = vm,
                    )
                }
                composable<Route.Akm.Resume> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Akm.Resume>()
                    val listEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Akm.List) }
                    val vm: AkmViewModel = hiltViewModel(listEntry)
                    AkmResumeScreen(
                        akmId = route.id,
                        onOpenInstruction = { navController.navigate(Route.Akm.Question(route.id)) },
                        // Pop back to the existing list rather than navigating to a fresh one:
                        // recreating it would drop the shared AkmViewModel and re-fire the clock gate.
                        onSubmitted = { navController.popBackStack(Route.Akm.List, inclusive = false) },
                        viewModel = vm,
                    )
                }
                composable<Route.Akm.Question> { backStackEntry ->
                    val listEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Akm.List) }
                    val vm: AkmViewModel = hiltViewModel(listEntry)
                    AkmQuestionScreen(
                        onBack = { navController.popBackStack() },
                        onTimeUp = { navController.popBackStack(Route.Akm.List, inclusive = false) },
                        viewModel = vm,
                    )
                }
                composable<Route.Akm.Score> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Akm.Score>()
                    val listEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Akm.List) }
                    val vm: AkmViewModel = hiltViewModel(listEntry)
                    AkmScoreScreen(
                        scoreId = route.id,
                        onBack = { navController.popBackStack() },
                        onOpenExplanation = { navController.navigate(Route.Akm.Explain(route.id)) },
                        viewModel = vm,
                    )
                }
                composable<Route.Akm.Explain> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Akm.Explain>()
                    val listEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Akm.List) }
                    val vm: AkmViewModel = hiltViewModel(listEntry)
                    AkmExplainScreen(
                        scoreId = route.id,
                        onBack = { navController.popBackStack() },
                        viewModel = vm,
                    )
                }

                composable<Route.Notifikasi> {
                    val viewModel: NotifikasiViewModel = hiltViewModel(navController.getBackStackEntry(Route.Notifikasi))
                    NotifikasiScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { item ->
                            viewModel.markRead(item.id)
                            navController.navigate(Route.NotifikasiDetail(item.id))
                        },
                    )
                }
                composable<Route.NotifikasiDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.NotifikasiDetail>()
                    val viewModel: NotifikasiViewModel = hiltViewModel(navController.getBackStackEntry(Route.Notifikasi))
                    NotifikasiDetailScreen(item = viewModel.findById(route.id), onBack = { navController.popBackStack() })
                }
    
                // Akun subscreens — session info is re-read per screen (cheap, SharedPreferences-backed).
                composable<Route.AkunSub.Setting> {
                    val vm: AkunViewModel = hiltViewModel()
                    AkunSettingScreen(session = readAkunSession(LocalContext.current, vm), onBack = { navController.popBackStack() })
                }
                composable<Route.AkunSub.Kontak> {
                    val vm: AkunViewModel = hiltViewModel()
                    AkunKontakScreen(session = readAkunSession(LocalContext.current, vm), onBack = { navController.popBackStack() })
                }
                composable<Route.AkunSub.Devices> {
                    AkunDevicesScreen(onBack = { navController.popBackStack() })
                }
                composable<Route.AkunSub.Pass> {
                    AkunPassScreen(onBack = { navController.popBackStack() })
                }
                composable<Route.AkunSub.Card> {
                    val vm: AkunViewModel = hiltViewModel()
                    AkunCardScreen(session = readAkunSession(LocalContext.current, vm), onBack = { navController.popBackStack() })
                }
                composable<Route.AkunSub.About> {
                    AkunAboutScreen(onBack = { navController.popBackStack() })
                }
    
                // Pembayaran subscreens — share ONE PembayaranViewModel (scoped to the Pembayaran
                // tab's own back-stack entry) so selections made on one screen (e.g. bills picked on
                // Spp) are still there on the next (e.g. Checkout).
                composable<Route.PembayaranSub.TopUp> {
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    TopUpScreen(viewModel = vm, onBack = { navController.popBackStack() }, onContinue = { navController.navigate(Route.PembayaranSub.TopUpVa) })
                }
                composable<Route.PembayaranSub.TopUpVa> {
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    TopUpVaScreen(
                        onBack = { navController.popBackStack() },
                        onDone = {
                            vm.completeTopUp(vm.topUpAmount.value)
                            navController.navigate(Route.PembayaranSub.Riwayat) { popUpTo(Route.MainTab.Pembayaran) }
                        },
                    )
                }
                composable<Route.PembayaranSub.Transfer> {
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    TransferScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onContinue = {
                            navController.navigate(
                                Route.PembayaranSub.Pin(amountLabel = formatRupiah(vm.transferAmount.value), purpose = "transfer")
                            )
                        },
                    )
                }
                composable<Route.PembayaranSub.Qr> {
                    QrScreen(onBack = { navController.popBackStack() })
                }
                composable<Route.PembayaranSub.Riwayat> {
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    RiwayatScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }
                composable<Route.PembayaranSub.Spp> {
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    SppScreen(viewModel = vm, onBack = { navController.popBackStack() }, onBayar = { navController.navigate(Route.PembayaranSub.Checkout()) })
                }
                composable<Route.PembayaranSub.Checkout> {
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    CheckoutScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onPay = { amountLabel -> navController.navigate(Route.PembayaranSub.Pin(amountLabel = amountLabel, purpose = "checkout")) },
                    )
                }
                composable<Route.PembayaranSub.Pin> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.PembayaranSub.Pin>()
                    val vm: PembayaranViewModel = hiltViewModel(navController.getBackStackEntry(Route.MainTab.Pembayaran))
                    PinScreen(
                        amountLabel = route.amountLabel,
                        onBack = { navController.popBackStack() },
                        onConfirmed = {
                            if (route.purpose == "checkout") vm.completePayment(vm.selectedBills().sumOf { it.amount })
                            navController.navigate(Route.PembayaranSub.Success(route.amountLabel)) {
                                popUpTo(Route.MainTab.Pembayaran)
                            }
                        },
                    )
                }
                composable<Route.PembayaranSub.Success> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.PembayaranSub.Success>()
                    PaymentSuccessScreen(
                        amountLabel = route.amountLabel,
                        onDownload = {},
                        onBackToPembayaran = {
                            navController.navigate(Route.MainTab.Pembayaran) { popUpTo(Route.MainTab.Pembayaran) { inclusive = true } }
                        },
                    )
                }
            }
        }
    }
}

