package id.app.education.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.app.education.R

// Bundled Lato weights (res/font/lato_*.ttf, OFL-licensed — see assets/licenses/OFL_Lato.txt)
val LatoFontFamily = FontFamily(
    Font(R.font.lato_light, FontWeight.Light),
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.Black),
)

// M3 Typography — hierarchy driven purely by weight/size per the README's Lato scale.
val DiskolaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 1.15.em,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 1.2.em,
    ),
    titleLarge = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 1.2.em,
    ),
    titleMedium = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 17.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 1.55.em,
    ),
    bodyMedium = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 1.5.em,
    ),
    bodySmall = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 1.6.em,
    ),
    labelLarge = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
    ),
)

/** Non-M3 role: uppercase, tracked "overline" used for section labels/badges. */
@Immutable
data class ExtendedTypography(
    val overline: TextStyle,
)

val LightExtendedTypography = ExtendedTypography(
    overline = TextStyle(
        fontFamily = LatoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
    ),
)

val LocalExtendedTypography = staticCompositionLocalOf { LightExtendedTypography }
