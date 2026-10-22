package id.diskola.app.utils

/**
 * The screen-pinning ("mode ketat") state machine of an exam session (doc `06` §9, legacy `ExamLockdown`). It holds no Android types:
 * the clock and "is the task pinned right now" are passed in, and what the Android side must do comes back as an [Action], so every
 * transition has a JVM test. All times are `SystemClock.elapsedRealtime()` milliseconds.
 *
 * Differences from legacy, all deliberate (`docs/FLOW_QUESTIONS.md`, 2026-10-16 "A2"):
 *  - [forceRelock] works before the first pin (legacy: a no-op, so a student whose device refused the pin was stuck on a dialog that
 *    cannot be dismissed), and after [FAILED_RELOCKS_BEFORE_ESCAPE] failed taps [escapeAvailable] offers a way out.
 *  - Whether the system prompt ended is decided by [evaluate] from events (window focus lost/regained, the pin state, the 20 s window)
 *    instead of a fixed 1.8 s guess that also forgot the prompt window and so never noticed a late "Start".
 */
object AkmLockdownRules {

    /** Before the first successful pin only this many requests are made (afterwards unlimited — the student unpinned on purpose). */
    const val MAX_ATTEMPTS = 2

    /** How long the system's "Screen pinning" prompt is given to be answered. */
    const val PROMPT_TIMEOUT_MS = 20_000L

    /** No window-focus change this long after the request ⇒ the system showed no prompt at all (pinning switched off in Settings). */
    const val NO_DIALOG_MS = 2_500L

    /** After focus returns from the prompt the pin state may lag a moment behind; wait this long before calling it a "no". */
    const val FOCUS_SETTLE_MS = 800L

    /** A deliberate exit (opening the data settings) is not a breach for this long. */
    const val INTENTIONAL_EXIT_MS = 10_000L

    /** A session older than this releases the pin by itself so a stuck exam can never lock a phone for good. */
    const val MAX_SESSION_MS = 6L * 60 * 60 * 1000

    /** `startLockTask` throws "task not in foreground" on some OEMs right after a screen change: retry this many times. */
    const val FOREGROUND_RACE_RETRIES = 3
    const val FOREGROUND_RACE_RETRY_DELAY_MS = 300L

    /** After this many failed taps on "Aktifkan" the recovery dialog also offers "Keluar dari ujian". */
    const val FAILED_RELOCKS_BEFORE_ESCAPE = 3

    data class State(
        /** The session wants the screen pinned. */
        val desired: Boolean = false,
        val armedAt: Long = 0L,
        /** The task has been pinned at least once in this session: from then on an unpin is a breach. */
        val everPinned: Boolean = false,
        /** The last request was refused or ignored; nothing is requested again until [retryLock]/[forceRelock]. */
        val declined: Boolean = false,
        val attempts: Int = 0,
        /** When the request was made (0 = none pending). */
        val promptSince: Long = 0L,
        val focusLostDuringPrompt: Boolean = false,
        val focusRegainedAt: Long = 0L,
        val intentionalUntil: Long = 0L,
        /** The pending request came from the recovery dialog's "Aktifkan" (its failures count towards the escape). */
        val viaRecovery: Boolean = false,
        val failedRelocks: Int = 0,
    )

    enum class Action { None, StartLockTask }

    data class Step(val state: State, val action: Action = Action.None)

    fun arm(now: Long): State = State(desired = true, armedAt = now)

    fun promptPending(state: State, now: Long): Boolean = state.promptSince != 0L && now - state.promptSince < PROMPT_TIMEOUT_MS

    fun isIntentionalExit(state: State, now: Long): Boolean = state.intentionalUntil != 0L && now < state.intentionalUntil

    /**
     * Pinned before and not pinned now, with no prompt open and no deliberate exit: the student (or the system) took the pin away.
     * Every protected action is held back and the recovery dialog shows (`enforceLockdownAtCheckpoint`).
     */
    fun hasActiveBreach(state: State, now: Long, pinned: Boolean): Boolean =
        state.desired && state.everPinned && !promptPending(state, now) && !isIntentionalExit(state, now) && !pinned

    /** One request to the system, if the rules allow one right now. */
    fun requestLock(state: State, now: Long, pinned: Boolean): Step {
        if (!state.desired || state.declined) return Step(state)
        if (!state.everPinned && state.attempts >= MAX_ATTEMPTS) return Step(state)
        if (pinned) return Step(state.copy(everPinned = true))
        val next = state.copy(
            attempts = state.attempts + 1,
            promptSince = now,
            focusLostDuringPrompt = false,
            focusRegainedAt = 0L,
        )
        return Step(next, Action.StartLockTask)
    }

