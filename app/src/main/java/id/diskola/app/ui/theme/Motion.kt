package id.diskola.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/** Durations (ms) and easings from the design handoff README. */
object Motion {
    const val FAST = 150
    const val BASE = 250
    const val SLOW = 400

    val EaseIn = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EaseOut = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val PRESS_SCALE = 0.97f
}
