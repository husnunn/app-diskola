package id.diskola.app.utils

import id.diskola.app.utils.AkmViolationRules.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AkmViolationTrackerTest {

    private var elapsed = 10_000L
    private var wall = 1_000_000_000L
    private var suppressed = false
    private val reported = mutableListOf<Pair<String, Map<String, Any>?>>()

    private fun tracker(onlineAtStart: Boolean = true): AkmViolationTracker {
        val tracker = AkmViolationTracker(
            report = { type, metadata -> reported += type to metadata },
            isSuppressed = { suppressed },
            elapsed = { elapsed },
            wall = { wall },
            isoTime = { "T$it" },
        )
        tracker.start(onlineAtStart)
        return tracker
    }

    private fun advance(ms: Long, wallMs: Long = ms) {
        elapsed += ms
        wall += wallMs
    }

    // ---- leaving the app ----

    @Test
    fun `leaving and coming back is recorded with the time away, even when the penalty is off`() {
        val tracker = tracker()
        tracker.onPaused(changingConfigurations = false)
        advance(12_400)
        tracker.onResumed()
        assertEquals(listOf(Type.APP_BACKGROUND to mapOf<String, Any>("durationSeconds" to 12L)), reported)
    }

    @Test
    fun `a quick trip under a second is still recorded`() {
        val tracker = tracker()
        tracker.onPaused(false)
        advance(300)
        tracker.onResumed()
        assertEquals(mapOf<String, Any>("durationSeconds" to 0L), reported.single().second)
    }

    @Test
    fun `a rotation or a resize that recreates the screen is not leaving`() {
        val tracker = tracker()
        tracker.onPaused(changingConfigurations = true)
        advance(500)
        tracker.onResumed()
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `coming back without having left records nothing`() {
        tracker().onResumed()
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `leaving while the system prompt is open or right after a deliberate exit is not recorded`() {
        val tracker = tracker()
        tracker.onPaused(false)
        advance(2_000)
        suppressed = true
        tracker.onResumed()
        assertTrue(reported.isEmpty())
        // and the next, ordinary trip is recorded again
        suppressed = false
        tracker.onPaused(false)
        advance(2_000)
        tracker.onResumed()
        assertEquals(1, reported.size)
    }

    @Test
    fun `one trip is one row, a second resume adds nothing`() {
        val tracker = tracker()
        tracker.onPaused(false)
        advance(1_000)
        tracker.onResumed()
        tracker.onResumed()
        assertEquals(1, reported.size)
    }

    // ---- overlay ----

    @Test
    fun `a burst of obscured touches is one row, and another comes five seconds later`() {
        val tracker = tracker()
        tracker.onTouchObscured()
        advance(1_000)
        tracker.onTouchObscured()
        advance(3_000)
        tracker.onTouchObscured()
        assertEquals(listOf(Type.FLOATING_WINDOW), reported.map { it.first })
        advance(1_000)
        tracker.onTouchObscured()
        assertEquals(listOf(Type.FLOATING_WINDOW, Type.FLOATING_WINDOW), reported.map { it.first })
    }

    // ---- split screen ----

    @Test
    fun `a split-screen episode is one row however many times it is checked`() {
        val tracker = tracker()
        tracker.onMultiWindowChanged(true)
        tracker.onMultiWindowChanged(true)
        tracker.onMultiWindowChanged(true)
        assertEquals(listOf(Type.SPLIT_SCREEN), reported.map { it.first })
    }

    @Test
    fun `leaving split screen and entering it again is a new episode`() {
        val tracker = tracker()
        tracker.onMultiWindowChanged(true)
        tracker.onMultiWindowChanged(false)
        tracker.onMultiWindowChanged(true)
        assertEquals(2, reported.size)
    }

    @Test
    fun `a full window records nothing`() {
        val tracker = tracker()
        tracker.onMultiWindowChanged(false)
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `a new session starts with no episode counted`() {
        val tracker = tracker()
        tracker.onMultiWindowChanged(true)
        tracker.start(true)
        tracker.onMultiWindowChanged(true)
        assertEquals(2, reported.size)
    }

    // ---- connection ----

    @Test
    fun `an outage of five seconds or more is reported when the connection returns`() {
        val tracker = tracker()
        tracker.onOnlineChanged(false)
        advance(7_900)
        tracker.onOnlineChanged(true)
        assertEquals(listOf(Type.CONNECTION_LOSS to mapOf<String, Any>("durationSeconds" to 7L)), reported)
    }

    @Test
    fun `a blip shorter than five seconds is ignored`() {
        val tracker = tracker()
        tracker.onOnlineChanged(false)
        advance(4_900)
        tracker.onOnlineChanged(true)
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `repeated online or offline events change nothing`() {
        val tracker = tracker()
        tracker.onOnlineChanged(true)
        tracker.onOnlineChanged(false)
        advance(10_000)
        tracker.onOnlineChanged(false)
        advance(10_000)
        tracker.onOnlineChanged(true)
        assertEquals(mapOf<String, Any>("durationSeconds" to 20L), reported.single().second)
    }

    @Test
    fun `an exam that starts offline does not count the time before the first connection`() {
        val tracker = tracker(onlineAtStart = false)
        advance(60_000)
        tracker.onOnlineChanged(true)
        assertTrue(reported.isEmpty())
    }

    // ---- clock ----

    @Test
    fun `moving the clock an hour ahead is recorded with where the clock would have been`() {
        val tracker = tracker()
        val before = wall
        advance(ms = 60_000, wallMs = 60_000 + 3_600_000)
        tracker.onTimeChanged()
        val (type, metadata) = reported.single()
        assertEquals(Type.TIME_TAMPERING, type)
        assertEquals("T${before + 60_000}", metadata!!["deviceTimeBefore"])
        assertEquals("T$wall", metadata["deviceTimeAfter"])
    }

    @Test
    fun `a sync of a couple of seconds is not tampering`() {
        val tracker = tracker()
        advance(ms = 60_000, wallMs = 62_000)
        tracker.onTimeChanged()
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `the baseline moves with every event, so one change is one row`() {
        val tracker = tracker()
        advance(ms = 1_000, wallMs = 1_000 + 3_600_000)
        tracker.onTimeChanged()
        advance(ms = 1_000, wallMs = 1_000)
        tracker.onTimeChanged()
        assertEquals(1, reported.size)
    }

    @Test
    fun `setting the clock back is recorded too`() {
        val tracker = tracker()
        advance(ms = 30_000, wallMs = -3_600_000)
        tracker.onTimeChanged()
        assertEquals(Type.TIME_TAMPERING, reported.single().first)
    }
}
