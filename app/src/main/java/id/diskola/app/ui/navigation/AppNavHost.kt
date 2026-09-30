package id.diskola.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.diskola.app.ui.components.AppDialog
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
    var sessionEndedDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Doc `02-auth-login-sesi.md` §9, decision Q6: a single collector reacting to every 401,
    // instead of the legacy per-screen handling — see `ResponseInterceptor`/`SessionEvents`.
    LaunchedEffect(Unit) {
        sessionEvents.events.collect { event ->
            when (event) {
                SessionEvent.Unauthorized -> sessionEndedDialog = true
                SessionEvent.LogoutBlockedByPendingExam -> Unit // surfaced locally by AkunScreen's own dialog today
            }
        }
    }

    NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash> {
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

        composable<Route.Onboarding> {
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
            )
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
                scope.launch { sessionManager.logout() }
            },
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<Route.AuthGraph>(startDestination = Route.Auth.Login) {
        composable<Route.Auth.Login> { backStackEntry ->
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

        composable<Route.Auth.Password> { backStackEntry ->
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

        composable<Route.Auth.Sso> {
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

        composable<Route.Auth.KlaspayActivation> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Auth.KlaspayActivation>()
            KlaspayActivationScreen(
                isSso = route.isSso,
                onDone = {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.AuthGraph) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.Auth.ResetPassword> {
            ResetPasswordScreen(
                onBack = { navController.popBackStack() },
                onSent = { email -> navController.navigate(Route.Auth.ResetPasswordSent(email)) },
            )
        }

        composable<Route.Auth.ResetPasswordSent> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Auth.ResetPasswordSent>()
            ResetPasswordSentScreen(email = route.email, onBack = { navController.popBackStack() })
        }
    }
}
