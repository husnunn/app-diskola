package id.app.education.ui.navigation

import androidx.compose.runtime.Composable
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.app.education.ui.components.AmbientGradientBackground
import id.app.education.ui.screens.auth.GoogleAccountInfo
import id.app.education.ui.screens.auth.GuestConfirmDialog
import id.app.education.ui.screens.auth.LoginScreen
import id.app.education.ui.screens.auth.OnboardingScreen
import id.app.education.ui.screens.auth.PasswordScreen
import id.app.education.ui.screens.auth.SplashDestination
import id.app.education.ui.screens.auth.SplashScreen
import id.app.education.ui.screens.auth.SsoScreen
import id.app.education.ui.screens.auth.TermsDialog
import id.app.education.ui.screens.auth.signInWithGoogle
import id.app.education.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash> {
            SplashScreen(
                onNavigate = { destination ->
                    val target = when (destination) {
                        SplashDestination.MAIN -> Route.Main
                        SplashDestination.UPDATE_REQUIRED -> Route.Auth.Login // handled inline via dialog in real update flow
                        SplashDestination.LOGIN -> Route.Auth.Login
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
                    navController.navigate(Route.Auth.Login) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                },
            )
        }

        authGraph(navController)

        composable<Route.Main> {
            MainTabsScreen(
                onLoggedOut = {
                    navController.navigate(Route.Auth.Login) {
                        popUpTo(Route.Main) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<Route.Auth.Login> {
        // Scoping the ViewModel to this back-stack entry keeps Login's form state (NISN, school,
        // terms) alive when the user backs out of Password/SSO, per the design spec.
        val authViewModel: AuthViewModel = hiltViewModel()
        var termsAccepted by remember { mutableStateOf(false) }
        var termsDialogVisible by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        LoginScreen(
            viewModel = authViewModel,
            termsAccepted = termsAccepted,
            onTermsAcceptedChange = { termsAccepted = it },
            onOpenTerms = { termsDialogVisible = true },
            onAccountVerified = { account ->
                navController.navigate(
                    Route.Auth.Password(
                        userId = account.id,
                        userUuid = account.uuid,
                        userName = account.name.orEmpty(),
                        userEmail = account.email.orEmpty(),
                        userNisNik = account.nis_nik.orEmpty(),
                        userAvatarImage = account.user_avatar_image.orEmpty(),
                        schoolUuid = account.school?.uuid.orEmpty(),
                        schoolName = account.school?.name.orEmpty(),
                        isStudent = account.student != null,
                        isTeacher = account.teacher != null,
                    )
                )
            },
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
            )
        }
    }

    composable<Route.Auth.Password> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Auth.Password>()
        PasswordScreen(
            route = route,
            onBack = { navController.popBackStack() },
            onLoginSuccess = {
                navController.navigate(Route.Main) {
                    popUpTo(Route.Splash) { inclusive = true }
                }
            },
            onResetPassword = { navController.navigate(Route.Auth.ResetPassword) },
        )
    }

    composable<Route.Auth.Sso> {
        val account = navController.previousBackStackEntry?.savedStateHandle?.get<GoogleAccountInfo>("google_account")
        var guestDialogVisible by remember { mutableStateOf(false) }
        if (account != null) {
            SsoScreen(
                account = account,
                onBack = { navController.popBackStack() },
                onContinue = {
                    // The `login-sso` backend contract isn't confirmed yet (see SsoScreen's note),
                    // so this only demonstrates the guest-confirmation gate the design calls for.
                    guestDialogVisible = true
                },
            )
        }
        if (guestDialogVisible) {
            GuestConfirmDialog(
                onDismiss = { guestDialogVisible = false },
                onConfirm = {
                    guestDialogVisible = false
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
            )
        }
    }

    composable<Route.Auth.ResetPassword> {
        // Placeholder — no reset-password endpoint contract is available yet either.
        androidx.compose.material3.Text("Reset Password — belum diimplementasikan")
    }
}
