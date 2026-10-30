package id.diskola.app.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A Room-backed list together with its loading flag, so a screen can tell "not known yet" from "known to be empty". An empty list before Room
 * has answered, or right after a fetch wrote rows that Room has not handed over yet, is not an empty result: showing the empty state there is
 * a flash of "nothing here" in front of the real list. So [loading] stays up until Room has answered once, while a [fetch] runs, and after a
 * [fetch] until [items] has caught up with what Room already holds. A list that really is empty releases at once — nothing is waited for.
 */
class ListLoad<T>(
    scope: CoroutineScope,
    private val source: Flow<List<T>>,
    private val catchUpTimeoutMillis: Long = CATCH_UP_TIMEOUT_MILLIS,
) {
    private val answered = MutableStateFlow(false)
    private val running = MutableStateFlow(0)

    val items: StateFlow<List<T>> = source
        .onEach { answered.value = true }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIBER_GRACE_MILLIS), emptyList())

    /** Starts true: until Room answers, an empty [items] only means "not asked yet". */
    val loading: StateFlow<Boolean> = combine(running, answered) { runs, hasAnswered -> runs > 0 || !hasAnswered }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIBER_GRACE_MILLIS), true)

    /** Runs [block] (a network fetch that writes into Room) with [loading] up, and keeps it up until the list has caught up with the write. */
    suspend fun <R> fetch(block: suspend () -> R): R {
        running.update { it + 1 }
        try {
            return block()
        } finally {
            try {
                awaitCaughtUp()
            } finally {
                running.update { it - 1 }
            }
        }
    }

    /** Rows that Room already holds but [items] does not show yet: wait for them, but never longer than [catchUpTimeoutMillis]. */
    private suspend fun awaitCaughtUp() {
        if (items.value.isNotEmpty()) return
        if (source.firstOrNull().orEmpty().isEmpty()) return
        withTimeoutOrNull(catchUpTimeoutMillis) { items.first { it.isNotEmpty() } }
    }

    companion object {
        const val CATCH_UP_TIMEOUT_MILLIS = 1500L
        private const val SUBSCRIBER_GRACE_MILLIS = 5000L
    }
}
