package id.diskola.app.utils

/**
 * Pure decisions of the loading skeleton (`ui/components/Skeleton.kt`): when it shows and where the shimmer's highlight is. Kept free of
 * Android so each rule has a JVM test.
 */
object SkeletonRules {

    /**
     * A skeleton appears only after this long. A load that answers sooner never shows one, so a fast network does not flash grey blocks at
     * the user; the skeleton then fades in over `Motion.FAST`, which keeps a brief appearance from reading as a flicker.
     */
    const val SHOW_DELAY_MILLIS = 200L

    /**
     * Left edge of the shimmer's highlight band for a sweep at [progress] (0..1): it starts fully off the left side and ends fully off the
     * right, so the loop never shows a band stopped in the middle. [progress] outside 0..1 is clamped.
     */
    fun bandStart(progress: Float, width: Float, bandWidth: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return -bandWidth + p * (width + bandWidth)
    }

    /** The system "remove animations" setting (`animator_duration_scale` = 0) turns the shimmer off; the blocks stay as plain shapes. */
    fun isReduceMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f
}
