package id.app.education.ui.screens.auth

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.app.education.R
import id.app.education.dataclass.ResponData.GeneralViewModel
import id.app.education.ui.theme.LatoFontFamily
import id.app.education.ui.theme.Motion
import id.app.education.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class SplashDestination { UPDATE_REQUIRED, LOGIN, MAIN }

@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeneralViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(Motion.SLOW, easing = Motion.EaseIn),
        label = "splashLogoScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(Motion.SLOW, easing = Motion.EaseIn),
        label = "splashLogoAlpha",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinnerRotation",
    )

    LaunchedEffect(Unit) {
        visible = true
        val destination = resolveDestination(context, viewModel)
        onNavigate(destination)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier
                .size(108.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xl))
        Text(
            "DISKOLA",
            style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 7.sp, fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text("Memuat…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = Spacing.md), contentAlignment = Alignment.BottomCenter) {
        Text(
            "DISKOLA MOBILE V${try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "1.0.0" }}",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontFamily = LatoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

private suspend fun resolveDestination(context: Context, viewModel: GeneralViewModel): SplashDestination {
    withTimeoutOrNull(2000) { viewModel.checkVersion() }

    val shared = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val isLoggedIn = shared.getBoolean("is_logged_in", false)
    val token = shared.getString("token", "")

    try {
        val prefToken = viewModel.pref.getString("accessToken")
        if (prefToken.isBlank() && !token.isNullOrBlank()) {
            viewModel.pref.putString("accessToken", token)
        }
    } catch (_: Exception) {
    }

    return if (isLoggedIn && !token.isNullOrBlank()) SplashDestination.MAIN else SplashDestination.LOGIN
}
