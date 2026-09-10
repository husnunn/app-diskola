package id.app.education.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Window width bucket, mirroring M3's `WindowWidthSizeClass` without leaking the experimental API into UI code. */
enum class WindowWidth { Compact, Medium, Expanded }

val LocalWindowWidth = staticCompositionLocalOf { WindowWidth.Compact }

/**
 * Widest a single content column is allowed to grow. Past this, text lines get too long to
 * scan and label/value rows drift so far apart they stop reading as pairs — so on tablets the
 * column is centred and the extra width becomes margin instead.
 */
val ContentMaxWidth = 720.dp

/** Caps content at [maxWidth] and centres it, leaving the parent free to stay full-bleed. */
fun Modifier.contentContainer(maxWidth: Dp = ContentMaxWidth): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
