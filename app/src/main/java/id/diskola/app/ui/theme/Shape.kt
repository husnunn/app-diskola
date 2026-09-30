package id.diskola.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// M3 shape scale (README: small 8 · medium 14 · large 24 · extraLarge 28).
val DiskolaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Shapes the 4-slot M3 [Shapes] object doesn't cover — kept as named vals, not forced into it. */
object DiskolaExtraShapes {
    val chipBadge = RoundedCornerShape(8.dp)
    val iconBox = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(14.dp)
    val textField = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(14.dp)
    val dialog = RoundedCornerShape(24.dp)
    val bottomSheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val searchPill = RoundedCornerShape(24.dp)
    val avatar = CircleShape
}
