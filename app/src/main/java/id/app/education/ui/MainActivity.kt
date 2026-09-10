package id.app.education.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import dagger.hilt.android.AndroidEntryPoint
import id.app.education.ui.navigation.AppNavHost
import id.app.education.ui.theme.DiskolaTheme
import id.app.education.ui.theme.WindowWidth

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowWidth = when (calculateWindowSizeClass(this).widthSizeClass) {
                WindowWidthSizeClass.Compact -> WindowWidth.Compact
                WindowWidthSizeClass.Medium -> WindowWidth.Medium
                else -> WindowWidth.Expanded
            }
            DiskolaTheme(windowWidth = windowWidth) {
                AppNavHost()
            }
        }
    }
}
