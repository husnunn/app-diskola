package id.diskola.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Diskola design-system theme. Wraps [MaterialTheme] with the M3 color roles from the
 * design handoff, plus [ExtendedColors]/[ExtendedTypography] tokens M3 has no slot for.
 */
@Composable
fun DiskolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    windowWidth: WindowWidth = WindowWidth.Compact,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DiskolaDarkColorScheme else DiskolaLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalExtendedTypography provides LightExtendedTypography,
        LocalWindowWidth provides windowWidth,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DiskolaTypography,
            shapes = DiskolaShapes,
            content = content,
        )
    }
}

/** `MaterialTheme.extendedColors` — success/warning/ambient-glow/brand-gradient tokens. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

/** `MaterialTheme.extendedTypography` — the "overline" role outside M3's 15 slots. */
val MaterialTheme.extendedTypography: ExtendedTypography
    @Composable
    get() = LocalExtendedTypography.current
