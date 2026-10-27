package id.diskola.app.ui.components

import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import id.diskola.app.ui.theme.DiskolaExtraShapes
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.ui.theme.extendedColors
import androidx.core.view.doOnLayout
import id.diskola.app.utils.HtmlMathRenderer
import id.diskola.app.utils.containsMath

/** ALL-CAPS section label used throughout the Materi and Asesmen screens. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Thin determinate bar used for download progress and per-subtest scores. */
@Composable
fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
    }
}

/**
 * Top bar shown while an exam is in progress. Replaces the normal app bar: there is no back
 * affordance unless [onBack] is given, because leaving mid-exam is a penalised action.
 */
@Composable
fun ExamModeHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    penaltyLabel: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    val extended = MaterialTheme.extendedColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(extended.examSurface)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = extended.onExamSurface)
            }
        } else {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(Icons.Rounded.Assignment, contentDescription = null, tint = extended.onExamSurface, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Spacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = extended.onExamSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.onExamSurface.copy(alpha = 0.82f),
                )
            }
        }
        penaltyLabel?.let { label ->
            Row(
                modifier = Modifier
                    .background(extended.penaltyContainer, RoundedCornerShape(13.dp))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = extended.onPenaltyContainer, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = extended.onPenaltyContainer,
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        trailing()
    }
}

/**
 * Renders AKM question/answer text, which is raw server HTML that may hold LaTeX formulas (`\( \)`, `\[ \]`, `$$ $$`, doc 06 §5.8). Compose's
 * `Text()` cannot host the `ImageSpan`-based formula drawing, so this wraps a plain `TextView` ([HtmlMathRenderer]) in an `AndroidView`.
 *
 * - [color] is handed to the renderer explicitly: a formula is drawn, not text, so it does not follow the TextView's text colour.
 * - The width is only known after layout, and a formula wider than its row is scaled down to it — so the text is rendered once at once
 *   and once more after the first layout (and again if the width changes). The key `"colour|width|html"` keeps identical re-binds free.
 * - [dataValueFormulas] is for the **question text** only (legacy turns a WYSIWYG plugin's `data-value` into a formula there, never in answers).
 * - [enableLinks] is off by default: `LinkMovementMethod` makes the TextView swallow touches, which broke the Menjodohkan long-press drag and
 *   the tap on an answer row.
 */
@Composable
fun MathHtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = LocalContentColor.current,
    dataValueFormulas: Boolean = false,
    enableLinks: Boolean = false,
) {
    val textSizeSp = if (style.fontSize.type == TextUnitType.Sp) style.fontSize.value else 14f
    val textColorArgb = color.toArgb()
    val isBold = (style.fontWeight?.weight ?: 400) >= FontWeight.Bold.weight
    val source = if (dataValueFormulas) HtmlMathRenderer.preprocessDataValueFormula(html) else html
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                // This text is never interactive (no real links/selection in AKM content) — make
                // sure it never claims touch itself, so gestures on ancestor composables (e.g. the
                // Menjodohkan drag handle wrapping this) always see the full press-and-drag stream.
                isClickable = false
                isLongClickable = false
                isFocusable = false
            }
        },
        update = { textView ->
            textView.textSize = textSizeSp
            textView.setTextColor(textColorArgb)
            textView.setTypeface(textView.typeface, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            renderMath(textView, source, textColorArgb, enableLinks)
        },
    )
}

/** Draws [source] into [view] unless exactly this (colour, width, text) is already drawn; draws again after layout when the width was still 0. */
private fun renderMath(view: TextView, source: String, @ColorInt color: Int, enableLinks: Boolean) {
    val key = "$color|${view.width}|$enableLinks|$source"
    if (view.tag == key) return
    view.tag = key
    HtmlMathRenderer.setTextWithMath(view, source, color, enableLinks)
    if (view.width == 0 && containsMath(source)) {
        view.doOnLayout { if (view.tag == key) renderMath(view, source, color, enableLinks) }
    }
}

/**
 * One answer choice. [letter] is shown for single-answer questions (A/B/C/D) and omitted for
 * multi-select, matching the handoff.
 */
@Composable
fun AnswerOption(
    label: String,
    selected: Boolean,
    multiSelect: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    letter: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val icon = when {
        multiSelect && selected -> Icons.Rounded.CheckBox
        multiSelect -> Icons.Rounded.CheckBoxOutlineBlank
        selected -> Icons.Rounded.RadioButtonChecked
        else -> Icons.Rounded.RadioButtonUnchecked
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(
                if (selected) scheme.surfaceContainerLow else scheme.surfaceContainerLowest,
                DiskolaExtraShapes.card,
            )
            .border(
                width = 1.5.dp,
                color = if (selected) scheme.primary else scheme.outlineVariant,
                shape = DiskolaExtraShapes.card,
            )
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) scheme.primary else scheme.outline,
            modifier = Modifier.size(22.dp),
        )
        if (letter != null) {
            Text(
                letter,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp),
            )
        }
        MathHtmlText(label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
    }
}

/** Big circular score readout on the Hasil screen. */
@Composable
fun ScoreRing(score: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(120.dp)
            .background(scheme.surfaceContainerLow, CircleShape)
            .border(8.dp, scheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                score.toString(),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = scheme.primary,
            )
            Text(
                "NILAI",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
