package id.app.education.ui.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.app.education.dataclass.mock.NotificationItem
import id.app.education.dataclass.mock.formatRupiah
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.ui.screens.absensi.AbsensiScreen
import id.app.education.ui.screens.akun.AkunAboutScreen
import id.app.education.ui.screens.akun.AkunCardScreen
import id.app.education.ui.screens.akun.AkunDevicesScreen
import id.app.education.ui.screens.akun.AkunKontakScreen
import id.app.education.ui.screens.akun.AkunPassScreen
import id.app.education.ui.screens.akun.AkunScreen
import id.app.education.ui.screens.akun.AkunSettingScreen
import id.app.education.ui.screens.akun.readAkunSession
import id.app.education.ui.screens.home.HomeScreen
import id.app.education.ui.screens.materi.MateriDetailScreen
import id.app.education.ui.screens.materi.MateriScreen
import id.app.education.ui.screens.materi.UploadMateriScreen
import id.app.education.ui.screens.notifikasi.NotifikasiDetailScreen
import id.app.education.ui.screens.notifikasi.NotifikasiScreen
import id.app.education.ui.screens.pembayaran.CheckoutScreen
import id.app.education.ui.screens.pembayaran.PaymentSuccessScreen
import id.app.education.ui.screens.pembayaran.PembayaranScreen
import id.app.education.ui.screens.pembayaran.PinScreen
import id.app.education.ui.screens.pembayaran.QrScreen
import id.app.education.ui.screens.pembayaran.RiwayatScreen
import id.app.education.ui.screens.pembayaran.SppScreen
import id.app.education.ui.screens.pembayaran.TopUpScreen
import id.app.education.ui.screens.pembayaran.TopUpVaScreen
import id.app.education.ui.screens.pembayaran.TransferScreen
import id.app.education.ui.theme.LocalWindowWidth
import id.app.education.ui.theme.WindowWidth
import id.app.education.ui.theme.contentContainer
import id.app.education.viewmodel.AkunViewModel
import id.app.education.viewmodel.NotifikasiViewModel
import id.app.education.viewmodel.PembayaranViewModel

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
                    HomeScreen(
                        onNavigateToMateri = { navController.navigate(Route.Materi) },
                        onNavigateToAbsensi = { navController.navigate(Route.Absensi) },
                        onNavigateToNotifikasi = { navController.navigate(Route.Notifikasi) },
                    )
                }
                composable<Route.MainTab.Akun> {
                    AkunScreen(
                        onLoggedOut = onLoggedOut,
                        onNavigate = { sub -> navController.navigate(sub) },
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
    
                composable<Route.Materi> {
                    MateriScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { item -> navController.navigate(item.toDetailRoute()) },
                        onAddMateri = { navController.navigate(Route.UploadMateri()) },
                        onEditMateri = { item -> navController.navigate(item.toEditRoute()) },
                    )
                }
                composable<Route.Absensi> {
                    AbsensiScreen(onBack = { navController.popBackStack() })
                }
                composable<Route.MateriDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.MateriDetail>()
                    MateriDetailScreen(route = route, onBack = { navController.popBackStack() })
                }
                composable<Route.UploadMateri> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.UploadMateri>()
                    UploadMateriScreen(route = route, onDone = { navController.popBackStack() })
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

private fun MateriItem.toDetailRoute() = Route.MateriDetail(
    title = name,
    description = description,
    teacher = teacher?.name.orEmpty(),
    date = created_at_label,
    subjectName = subject?.name.orEmpty(),
    subjectIcon = subject?.icon_image.orEmpty(),
    filePath = file_path.orEmpty(),
    fileName = file_name.orEmpty(),
    fileType = file_type.orEmpty(),
    fileSize = file_size.orEmpty(),
    links = uri?.link ?: emptyList(),
)

private fun MateriItem.toEditRoute() = Route.UploadMateri(
    isEdit = true,
    materiId = id,
    materiName = name,
    materiDesc = description,
    subjectId = subject?.id ?: -1,
    link = uri?.link?.firstOrNull().orEmpty(),
)
