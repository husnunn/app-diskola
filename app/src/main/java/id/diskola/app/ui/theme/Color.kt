package id.diskola.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light scheme
private val md_light_primary = Color(0xFF006A60)
private val md_light_onPrimary = Color(0xFFFFFFFF)
private val md_light_primaryContainer = Color(0xFF005048)
private val md_light_onPrimaryContainer = Color(0xFF95E7DA)
private val md_light_secondary = Color(0xFF007A6E)
private val md_light_onSecondary = Color(0xFFFFFFFF)
private val md_light_secondaryContainer = Color(0xFFCAE5E0)
private val md_light_onSecondaryContainer = Color(0xFF4E6763)
private val md_light_tertiary = Color(0xFF2D4960)
private val md_light_onTertiary = Color(0xFFFFFFFF)
private val md_light_tertiaryContainer = Color(0xFF456179)
private val md_light_onTertiaryContainer = Color(0xFFBEDCF8)
private val md_light_error = Color(0xFFBA1A1A)
private val md_light_onError = Color(0xFFFFFFFF)
private val md_light_errorContainer = Color(0xFFFFDAD6)
private val md_light_onErrorContainer = Color(0xFF93000A)
private val md_light_background = Color(0xFFF4FAF8)
private val md_light_onBackground = Color(0xFF151D1C)
private val md_light_surface = Color(0xFFF4FAF8)
private val md_light_onSurface = Color(0xFF151D1C)
private val md_light_surfaceVariant = Color(0xFFDBE4E2)
private val md_light_onSurfaceVariant = Color(0xFF3E4947)
private val md_light_outline = Color(0xFF6E7977)
private val md_light_outlineVariant = Color(0xFFBEC9C6)
private val md_light_surfaceDim = Color(0xFFD3DCD9)
private val md_light_surfaceBright = Color(0xFFF4FAF8)
private val md_light_surfaceContainerLowest = Color(0xFFFFFFFF)
private val md_light_surfaceContainerLow = Color(0xFFEDF6F3)
private val md_light_surfaceContainer = Color(0xFFE7F0ED)
private val md_light_surfaceContainerHigh = Color(0xFFE1EAE7)
private val md_light_surfaceContainerHighest = Color(0xFFDBE4E2)
private val md_light_inverseSurface = Color(0xFF2A3231)
private val md_light_inverseOnSurface = Color(0xFFEAF3F0)
private val md_light_inversePrimary = Color(0xFF84D5C8)

// Dark scheme
private val md_dark_primary = Color(0xFF92D3C7)
private val md_dark_onPrimary = Color(0xFF003731)
private val md_dark_primaryContainer = Color(0xFF005047)
private val md_dark_onPrimaryContainer = Color(0xFF74C3B5)
private val md_dark_secondary = Color(0xFF43DDBD)
private val md_dark_onSecondary = Color(0xFF00382D)
private val md_dark_secondaryContainer = Color(0xFF00BE9F)
private val md_dark_onSecondaryContainer = Color(0xFF00463A)
private val md_dark_tertiary = Color(0xFF86D5C7)
private val md_dark_onTertiary = Color(0xFF00201C)
private val md_dark_tertiaryContainer = Color(0xFF005047)
private val md_dark_onTertiaryContainer = Color(0xFF74C3B5)
private val md_dark_error = Color(0xFFFFB4AB)
private val md_dark_onError = Color(0xFF690005)
private val md_dark_errorContainer = Color(0xFF93000A)
private val md_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_dark_background = Color(0xFF0A1513)
private val md_dark_onBackground = Color(0xFFD9E5E1)
private val md_dark_surface = Color(0xFF0A1513)
private val md_dark_onSurface = Color(0xFFD9E5E1)
private val md_dark_surfaceVariant = Color(0xFF2C3735)
private val md_dark_onSurfaceVariant = Color(0xFFBFC9C5)
private val md_dark_outline = Color(0xFF899390)
private val md_dark_outlineVariant = Color(0xFF3F4946)
private val md_dark_surfaceDim = Color(0xFF0A1513)
private val md_dark_surfaceBright = Color(0xFF303B39)
private val md_dark_surfaceContainerLowest = Color(0xFF06100E)
private val md_dark_surfaceContainerLow = Color(0xFF131E1C)
private val md_dark_surfaceContainer = Color(0xFF172220)
private val md_dark_surfaceContainerHigh = Color(0xFF212C2A)
private val md_dark_surfaceContainerHighest = Color(0xFF2C3735)
private val md_dark_inverseSurface = Color(0xFFD9E5E1)
private val md_dark_inverseOnSurface = Color(0xFF273330)
private val md_dark_inversePrimary = Color(0xFF26695F)

