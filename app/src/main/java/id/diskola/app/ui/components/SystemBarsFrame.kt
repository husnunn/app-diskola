package id.diskola.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Keeps a screen out of the status bar, the navigation bar, display cutouts and the keyboard.
 *
 * The app is edge-to-edge (`enableEdgeToEdge()`), so nothing pads for the system bars on its own. Screens inside `MainTabsScreen`
 * are padded by its `Scaffold` and need nothing; every destination hosted directly in `AppNavHost` (splash, onboarding, login,
 * password, SSO, Klaspay activation, reset password) must go through this frame. The strips above and below show the page background,
 * like on every other screen. `safeDrawingPadding()` also consumes the insets it applies, so an inner `TopAppBar` or `Scaffold`
 * does not pad for them a second time.
 */
@Composable
fun SystemBarsFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        content()
    }
}
