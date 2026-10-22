package id.diskola.app.utils

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands a PIN from the confirmation screen back to the screen that asked for it. The PIN lives in memory only, for exactly one read:
 * it must never travel in a route argument or a `SavedStateHandle` (both can be written to the saved-state bundle) nor reach a log.
 *
 * The asking screen is not composed while the confirmation screen is on top, so a flow would miss the value; instead it
 * [take]s the answer for its own [requestKey] when it comes back.
 */
@Singleton
class PinResultChannel @Inject constructor() {

    data class Result(val pin: String, val channelName: String)

    private val results = ConcurrentHashMap<String, Result>()

    fun deliver(requestKey: String, pin: String, channelName: String = "") {
        results[requestKey] = Result(pin, channelName)
    }

    /** The answer for [requestKey], consumed; null when there is none (the user went back without confirming). */
    fun take(requestKey: String): Result? = results.remove(requestKey)

    fun clear() = results.clear()
}
