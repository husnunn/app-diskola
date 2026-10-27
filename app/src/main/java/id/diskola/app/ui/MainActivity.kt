package id.diskola.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import id.diskola.app.exam.ExamSessionController
import id.diskola.app.ui.navigation.AppNavHost
import id.diskola.app.ui.screens.akm.LocalExamSession
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.WindowWidth
import id.diskola.app.utils.session.SessionEvents
import id.diskola.app.utils.session.SessionManager
import javax.inject.Inject

/** The scrim `enableEdgeToEdge()` puts behind a 3-button navigation bar in its light style (white, ~90% opaque); kept so light mode looks as before. */
private val NAV_BAR_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionEvents: SessionEvents

    @Inject
    lateinit var sessionManager: SessionManager

    /** The exam session (lockdown, window flags); this Activity forwards its lifecycle, focus and touches to it. */
    @Inject
    lateinit var examSession: ExamSessionController

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way — Chucker/push notifications just stay silent if denied */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only, so the bars are always light-styled (dark icons). The default style follows the system's dark mode
        // and would draw white icons on the light app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(NAV_BAR_SCRIM, NAV_BAR_SCRIM),
        )
        examSession.attach(this)
        requestNotificationPermissionIfNeeded()
        setContent {
            val windowWidth = when (calculateWindowSizeClass(this).widthSizeClass) {
                WindowWidthSizeClass.Compact -> WindowWidth.Compact
                WindowWidthSizeClass.Medium -> WindowWidth.Medium
                else -> WindowWidth.Expanded
            }
            DiskolaTheme(windowWidth = windowWidth) {
                CompositionLocalProvider(LocalExamSession provides examSession) {
                    AppNavHost(sessionEvents = sessionEvents, sessionManager = sessionManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        examSession.onActivityResumed()
    }

    override fun onPause() {
        examSession.onActivityPaused()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        examSession.onWindowFocusChanged(hasFocus)
    }

    /** While the pin of a strict exam is lost, touches do not reach the exam until the recovery dialog is answered. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (examSession.shouldBlockTouch(ev)) return false
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        examSession.detach(this)
        super.onDestroy()
    }

    /**
     * POST_NOTIFICATIONS is a runtime permission from API 33 onward — without it, both push
     * notifications and Chucker's HTTP-inspector notification are silently dropped, not just
     * hidden. Asked once up front at launch rather than deferred to whichever feature needs it
     * first, so the system prompt shows over the splash screen instead of mid-flow later.
     *
     * Never while an exam session runs: the Activity is recreated by a window resize or a split screen, and a system permission
     * dialog over the exam would be read as the student leaving it.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (examSession.state.value.active) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
