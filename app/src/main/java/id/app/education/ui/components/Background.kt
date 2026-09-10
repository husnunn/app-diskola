package id.app.education.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import id.app.education.ui.theme.extendedColors

/**
 * Pola A — Ambient Gradient Wash. Mounted ONCE around the Auth NavHost (`AuthScaffold`), never
 * per-screen, per the README's anti-pattern #6. Approximated with layered radial/linear
 * gradients rather than PNGs, per anti-pattern #3.
 */
@Composable
fun AmbientGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (dark) {
                drawDarkWash(extended)
            } else {
                drawLightWash(scheme.primary, extended)
            }
        }
        content()
    }
}

private fun DrawScope.drawLightWash(primary: Color, extended: id.app.education.ui.theme.ExtendedColors) {
    // 1. Base tonal wash
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFEDF6F3),
            0.5f to Color(0xFFF4FAF8),
            1f to Color(0xFFEDF6F3),
        ),
        size = size,
    )

    // 2. Brand arc — top ~34% of the screen, organic curve via quadratic bezier.
    val arcHeight = size.height * 0.34f
    val arcPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, arcHeight * 0.64f)
        quadraticTo(size.width * 0.5f, arcHeight * 1.3f, 0f, arcHeight * 0.58f)
        close()
    }
    drawPath(
        path = arcPath,
        brush = Brush.verticalGradient(
            0f to primary.copy(alpha = 0.16f),
            1f to primary.copy(alpha = 0.04f),
        ),
    )

    // 3. Ambient glow blobs (radial gradient -> transparent).
    drawGlow(center = Offset(size.width + 64.dp.toPx(), -80.dp.toPx()), radius = 288.dp.toPx(), color = primary.copy(alpha = 0.12f))
    drawGlow(center = Offset(-80.dp.toPx(), 112.dp.toPx()), radius = 240.dp.toPx(), color = extended.ambientGlowPrimary.copy(alpha = 0.25f))
    drawGlow(center = Offset(size.width, size.height + 64.dp.toPx()), radius = 280.dp.toPx(), color = primary.copy(alpha = 0.05f))
}

private fun DrawScope.drawDarkWash(extended: id.app.education.ui.theme.ExtendedColors) {
    drawRect(color = Color(0xFF0A1513), size = size)
    drawGlow(center = Offset(-80.dp.toPx(), -96.dp.toPx()), radius = 320.dp.toPx(), color = Color(0xFF005047).copy(alpha = 0.40f))
    drawGlow(center = Offset(size.width + 96.dp.toPx(), size.height / 2), radius = 280.dp.toPx(), color = Color(0xFF00BE9F).copy(alpha = 0.25f))
    drawGlow(center = Offset(size.width / 2, size.height + 80.dp.toPx()), radius = 330.dp.toPx(), color = Color(0xFF005047).copy(alpha = 0.30f))
}

private fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color,
            0.7f to color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * Pola B — Hero banner used by the non-auth Home/Dashboard header: vertical brand gradient,
 * 196dp tall, bottom corners rounded 28dp.
 */
@Composable
fun HeroBanner(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val extended = MaterialTheme.extendedColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
            .background(
                brush = Brush.verticalGradient(
                    0f to extended.brandGradientDark,
                    0.6f to extended.brandGradientMid,
                    1f to extended.brandGradientLight,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp,
                ),
            ),
    ) {
        content()
    }
}
