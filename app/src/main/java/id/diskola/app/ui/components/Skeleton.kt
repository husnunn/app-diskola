package id.diskola.app.ui.components

import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.DiskolaTheme
import id.diskola.app.ui.theme.Motion
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.SkeletonRules

/** Which screen the skeleton stands in for: its blocks follow the shape of the real content so nothing jumps when the data arrives. */
enum class SkeletonVariant {
    /** An icon box and two text lines, repeated — lists of tugas, jurnal, materi, payment methods. */
    ListRow,

    /** A bordered card with a tick, a title, a pill and a price — the SPP bills. */
    Card,

    /** A big title, two meta lines, a paragraph and an attachment block — the detail screens. */
    Detail,
}

/** The sizes of the skeleton blocks, named here instead of scattered through the variants (they match the content they stand in for). */
internal object SkeletonSizes {
    val Icon = 44.dp
    val TitleBar = 12.dp
    val SubBar = 10.dp
    val HeadlineBar = 20.dp
    val ParagraphBar = 12.dp
    val Attachment = 72.dp
    val Check = 24.dp
    val Pill = 20.dp
    val Meta = 96.dp
    val Label = 28.dp
    val Price = 72.dp
    val Border = 1.dp
}

/** The sweep shared by every block of one skeleton; null when the user turned animations off (the blocks then stay plain). */
private val LocalShimmerProgress = staticCompositionLocalOf<State<Float>?> { null }

/** Honours the system "remove animations" setting, like `AgendaPolicySection` does. */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        SkeletonRules.isReduceMotion(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}

/**
 * One infinite transition for the whole skeleton, so the highlight moves in step across every block and a screen of thirty blocks costs
 * one animation, not thirty. The value is read where the blocks draw ([SkeletonBlock]), so a frame redraws and does not recompose.
 */
@Composable
internal fun ProvideShimmer(content: @Composable () -> Unit) {
    val progress: State<Float>? = if (rememberReduceMotion()) {
        null
    } else {
        rememberInfiniteTransition(label = "skeletonShimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(Motion.SHIMMER, easing = LinearEasing)),
            label = "skeletonShimmerProgress",
        )
    }
    CompositionLocalProvider(LocalShimmerProgress provides progress, content = content)
}

/** A grey block with the shared shimmer passing over it. Not interactive and not announced: the skeleton as a whole says "Memuat". */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = DiskolaExtraShapes.chipBadge) {
    val progress = LocalShimmerProgress.current
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val bandWidth = size.width * SHIMMER_BAND_FRACTION
                val clear = highlight.copy(alpha = 0f)
                val peak = highlight.copy(alpha = SHIMMER_PEAK_ALPHA)
                onDrawBehind {
                    drawRect(base)
                    val sweep = progress?.value ?: return@onDrawBehind
                    val start = SkeletonRules.bandStart(sweep, size.width, bandWidth)
                    drawRect(Brush.horizontalGradient(listOf(clear, peak, clear), startX = start, endX = start + bandWidth))
                }
            },
    )
}

private const val SHIMMER_BAND_FRACTION = 0.6f
private const val SHIMMER_PEAK_ALPHA = 0.7f

/** The blocks of [variant] — what [LoadingState] fades in, and what the previews draw (a preview cannot wait for the delay). */
@Composable
internal fun SkeletonContent(variant: SkeletonVariant, rows: Int) {
    ProvideShimmer {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            when (variant) {
                SkeletonVariant.ListRow -> repeat(rows) { index -> ListRowSkeleton(quiet = index > 0) }
                SkeletonVariant.Card -> repeat(rows) { CardSkeleton() }
                SkeletonVariant.Detail -> DetailSkeleton()
            }
        }
    }
}

@Composable
private fun ListRowSkeleton(quiet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        // The rows after the first are a little quieter, as the static rows were before the shimmer.
        modifier = Modifier.fillMaxWidth().alpha(if (quiet) QUIET_ALPHA else 1f),
    ) {
        SkeletonBlock(Modifier.size(SkeletonSizes.Icon), DiskolaExtraShapes.iconBox)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(SkeletonSizes.TitleBar))
            SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(SkeletonSizes.SubBar))
        }
    }
}

@Composable
private fun CardSkeleton() {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(DiskolaExtraShapes.card)
            .background(scheme.surfaceContainerLowest)
            .border(SkeletonSizes.Border, scheme.outlineVariant, DiskolaExtraShapes.card)
            .padding(vertical = Spacing.md, horizontal = Spacing.lg),
    ) {
        SkeletonBlock(Modifier.size(SkeletonSizes.Check), DiskolaExtraShapes.chipBadge)
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.md),
        ) {
            SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(SkeletonSizes.TitleBar))
            SkeletonBlock(Modifier.width(SkeletonSizes.Meta).height(SkeletonSizes.Pill), CircleShape)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = Alignment.End) {
            SkeletonBlock(Modifier.width(SkeletonSizes.Label).height(SkeletonSizes.SubBar))
            SkeletonBlock(Modifier.width(SkeletonSizes.Price).height(SkeletonSizes.TitleBar))
        }
    }
}

@Composable
private fun DetailSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg), modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(Modifier.fillMaxWidth(0.65f).height(SkeletonSizes.HeadlineBar))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SkeletonBlock(Modifier.width(SkeletonSizes.Meta).height(SkeletonSizes.SubBar))
            SkeletonBlock(Modifier.width(SkeletonSizes.Meta).height(SkeletonSizes.SubBar))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PARAGRAPH_FRACTIONS.forEach { fraction ->
                SkeletonBlock(Modifier.fillMaxWidth(fraction).height(SkeletonSizes.ParagraphBar))
            }
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(SkeletonSizes.Attachment), DiskolaExtraShapes.card)
    }
}

private const val QUIET_ALPHA = 0.7f
private val PARAGRAPH_FRACTIONS = listOf(1f, 1f, 0.9f, 0.6f)

@Preview(name = "ListRow", showBackground = true)
@Composable
private fun ListRowSkeletonPreview() {
    DiskolaTheme { Box(Modifier.padding(Spacing.lg)) { SkeletonContent(SkeletonVariant.ListRow, rows = 3) } }
}

@Preview(name = "Card", showBackground = true)
@Composable
private fun CardSkeletonPreview() {
    DiskolaTheme { Box(Modifier.padding(Spacing.lg)) { SkeletonContent(SkeletonVariant.Card, rows = 3) } }
}

@Preview(name = "Detail", showBackground = true)
@Composable
private fun DetailSkeletonPreview() {
    DiskolaTheme { Box(Modifier.padding(Spacing.lg)) { SkeletonContent(SkeletonVariant.Detail, rows = 1) } }
}

@Preview(name = "Card — font 1.5x", showBackground = true, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CardSkeletonLargeFontPreview() {
    DiskolaTheme { Box(Modifier.padding(Spacing.lg)) { SkeletonContent(SkeletonVariant.Card, rows = 2) } }
}