    /** Called whenever the session screen is shown again (`onResume`): pins again if the student unpinned and came back. */
    fun ensureLocked(state: State, now: Long, pinned: Boolean): Step {
        if (!state.desired || state.declined || promptPending(state, now)) return Step(state)
        if (isIntentionalExit(state, now)) return Step(state)
        if (pinned) return Step(state.copy(everPinned = true))
        return requestLock(state, now, pinned)
    }

    /** Start of the session: clears a previous refusal and, before the first pin, the attempt count. */
    fun retryLock(state: State, now: Long, pinned: Boolean): Step {
        if (!state.desired || isIntentionalExit(state, now)) return Step(state)
        return requestLock(clearedForNewRequest(state), now, pinned)
    }

    /** The recovery dialog's "Aktifkan". Unlike legacy this also works before the first pin. */
    fun forceRelock(state: State, now: Long, pinned: Boolean): Step {
        if (!state.desired) return Step(state)
        val cleared = clearedForNewRequest(state).copy(intentionalUntil = 0L, viaRecovery = true)
        return requestLock(cleared, now, pinned)
    }

    private fun clearedForNewRequest(state: State) = state.copy(
        declined = false,
        promptSince = 0L,
        focusLostDuringPrompt = false,
        focusRegainedAt = 0L,
        attempts = if (state.everPinned) state.attempts else 0,
        viaRecovery = false,
    )

    /** `startLockTask()` threw for good (device without pinning, or every retry failed): the request is over and refused. */
    fun onStartLockTaskFailed(state: State): State = state.copy(
        promptSince = 0L,
        focusLostDuringPrompt = false,
        focusRegainedAt = 0L,
        declined = true,
        failedRelocks = if (state.viaRecovery) state.failedRelocks + 1 else state.failedRelocks,
        viaRecovery = false,
    )

    /** Window focus moved while a request is open: the system prompt took (or gave back) the focus. */
    fun onFocusChanged(state: State, hasFocus: Boolean, now: Long): State {
        if (!state.desired || state.promptSince == 0L) return state
        return when {
            !hasFocus -> state.copy(focusLostDuringPrompt = true, focusRegainedAt = 0L)
            state.focusLostDuringPrompt -> state.copy(focusRegainedAt = now)
            else -> state
        }
    }

    /**
     * Decides how an open request ended; call it on focus changes and while a request is pending. Pinned ⇒ success. Otherwise it is
     * refused when the prompt was dismissed without a pin, when no prompt ever appeared, or when the 20 s window ran out.
     */
    fun evaluate(state: State, now: Long, pinned: Boolean): State {
        if (!state.desired) return state
        if (pinned) {
            return state.copy(
                everPinned = true,
                declined = false,
                promptSince = 0L,
                focusLostDuringPrompt = false,
                focusRegainedAt = 0L,
                viaRecovery = false,
                failedRelocks = 0,
            )
        }
        if (state.promptSince == 0L) return state
        val dismissed = state.focusRegainedAt != 0L && now - state.focusRegainedAt >= FOCUS_SETTLE_MS
        val noDialog = !state.focusLostDuringPrompt && now - state.promptSince >= NO_DIALOG_MS
        val timedOut = now - state.promptSince >= PROMPT_TIMEOUT_MS
        if (!dismissed && !noDialog && !timedOut) return state
        return state.copy(
            declined = true,
            promptSince = 0L,
            focusLostDuringPrompt = false,
            focusRegainedAt = 0L,
            failedRelocks = if (state.viaRecovery) state.failedRelocks + 1 else state.failedRelocks,
            viaRecovery = false,
        )
    }

    /** The recovery dialog may offer "Keluar dari ujian" once the student has tried to re-enable the pin this many times in vain. */
    fun escapeAvailable(state: State): Boolean = state.failedRelocks >= FAILED_RELOCKS_BEFORE_ESCAPE

    /** Opening an app setting on purpose (data settings): the pin is dropped for a moment and that is not a breach. */
    fun beginTemporaryExit(state: State, now: Long, durationMs: Long = INTENTIONAL_EXIT_MS): State =
        state.copy(intentionalUntil = now + durationMs)

    fun endTemporaryExit(state: State): State = state.copy(intentionalUntil = 0L)

    /** The session is over: no longer wanted, and the unpin that follows is not a breach. */
    fun release(state: State, now: Long): State =
        state.copy(desired = false, promptSince = 0L, intentionalUntil = now + INTENTIONAL_EXIT_MS)

    fun watchdogExpired(state: State, now: Long): Boolean = state.desired && now - state.armedAt >= MAX_SESSION_MS
}
