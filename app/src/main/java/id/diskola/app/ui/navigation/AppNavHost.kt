package id.diskola.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.SystemBarsFrame
import id.diskola.app.ui.screens.auth.GoogleAccountInfo
import id.diskola.app.ui.screens.auth.KlaspayActivationScreen
import id.diskola.app.ui.screens.auth.LoginScreen
import id.diskola.app.ui.screens.auth.OnboardingScreen
import id.diskola.app.ui.screens.auth.PasswordScreen
import id.diskola.app.ui.screens.auth.ResetPasswordScreen
import id.diskola.app.ui.screens.auth.ResetPasswordSentScreen
import id.diskola.app.ui.screens.auth.SplashScreen
import id.diskola.app.ui.screens.auth.SsoScreen
import id.diskola.app.ui.screens.auth.TermsDialog
import id.diskola.app.ui.screens.pembayaran.SetPinScreen
import id.diskola.app.ui.screens.auth.signInWithGoogle
import id.diskola.app.utils.session.SessionEvent
import id.diskola.app.utils.session.SessionEvents
import id.diskola.app.utils.session.SessionManager
import id.diskola.app.viewmodel.LoginViewModel
import id.diskola.app.viewmodel.SplashDestination
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(sessionEvents: SessionEvents, sessionManager: SessionManager) {
    val navController = rememberNavController()
    val activity = LocalContext.current as? Activity
    var sessionEndedDialog by remember { mutableStateOf(false) }
    var logoutHeldDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Doc `02-auth-login-sesi.md` §9, decision Q6: a single collector reacting to every 401,
    // instead of the legacy per-screen handling — see `ResponseInterceptor`/`SessionEvents`.
    LaunchedEffect(Unit) {
        sessionEvents.events.collect { event ->
            when (event) {
                SessionEvent.Unauthorized -> sessionEndedDialog = true
                // A logout asked for by the server (FCM "logout") or forced by a 401 while exam answers are still unsent: it is held
                // back and carried out as soon as they are sent (`SessionManager.runDeferredLogoutIfNoPendingExam`).
                SessionEvent.LogoutBlockedByPendingExam -> logoutHeldDialog = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash,
        // an opaque base so a crossfade never shows the previous screen through a screen without its own background
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        framedComposable<Route.Splash> {
            SplashScreen(
                onNavigate = { destination ->
                    val target = when (destination) {
                        SplashDestination.MAIN -> Route.Main
                        SplashDestination.ONBOARDING -> Route.Onboarding
                        SplashDestination.LOGIN, SplashDestination.UPDATE_REQUIRED -> Route.AuthGraph
                    }
                    navController.navigate(target) {
                        popUpTo(Route.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        framedComposable<Route.Onboarding> {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Route.AuthGraph) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                },
            )
        }

        authGraph(navController)

        composable<Route.Main> {
            MainTabsScreen(
                onLoggedOut = {
                    navController.navigate(Route.AuthGraph) {
                        popUpTo(Route.Main) { inclusive = true }
                    }
                },
                onOpenKlaspayActivation = { navController.navigate(Route.Auth.KlaspayActivation(isSso = false, returnToMain = true)) },
                onOpenSetPin = { token -> navController.navigate(Route.SetPin(token)) },
            )
        }

        framedComposable<Route.SetPin> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.SetPin>()
            SetPinScreen(token = route.token, fromLink = false, onDone = { navController.leaveOrFinish(activity) })
        }

        // The mailed reset link opens this directly (even without a session), like legacy's `DeepLinkPage`.
        framedComposable<Route.SetPinLink>(deepLinks = resetPinDeepLinks) { backStackEntry ->
            val route = backStackEntry.toRoute<Route.SetPinLink>()
            SetPinScreen(token = route.token, fromLink = true, onDone = { navController.leaveOrFinish(activity) })
        }
    }

    if (sessionEndedDialog) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Sesi Berakhir",
            body = "Sesi Anda telah berakhir. Silakan login kembali.",
            primaryButtonText = "Masuk Kembali",
            onPrimaryClick = {
                sessionEndedDialog = false
                scope.launch {
                    // Blocked by unsent exam answers: not silently ignored any more — remembered and explained.
                    if (sessionManager.logout() is SessionManager.LogoutResult.BlockedByPendingExam) {
                        sessionManager.deferLogout()
                        logoutHeldDialog = true
                    }
                }
            },
        )
    }

    if (logoutHeldDialog) {
        AppDialog(
            onDismiss = { logoutHeldDialog = false },
            title = "Perhatian",
            body = "Masih terdapat ujian yang belum diselesaikan, silahkan kumpulkan ujian terlebih dahulu agar nilai ujian terproses. " +
                "Keluar dari akun akan dijalankan otomatis setelah jawabanmu terkirim.",
            primaryButtonText = "OK",
            onPrimaryClick = { logoutHeldDialog = false },
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<Route.AuthGraph>(startDestination = Route.Auth.Login) {
        framedComposable<Route.Auth.Login> { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.AuthGraph) }
            val loginViewModel: LoginViewModel = hiltViewModel(parentEntry)
            var termsAccepted by remember { mutableStateOf(false) }
            var termsDialogVisible by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            LoginScreen(
                viewModel = loginViewModel,
                termsAccepted = termsAccepted,
                onTermsAcceptedChange = { termsAccepted = it },
                onOpenTerms = { termsDialogVisible = true },
                onAccountVerified = { navController.navigate(Route.Auth.Password) },
                onGoogleSignIn = {
                    scope.launch {
                        signInWithGoogle(context)?.let {
                            navController.currentBackStackEntry?.savedStateHandle?.set("google_account", it)
                            navController.navigate(Route.Auth.Sso)
                        }
                    }
                },
            )

            if (termsDialogVisible) {
                TermsDialog(
                    onDismiss = { termsDialogVisible = false },
                    onAccept = { termsAccepted = true; termsDialogVisible = false },
                    viewModel = loginViewModel,
                )
            }
        }

        framedComposable<Route.Auth.Password> { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.AuthGraph) }
            val loginViewModel: LoginViewModel = hiltViewModel(parentEntry)
            PasswordScreen(
                viewModel = loginViewModel,
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.AuthGraph) { inclusive = true }
                    }
                },
                onResetPassword = { navController.navigate(Route.Auth.ResetPassword) },
            )
        }

        framedComposable<Route.Auth.Sso> {
            val account = navController.previousBackStackEntry?.savedStateHandle?.get<GoogleAccountInfo>("google_account")
            if (account != null) {
                SsoScreen(
                    account = account,
                    onBack = { navController.popBackStack() },
                    onConfirmed = { registering, klaspayActive ->
                        val destination = if (klaspayActive) Route.Main else Route.Auth.KlaspayActivation(isSso = true)
                        navController.navigate(destination) {
                            // "iya, itu saya" without an active wallet keeps the auth graph on the
                            // back stack (doc §5.3) — only Main or a completed "daftar" pops it.
                            if (klaspayActive || registering) popUpTo(Route.AuthGraph) { inclusive = true }
                        }
                    },
                )
            }
        }

        framedComposable<Route.Auth.KlaspayActivation> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Auth.KlaspayActivation>()
            KlaspayActivationScreen(
                isSso = route.isSso,
                onBack = { navController.popBackStack() },
                onDone = {
                    if (route.returnToMain) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Route.Main) {
                            popUpTo(Route.AuthGraph) { inclusive = true }
                        }
                    }
                },
            )
        }

        framedComposable<Route.Auth.ResetPassword> {
            ResetPasswordScreen(
                onBack = { navController.popBackStack() },
                onSent = { email -> navController.navigate(Route.Auth.ResetPasswordSent(email)) },
            )
        }

        framedComposable<Route.Auth.ResetPasswordSent> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Auth.ResetPasswordSent>()
            ResetPasswordSentScreen(email = route.email, onBack = { navController.popBackStack() })
        }
    }
}

/**
 * A destination hosted directly in [AppNavHost] (outside `MainTabsScreen`'s inset-aware `Scaffold`): its content is kept inside
 * the safe area by [SystemBarsFrame]. `Route.Main` is the one destination that is deliberately not framed.
 */
private inline fun <reified T : Any> NavGraphBuilder.framedComposable(
    deepLinks: List<NavDeepLink> = emptyList(),
    crossinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T>(deepLinks = deepLinks) { entry -> SystemBarsFrame { content(entry) } }
}

/** Prod and dev API hosts of the mailed reset-PIN link (`AndroidManifest.xml` carries the matching intent-filters). */
private val resetPinDeepLinks = listOf(
    navDeepLink<Route.SetPinLink>(basePath = "https://api.diskola.id/api/payment/reset-pin/token"),
    navDeepLink<Route.SetPinLink>(basePath = "https://dev.api.diskola.id/api/payment/reset-pin/token"),
)

/** Back out of a screen; when it was the only one (opened from a link in a fresh task) close the activity instead. */
private fun NavHostController.leaveOrFinish(activity: Activity?) {
    if (!popBackStack()) activity?.finish()
}
