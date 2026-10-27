package id.diskola.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AkmRules

// The "Dibatalkan" mark of a schedule (doc `06` §4.4). All three pieces are purely visual: none of them takes a touch, so nothing
// underneath is ever blocked, and none of them changes whether an exam can be downloaded, started or submitted.

/** Solid red capsule on the schedule card, under the exam name (legacy `bg_watermark_label`). */
@Composable
fun WatermarkLabel(label: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
        color = scheme.onError,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(scheme.error, DiskolaExtraShapes.chipBadge)
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
    )
}

/**
 * Large tilted, boxed stamp **behind** [content] (legacy `AkmStamp`, alpha ≈ 31 %). The text size follows the width of the screen so a long
 * custom label still fits on one line.
 */
@Composable
fun AkmStamp(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val color = MaterialTheme.colorScheme.error
    Box(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val size = (maxWidth.value * 1.3f / label.length.coerceAtLeast(1)).coerceIn(24f, 56f)
            Text(
                text = label,
                style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = color),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(-18f)
                    .alpha(STAMP_ALPHA)
                    .border(4.dp, color, RoundedCornerShape(10.dp))
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        content()
    }
}

/** [AkmStamp] when the schedule carries the mark, otherwise just [content] — callers need no `if`. */
@Composable
fun StampedIf(watermark: AkmRules.AkmWatermark, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (watermark.enabled) {
        AkmStamp(label = watermark.label, modifier = modifier, content = content)
    } else {
        Box(modifier = modifier) { content() }
    }
}

/**
 * Small text repeated diagonally over the whole screen (legacy `AkmWatermark`: rotation −30°, alpha ≈ 22 %), for the question page.
 * A plain [Canvas] with no pointer handling, so touches pass straight through to the question below.
 */
@Composable
fun AkmWatermarkOverlay(label: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val color = MaterialTheme.colorScheme.onSurface
    val style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = OVERLAY_ALPHA))
    Canvas(modifier = modifier.fillMaxSize()) {
        val text = measurer.measure(label, style = style, maxLines = 1)
        val stepX = text.size.width + 56.dp.toPx()
        val stepY = text.size.height + 64.dp.toPx()
        val reach = maxOf(size.width, size.height)
        rotate(degrees = -30f, pivot = center) {
            var row = 0
            var y = center.y - reach
            while (y < center.y + reach) {
                var x = center.x - reach - if (row % 2 == 0) 0f else stepX / 2
                while (x < center.x + reach) {
                    drawText(text, topLeft = Offset(x, y))
                    x += stepX
                }
                y += stepY
                row++
            }
        }
    }
}

private const val STAMP_ALPHA = 0.31f
private const val OVERLAY_ALPHA = 0.22f

@Preview(showBackground = true)
@Composable
private fun WatermarkLabelPreview() {
    DiskolaTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WatermarkLabel("DIBATALKAN")
            WatermarkLabel("SESI DIGANTI DENGAN JADWAL SUSULAN YANG DIUMUMKAN OLEH PANITIA UJIAN")
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AkmStampPreview() {
    DiskolaTheme(darkTheme = isSystemInDarkTheme()) {
        AkmStamp("DIBATALKAN", modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TEST SOAL ASESMEN", style = MaterialTheme.typography.titleLarge)
                Text("Berakhir 15 Okt 2026 · 16.13 · 3 soal", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AkmWatermarkOverlayPreview() {
    DiskolaTheme {
        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            Text("Soal 1 dari 3", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            AkmWatermarkOverlay("DIBATALKAN")
        }
    }
}
