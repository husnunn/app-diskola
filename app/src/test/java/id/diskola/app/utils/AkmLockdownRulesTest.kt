package id.diskola.app.utils

import id.diskola.app.utils.AkmLockdownRules.Action
import id.diskola.app.utils.AkmLockdownRules.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmLockdownRulesTest {

    private val t0 = 1_000_000L

    private fun armed() = AkmLockdownRules.arm(t0)

    /** An armed session whose first request is open since [t0]. */
    private fun requested(): State = AkmLockdownRules.retryLock(armed(), t0, pinned = false).state

    // ---- requesting ----

    @Test
    fun `arming wants a pin but asks the system for nothing yet`() {
        val state = armed()
        assertTrue(state.desired)
        assertFalse(state.everPinned)
        assertEquals(t0, state.armedAt)
    }

    @Test
    fun `the first request asks the system and opens the prompt window`() {
        val step = AkmLockdownRules.retryLock(armed(), t0, pinned = false)
        assertEquals(Action.StartLockTask, step.action)
        assertEquals(1, step.state.attempts)
        assertTrue(AkmLockdownRules.promptPending(step.state, t0 + 1))
        assertFalse(AkmLockdownRules.promptPending(step.state, t0 + AkmLockdownRules.PROMPT_TIMEOUT_MS))
    }

    @Test
    fun `an already pinned task needs no request`() {
        val step = AkmLockdownRules.retryLock(armed(), t0, pinned = true)
        assertEquals(Action.None, step.action)
        assertTrue(step.state.everPinned)
    }

    @Test
    fun `nothing is requested for a session that is not wanted`() {
        val released = AkmLockdownRules.release(armed(), t0)
        assertEquals(Action.None, AkmLockdownRules.requestLock(released, t0, pinned = false).action)
        assertEquals(Action.None, AkmLockdownRules.retryLock(released, t0, pinned = false).action)
        assertEquals(Action.None, AkmLockdownRules.forceRelock(released, t0, pinned = false).action)
    }

    @Test
    fun `before the first pin only two requests are made, then silence`() {
        var state = armed()
        repeat(2) {
            val step = AkmLockdownRules.requestLock(state, t0, pinned = false)
            assertEquals(Action.StartLockTask, step.action)
            // the request is answered with a refusal so the next one may be made
            state = step.state.copy(declined = false, promptSince = 0L)
        }
        assertEquals(Action.None, AkmLockdownRules.requestLock(state, t0, pinned = false).action)
    }

    @Test
    fun `after the first pin requests are not capped`() {
        val state = armed().copy(everPinned = true, attempts = 9)
        assertEquals(Action.StartLockTask, AkmLockdownRules.requestLock(state, t0, pinned = false).action)
    }

    @Test
    fun `a refused request is not repeated by ensureLocked`() {
        val declined = AkmLockdownRules.onStartLockTaskFailed(requested())
        assertTrue(declined.declined)
        assertEquals(Action.None, AkmLockdownRules.ensureLocked(declined, t0 + 5_000, pinned = false).action)
    }

    @Test
    fun `ensureLocked pins again when the student came back after unpinning`() {
        val state = armed().copy(everPinned = true)
        val step = AkmLockdownRules.ensureLocked(state, t0 + 60_000, pinned = false)
        assertEquals(Action.StartLockTask, step.action)
    }

    @Test
    fun `ensureLocked leaves a pending prompt and a deliberate exit alone`() {
        val pending = requested()
        assertEquals(Action.None, AkmLockdownRules.ensureLocked(pending, t0 + 100, pinned = false).action)
        val exiting = AkmLockdownRules.beginTemporaryExit(armed().copy(everPinned = true), t0)
        assertEquals(Action.None, AkmLockdownRules.ensureLocked(exiting, t0 + 100, pinned = false).action)
    }

    // ---- how a request ends ----

    @Test
    fun `a pin that appears means success and resets the recovery counters`() {
        val state = requested().copy(failedRelocks = 2, viaRecovery = true)
        val done = AkmLockdownRules.evaluate(state, t0 + 400, pinned = true)
        assertTrue(done.everPinned)
        assertFalse(done.declined)
        assertEquals(0L, done.promptSince)
        assertEquals(0, done.failedRelocks)
    }

    @Test
    fun `no focus change soon after the request means the system showed no prompt`() {
        val waiting = requested()
        assertEquals(waiting, AkmLockdownRules.evaluate(waiting, t0 + AkmLockdownRules.NO_DIALOG_MS - 1, pinned = false))
        val declined = AkmLockdownRules.evaluate(waiting, t0 + AkmLockdownRules.NO_DIALOG_MS, pinned = false)
        assertTrue(declined.declined)
        assertEquals(0L, declined.promptSince)
    }

    @Test
    fun `while the prompt is on screen the student gets the whole 20 seconds`() {
        var state = AkmLockdownRules.onFocusChanged(requested(), hasFocus = false, now = t0 + 300)
        // 10 s later the dialog is still open: neither refused nor pinned
        state = AkmLockdownRules.evaluate(state, t0 + 10_000, pinned = false)
        assertFalse(state.declined)
        assertTrue(AkmLockdownRules.promptPending(state, t0 + 10_000))
        // and a late "Start" is recognised
        val pinned = AkmLockdownRules.evaluate(state, t0 + 12_000, pinned = true)
        assertTrue(pinned.everPinned)
        assertFalse(pinned.declined)
    }

    @Test
    fun `a prompt that is dismissed without a pin is a refusal once focus has settled`() {
        var state = AkmLockdownRules.onFocusChanged(requested(), hasFocus = false, now = t0 + 300)
        state = AkmLockdownRules.onFocusChanged(state, hasFocus = true, now = t0 + 3_000)
        assertFalse(AkmLockdownRules.evaluate(state, t0 + 3_000 + AkmLockdownRules.FOCUS_SETTLE_MS - 1, pinned = false).declined)
        assertTrue(AkmLockdownRules.evaluate(state, t0 + 3_000 + AkmLockdownRules.FOCUS_SETTLE_MS, pinned = false).declined)
    }

    @Test
    fun `a prompt nobody answers is refused when the window runs out`() {
        val state = AkmLockdownRules.onFocusChanged(requested(), hasFocus = false, now = t0 + 300)
        val declined = AkmLockdownRules.evaluate(state, t0 + AkmLockdownRules.PROMPT_TIMEOUT_MS, pinned = false)
        assertTrue(declined.declined)
    }

    @Test
    fun `focus changes outside a request change nothing`() {
        val idle = armed()
        assertEquals(idle, AkmLockdownRules.onFocusChanged(idle, hasFocus = false, now = t0))
        assertEquals(idle, AkmLockdownRules.onFocusChanged(idle, hasFocus = true, now = t0))
    }

    // ---- breach ----

    @Test
    fun `losing a pin that was held is a breach, never having had one is not`() {
        val held = armed().copy(everPinned = true)
        assertTrue(AkmLockdownRules.hasActiveBreach(held, t0 + 10, pinned = false))
        assertFalse(AkmLockdownRules.hasActiveBreach(held, t0 + 10, pinned = true))
        assertFalse(AkmLockdownRules.hasActiveBreach(armed(), t0 + 10, pinned = false))
    }

    @Test
    fun `no breach while a prompt is open, during a deliberate exit, or after the session ended`() {
        val held = armed().copy(everPinned = true)
        val asking = held.copy(promptSince = t0)
        assertFalse(AkmLockdownRules.hasActiveBreach(asking, t0 + 100, pinned = false))
        val exiting = AkmLockdownRules.beginTemporaryExit(held, t0)
        assertFalse(AkmLockdownRules.hasActiveBreach(exiting, t0 + 100, pinned = false))
        assertTrue(AkmLockdownRules.hasActiveBreach(exiting, t0 + AkmLockdownRules.INTENTIONAL_EXIT_MS, pinned = false))
        assertFalse(AkmLockdownRules.hasActiveBreach(AkmLockdownRules.release(held, t0), t0 + 100, pinned = false))
    }

    @Test
    fun `ending a deliberate exit makes the breach count again at once`() {
        val exiting = AkmLockdownRules.beginTemporaryExit(armed().copy(everPinned = true), t0)
        val back = AkmLockdownRules.endTemporaryExit(exiting)
        assertTrue(AkmLockdownRules.hasActiveBreach(back, t0 + 100, pinned = false))
    }

    // ---- recovery dialog ----

    @Test
    fun `Aktifkan works even though the first pin never happened (legacy forceRelock did nothing)`() {
        val declined = AkmLockdownRules.onStartLockTaskFailed(requested())
        assertFalse(declined.everPinned)
        val step = AkmLockdownRules.forceRelock(declined, t0 + 5_000, pinned = false)
        assertEquals(Action.StartLockTask, step.action)
        assertFalse(step.state.declined)
        assertTrue(step.state.viaRecovery)
    }

    @Test
    fun `before the first pin Aktifkan resets the attempt count so it is never silenced`() {
        var state = armed().copy(attempts = AkmLockdownRules.MAX_ATTEMPTS, declined = true)
        val step = AkmLockdownRules.forceRelock(state, t0, pinned = false)
        assertEquals(Action.StartLockTask, step.action)
        assertEquals(1, step.state.attempts)
        state = step.state
        assertTrue(state.promptSince != 0L)
    }

    @Test
    fun `three failed taps on Aktifkan unlock the way out`() {
        var state = AkmLockdownRules.onStartLockTaskFailed(requested())
        assertFalse(AkmLockdownRules.escapeAvailable(state))
        var now = t0 + 5_000
        repeat(AkmLockdownRules.FAILED_RELOCKS_BEFORE_ESCAPE) {
            assertFalse(AkmLockdownRules.escapeAvailable(state))
            state = AkmLockdownRules.forceRelock(state, now, pinned = false).state
            now += AkmLockdownRules.NO_DIALOG_MS
            state = AkmLockdownRules.evaluate(state, now, pinned = false)
            assertTrue(state.declined)
        }
        assertTrue(AkmLockdownRules.escapeAvailable(state))
    }

    @Test
    fun `failures of the automatic request do not count towards the way out`() {
        var state = requested()
        state = AkmLockdownRules.evaluate(state, t0 + AkmLockdownRules.NO_DIALOG_MS, pinned = false)
        assertTrue(state.declined)
        assertEquals(0, state.failedRelocks)
    }

    @Test
    fun `a refusal from startLockTask itself counts when it came from Aktifkan`() {
        val asked = AkmLockdownRules.forceRelock(AkmLockdownRules.onStartLockTaskFailed(requested()), t0 + 100, pinned = false).state
        assertEquals(1, AkmLockdownRules.onStartLockTaskFailed(asked).failedRelocks)
    }

    // ---- release and watchdog ----

    @Test
    fun `releasing stops wanting the pin and opens a short window in which the unpin is legitimate`() {
        val released = AkmLockdownRules.release(armed().copy(everPinned = true), t0)
        assertFalse(released.desired)
        assertTrue(AkmLockdownRules.isIntentionalExit(released, t0 + 1))
        assertFalse(AkmLockdownRules.isIntentionalExit(released, t0 + AkmLockdownRules.INTENTIONAL_EXIT_MS))
    }

    @Test
    fun `a session older than six hours frees the phone by itself`() {
        val state = armed()
        assertFalse(AkmLockdownRules.watchdogExpired(state, t0 + AkmLockdownRules.MAX_SESSION_MS - 1))
        assertTrue(AkmLockdownRules.watchdogExpired(state, t0 + AkmLockdownRules.MAX_SESSION_MS))
        assertFalse(AkmLockdownRules.watchdogExpired(AkmLockdownRules.release(state, t0), t0 + AkmLockdownRules.MAX_SESSION_MS))
    }

    @Test
    fun `the numbers carried over from legacy are unchanged`() {
        assertEquals(2, AkmLockdownRules.MAX_ATTEMPTS)
        assertEquals(20_000L, AkmLockdownRules.PROMPT_TIMEOUT_MS)
        assertEquals(10_000L, AkmLockdownRules.INTENTIONAL_EXIT_MS)
        assertEquals(21_600_000L, AkmLockdownRules.MAX_SESSION_MS)
        assertEquals(3, AkmLockdownRules.FOREGROUND_RACE_RETRIES)
        assertEquals(300L, AkmLockdownRules.FOREGROUND_RACE_RETRY_DELAY_MS)
    }
}
