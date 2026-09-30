package id.diskola.app.ui.theme

import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale from the design handoff README. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val massive = 48.dp
}

/** Horizontal screen padding, constant across the whole app per the README. */
val ScreenHorizontalPadding = Spacing.xl

/**
 * How far the first content card is pulled up over the hero above it. Shared so a hero and the
 * screen overlapping it can't drift apart — get this wrong and the card lands on the hero's text.
 */
val HeroOverlap = 56.dp