val DiskolaLightColorScheme: ColorScheme = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary,
    onSecondary = md_light_onSecondary,
    secondaryContainer = md_light_secondaryContainer,
    onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary = md_light_tertiary,
    onTertiary = md_light_onTertiary,
    tertiaryContainer = md_light_tertiaryContainer,
    onTertiaryContainer = md_light_onTertiaryContainer,
    error = md_light_error,
    onError = md_light_onError,
    errorContainer = md_light_errorContainer,
    onErrorContainer = md_light_onErrorContainer,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant,
    onSurfaceVariant = md_light_onSurfaceVariant,
    outline = md_light_outline,
    outlineVariant = md_light_outlineVariant,
    surfaceDim = md_light_surfaceDim,
    surfaceBright = md_light_surfaceBright,
    surfaceContainerLowest = md_light_surfaceContainerLowest,
    surfaceContainerLow = md_light_surfaceContainerLow,
    surfaceContainer = md_light_surfaceContainer,
    surfaceContainerHigh = md_light_surfaceContainerHigh,
    surfaceContainerHighest = md_light_surfaceContainerHighest,
    inverseSurface = md_light_inverseSurface,
    inverseOnSurface = md_light_inverseOnSurface,
    inversePrimary = md_light_inversePrimary,
)

val DiskolaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    error = md_dark_error,
    onError = md_dark_onError,
    errorContainer = md_dark_errorContainer,
    onErrorContainer = md_dark_onErrorContainer,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    outline = md_dark_outline,
    outlineVariant = md_dark_outlineVariant,
    surfaceDim = md_dark_surfaceDim,
    surfaceBright = md_dark_surfaceBright,
    surfaceContainerLowest = md_dark_surfaceContainerLowest,
    surfaceContainerLow = md_dark_surfaceContainerLow,
    surfaceContainer = md_dark_surfaceContainer,
    surfaceContainerHigh = md_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_dark_surfaceContainerHighest,
    inverseSurface = md_dark_inverseSurface,
    inverseOnSurface = md_dark_inverseOnSurface,
    inversePrimary = md_dark_inversePrimary,
)

/**
 * Tokens that Material3's [ColorScheme] has no slot for (README "Extended colors").
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val ambientGlowPrimary: Color,
    val ambientGlowSecondary: Color,
    val ambientGlowTertiary: Color,
    val brandGradientDark: Color,
    val brandGradientMid: Color,
    val brandGradientLight: Color,
    /**
     * Exam-mode header surface. Deliberately its own token rather than `tertiary`: the handoff
     * paints this bar with hardcoded white text, and `tertiary` flips to a light mint in dark
     * mode, where white would be unreadable.
     */
    val examSurface: Color,
    val onExamSurface: Color,
    /** Penalty countdown chip — always reads as an alert, in either theme. */
    val penaltyContainer: Color,
    val onPenaltyContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    success = Color(0xFF008779),
    warning = Color(0xFFFF891C),
    ambientGlowPrimary = Color(0xFF9FF2E4),
    ambientGlowSecondary = Color(0xFFCDE8E3),
    ambientGlowTertiary = Color(0xFFCCE5FF),
    brandGradientDark = Color(0xFF014D48),
    brandGradientMid = Color(0xFF0F7A6E),
    brandGradientLight = Color(0xFF12A78E),
    examSurface = Color(0xFF2D4960),
    onExamSurface = Color(0xFFFFFFFF),
    penaltyContainer = Color(0xFFFFDAD6),
    onPenaltyContainer = Color(0xFF93000A),
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF43DDBD),
    warning = Color(0xFFFFB86A),
    ambientGlowPrimary = Color(0xFF005047),
    ambientGlowSecondary = Color(0xFF00BE9F),
    ambientGlowTertiary = Color(0xFF005047),
    brandGradientDark = Color(0xFF014D48),
    brandGradientMid = Color(0xFF0F7A6E),
    brandGradientLight = Color(0xFF12A78E),
    examSurface = Color(0xFF1E3345),
    onExamSurface = Color(0xFFFFFFFF),
    penaltyContainer = Color(0xFF93000A),
    onPenaltyContainer = Color(0xFFFFDAD6),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
