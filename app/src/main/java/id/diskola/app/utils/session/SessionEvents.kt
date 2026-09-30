package id.diskola.app.utils.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionEvent {
    /** Emitted by `ResponseInterceptor` on HTTP 401 while a session was logged in — doc
     * `02-auth-login-sesi.md` §9, decision Q6: handled centrally (one dialog, one place), instead
     * of every screen reacting to its own 401 like the legacy app did. */
    data object Unauthorized : SessionEvent

    /** Emitted after [id.diskola.app.utils.session.SessionManager.logout] finishes because there
     * was a pending AKM exam — the collector should show the "Perhatian" alert and offer to open
     * the exam list, exactly like the manual logout button does (doc §10.1). */
    data object LogoutBlockedByPendingExam : SessionEvent
}

/**
 * Where `ResponseInterceptor` (an OkHttp interceptor running off the main thread, with no
 * `Activity`/`Composable` of its own) hands a 401 to whatever is currently on screen. A single
 * top-level collector in `AppNavHost` reacts to it with a proper dialog + [SessionManager] call,
 * instead of the interceptor starting an `Activity` directly.
 */
@Singleton
class SessionEvents @Inject constructor() {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun emit(event: SessionEvent) {
        _events.tryEmit(event)
    }
}
