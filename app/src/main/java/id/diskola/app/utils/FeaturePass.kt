package id.diskola.app.utils

/**
 * A one-shot "the gate just passed" note between the screen that asked the server (the Pembayaran hub) and the screen it opens (Bayar
 * SPP), so the second one does not ask again straight away (doc 08 §17.3: once per navigation). It lives in memory only and goes stale
 * after [ttlMillis]: after the process died, or when the screen is opened from somewhere else (notification, push), there is no
 * fresh note and the opened screen asks the server itself.
 */
class FeaturePass(
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = TTL_MS,
) {
    private val grantedAt = mutableMapOf<String, Long>()

    @Synchronized
    fun grant(name: String) {
        grantedAt[name] = clock()
    }

    /** True once per [grant], and only while the note is fresh. */
    @Synchronized
    fun consume(name: String): Boolean {
        val at = grantedAt.remove(name) ?: return false
        return clock() - at in 0..ttlMillis
    }

    companion object {
        const val TTL_MS = 10_000L
    }
}
