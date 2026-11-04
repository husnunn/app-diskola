package id.diskola.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Motion
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.CoachMarkRules

/** One stop of the tour: the element it points at ([key], see [coachTarget]), a title and a sentence. */
data class CoachStep(
    val key: String,
    val title: String,
    val description: String,
    /** Room between the element and the highlight frame. */
    val padding: Dp = 8.dp,
    val cornerRadius: Dp = 16.dp,
)

/** Where each tour target is on screen; the screen records them with [coachTarget] and hands this to [CoachMarkOverlay]. */
@Stable
class CoachMarkState {
    internal val bounds = mutableStateMapOf<String, Rect>()
}

@Composable
fun rememberCoachMarkState(): CoachMarkState = remember { CoachMarkState() }

/** Marks this element as the target [key] of a tour; its bounds follow it through scrolling and layout changes. */
fun Modifier.coachTarget(state: CoachMarkState, key: String): Modifier =
    onGloballyPositioned { state.bounds[key] = it.boundsInRoot() }

/**
 * The spotlight tour behind a "Panduan" button (legacy `OnboardingGuide`): the screen is dimmed, the element of the current step is cut out
 * of the dimming and framed, and a bubble with an arrow says what it is. "Lanjut" goes on, the last step says "Selesai"; the back button
 * closes the tour. Draw this above the whole screen, in a [Box] that covers it, so the app bar can be a target too.
 */
@Composable
fun CoachMarkOverlay(
    steps: List<CoachStep>,
    state: CoachMarkState,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    var origin by remember { mutableStateOf(Offset.Zero) }
    val scheme = MaterialTheme.colorScheme

    BackHandler(onBack = onFinish)

    // The overlay may not start at the root's origin (insets), so a target's root position is turned into the overlay's own.
    val target = state.bounds[step.key]?.translate(-origin)
    val animated by animateRectAsState(
        targetValue = target ?: Rect.Zero,
        animationSpec = tween(if (rememberReduceMotion()) 0 else Motion.BASE),
        label = "coachHighlight",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            // Eats the taps: nothing under the dimming reacts while the tour runs.
            .pointerInput(Unit) { detectTapGestures { } }
            .semantics { paneTitle = "Panduan" },
    ) {
        val scrim = scheme.scrim.copy(alpha = 0.6f)
        val frame = scheme.primary
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(scrim)
            if (target != null) {
                val pad = step.padding.toPx()
                val topLeft = Offset(animated.left - pad, animated.top - pad)
                val size = Size(animated.width + pad * 2, animated.height + pad * 2)
                val radius = CornerRadius(step.cornerRadius.toPx())
                drawRoundRect(Color.Black, topLeft, size, radius, blendMode = BlendMode.Clear)
                drawRoundRect(frame, topLeft, size, radius, style = Stroke(width = 2.dp.toPx()))
            }
        }

        val bubbleColor = scheme.surfaceContainerLowest
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                CoachBubble(
                    step = step,
                    label = CoachMarkRules.buttonLabel(index, steps.size),
                    counter = CoachMarkRules.counter(index, steps.size),
                    color = bubbleColor,
                    onNext = { CoachMarkRules.advance(index, steps.size)?.let { index = it } ?: onFinish() },
                )
                CoachArrow(up = true, color = bubbleColor)
                CoachArrow(up = false, color = bubbleColor)
            },
        ) { measurables, constraints ->
            val margin = 16.dp.roundToPx()
            val areaW = constraints.maxWidth
            val areaH = constraints.maxHeight
            val usable = areaW - margin * 2
            val bubble = measurables[0].measure(
                Constraints(minWidth = minOf(280.dp.roundToPx(), usable), maxWidth = minOf(320.dp.roundToPx(), usable), maxHeight = areaH),
            )
            val arrowUp = measurables[1].measure(Constraints())
            val arrowDown = measurables[2].measure(Constraints())
            layout(areaW, areaH) {
                if (target == null) {
                    bubble.place((areaW - bubble.width) / 2, (areaH - bubble.height) / 2)
                } else {
                    val placement = CoachMarkRules.place(
                        targetLeft = animated.left,
                        targetTop = animated.top,
                        targetRight = animated.right,
                        targetBottom = animated.bottom,
                        bubbleWidth = bubble.width.toFloat(),
                        bubbleHeight = bubble.height.toFloat(),
                        areaWidth = areaW.toFloat(),
                        areaHeight = areaH.toFloat(),
                        margin = 8.dp.toPx(),
                        pad = step.padding.toPx(),
                        arrowWidth = arrowUp.width.toFloat(),
                    )
                    bubble.place(placement.left.toInt(), placement.top.toInt())
                    val arrowX = (placement.arrowCenterX - arrowUp.width / 2f).toInt()
                    if (placement.above) {
                        arrowDown.place(arrowX, placement.top.toInt() + bubble.height)
                    } else {
                        arrowUp.place(arrowX, placement.top.toInt() - arrowUp.height)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachBubble(step: CoachStep, label: String, counter: String, color: Color, onNext: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .background(color, RoundedCornerShape(16.dp))
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // The words are a live region: moving to the next step is read out without the button losing its own stop.
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Box(
                    modifier = Modifier.size(36.dp).background(scheme.surfaceContainerLow, DiskolaExtraShapes.iconBox),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = scheme.primary)
                }
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(step.description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        }
        HorizontalDivider(color = scheme.outlineVariant, modifier = Modifier.padding(top = Spacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(counter, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(Spacing.md))
            AppButton(text = label, onClick = onNext)
        }
    }
}

/** The bubble's pointer: apex up when the bubble is below its element, down when above. */
@Composable
private fun CoachArrow(up: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 16.dp, height = 10.dp)
            .drawBehind {
                val path = Path().apply {
                    if (up) {
                        moveTo(0f, size.height)
                        lineTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                    } else {
                        moveTo(0f, 0f)
                        lineTo(size.width / 2f, size.height)
                        lineTo(size.width, 0f)
                    }
                    close()
                }
                drawPath(path, color)
            },
    )
}

// ---- Previews ----
// A screen-sized box with two fake targets (one near the top, one near the bottom), so the bubble is seen below and above its element.

@Composable
private fun CoachPreview(first: Boolean) {
    id.diskola.app.ui.theme.DiskolaTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        val state = rememberCoachMarkState()
        state.bounds["top"] = Rect(40f, 260f, 1000f, 420f)
        state.bounds["bottom"] = Rect(40f, 1500f, 1000f, 1700f)
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                CoachMarkOverlay(
                    steps = listOf(
                        CoachStep("top", "Pilih Jadwal", "Ketuk Jadwal untuk melihat daftar jadwal magang yang tersedia."),
                        CoachStep("bottom", "Tambah Izin", "Ketuk ikon ini untuk mengajukan atau melihat riwayat izin/sakit magang."),
                    ).let { if (first) it else it.reversed() },
                    state = state,
                    onFinish = {},
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Coach, bubble below", showBackground = true, widthDp = 360, heightDp = 780)
@androidx.compose.ui.tooling.preview.Preview(name = "Coach, bubble below, dark", showBackground = true, widthDp = 360, heightDp = 780, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@androidx.compose.ui.tooling.preview.Preview(name = "Coach, bubble below, large font", showBackground = true, widthDp = 360, heightDp = 780, fontScale = 1.5f)
@Composable
private fun CoachBelowPreview() = CoachPreview(first = true)

@androidx.compose.ui.tooling.preview.Preview(name = "Coach, bubble above", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CoachAbovePreview() = CoachPreview(first = false)
