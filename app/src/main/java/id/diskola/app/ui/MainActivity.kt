package id.diskola.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import id.diskola.app.ui.navigation.AppNavHost
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.WindowWidth
import id.diskola.app.utils.session.SessionEvents
import id.diskola.app.utils.session.SessionManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionEvents: SessionEvents

    @Inject
    lateinit var sessionManager: SessionManager

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way — Chucker/push notifications just stay silent if denied */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val windowWidth = when (calculateWindowSizeClass(this).widthSizeClass) {
                WindowWidthSizeClass.Compact -> WindowWidth.Compact
                WindowWidthSizeClass.Medium -> WindowWidth.Medium
                else -> WindowWidth.Expanded
            }
            DiskolaTheme(windowWidth = windowWidth) {
                AppNavHost(sessionEvents = sessionEvents, sessionManager = sessionManager)
            }
        }
    }

    /**
     * POST_NOTIFICATIONS is a runtime permission from API 33 onward — without it, both push
     * notifications and Chucker's HTTP-inspector notification are silently dropped, not just
     * hidden. Asked once up front at launch rather than deferred to whichever feature needs it
     * first, so the system prompt shows over the splash screen instead of mid-flow later.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
