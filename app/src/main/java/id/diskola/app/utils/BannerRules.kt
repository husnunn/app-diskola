package id.diskola.app.utils

/**
 * How long an inline error banner stays before it clears itself (the X still clears it at once). Legacy showed these errors as a `Toast`, which
 * goes by itself; a banner that only the X could remove stayed on top of the form, pushed the fields down and outlived the thing it complained about.
 */
object BannerRules {
    const val MIN_MILLIS = 5_000L
    const val MAX_MILLIS = 12_000L
    private const val BASE_MILLIS = 3_000L
    private const val PER_CHAR_MILLIS = 60L

    /** Time to read the text (about 17 characters a second) plus a short lead-in, kept between [MIN_MILLIS] and [MAX_MILLIS]. */
    fun autoDismissMillis(message: String): Long =
        (BASE_MILLIS + message.length * PER_CHAR_MILLIS).coerceIn(MIN_MILLIS, MAX_MILLIS)
}
