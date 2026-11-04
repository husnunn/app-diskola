package id.diskola.app.utils

/**
 * The decisions of the spotlight tour ("Panduan") that are pure arithmetic: where the speech bubble goes against the highlighted element, and
 * how the steps advance. Same placement as the legacy `OnboardingGuide.positionStep`: the bubble goes on the side of the element with more room
 * (above only when it fits), is centred on the element and kept [margin] from the screen edges, and the arrow points at the element's centre.
 * All values are pixels in the overlay's own coordinates.
 */
object CoachMarkRules {

    data class Placement(
        /** The bubble sits above the element (its arrow points down); otherwise below (arrow up). */
        val above: Boolean,
        val left: Float,
        val top: Float,
        /** Where the arrow's centre is, kept [arrowWidth] inside the bubble's edges. */
        val arrowCenterX: Float,
    )

    /** [pad] is the room the highlight frame takes around the element; [arrowWidth] keeps the arrow off the bubble's rounded corners. */
    fun place(
        targetLeft: Float,
        targetTop: Float,
        targetRight: Float,
        targetBottom: Float,
        bubbleWidth: Float,
        bubbleHeight: Float,
        areaWidth: Float,
        areaHeight: Float,
        margin: Float,
        pad: Float,
        arrowWidth: Float,
    ): Placement {
        val spaceAbove = targetTop
        val spaceBelow = areaHeight - targetBottom
        var above = spaceAbove > spaceBelow
        if (above && spaceAbove < bubbleHeight + margin + pad) above = false

        val centerX = (targetLeft + targetRight) / 2f
        val left = (centerX - bubbleWidth / 2f).coerceAtLeast(margin).coerceAtMost(areaWidth - bubbleWidth - margin)
        val top = if (above) {
            (targetTop - bubbleHeight - margin - pad).coerceAtLeast(margin)
        } else {
            (targetBottom + margin + pad).coerceAtMost(areaHeight - bubbleHeight - margin)
        }
        val arrowCenterX = centerX.coerceIn(left + arrowWidth, (left + bubbleWidth - arrowWidth).coerceAtLeast(left + arrowWidth))
        return Placement(above, left, top, arrowCenterX)
    }

    /** "Lanjut" until the last step, then "Selesai" (legacy wording). */
    fun buttonLabel(index: Int, total: Int): String = if (index >= total - 1) "Selesai" else "Lanjut"

    /** "2 dari 4". */
    fun counter(index: Int, total: Int): String = "${index + 1} dari $total"

    /** The step after [index], or null when [index] was the last one (the tour is over). */
    fun advance(index: Int, total: Int): Int? = (index + 1).takeIf { it < total }
}
