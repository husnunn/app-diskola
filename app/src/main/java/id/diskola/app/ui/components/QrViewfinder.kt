package id.diskola.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val QrAccent = Color(0xFF43DDBD)

/** Dashed viewfinder frame with 4 teal corner brackets, per the QR Pay mockup. */
@Composable
fun QrViewfinder(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 246.dp) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val corner = 32.dp.toPx()
            val stroke = 4.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
            )
            val w = this.size.width
            val h = this.size.height
            // Four corner brackets
            listOf(
                Offset(0f, 0f) to Offset(1f, 1f),
                Offset(w, 0f) to Offset(-1f, 1f),
                Offset(0f, h) to Offset(1f, -1f),
                Offset(w, h) to Offset(-1f, -1f),
            ).forEach { (origin, dir) ->
                drawLine(QrAccent, origin, Offset(origin.x + corner * dir.x, origin.y), strokeWidth = stroke)
                drawLine(QrAccent, origin, Offset(origin.x, origin.y + corner * dir.y), strokeWidth = stroke)
            }
        }
    }
}
