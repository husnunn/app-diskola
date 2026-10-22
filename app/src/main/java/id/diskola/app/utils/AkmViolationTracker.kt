package id.diskola.app.utils

import id.diskola.app.utils.AkmViolationRules.Type

/**
 * The memory of the violation watchers (doc `06` §10.1): when the student left, when an overlay was last reported, whether a split-screen
 * episode is already counted, since when the network is gone, what the clock showed last. It holds no Android type — the clocks and the
 * "is this the system's doing" question are passed in — so every "does this count?" has a JVM test. `ExamViolationDetector` feeds it
 * with the real events. Main thread only.
 */
class AkmViolationTracker(
    /** Records one violation; `metadata` may be null. */
    private val report: (type: String, metadata: Map<String, Any>?) -> Unit,
    /** The system's pin prompt is open, or the student just left on purpose: leaving then is not recorded. */
    private val isSuppressed: () -> Boolean,
    private val elapsed: () -> Long,
    private val wall: () -> Long,
    private val isoTime: (Long) -> String = { AkmViolationRules.isoDateTime(it) },
) {
    private var pausedAt = 0L
    private var lastObscuredReport = 0L
    private var episodeActive = false
    private var online = true
    private var offlineSince = 0L
    private var lastWall = 0L
    private var lastElapsed = 0L

    /** A session begins: [onlineNow] is what the connection looks like at that moment. */
    fun start(onlineNow: Boolean) {
        pausedAt = 0L
        lastObscuredReport = 0L
        episodeActive = false
        online = onlineNow
        offlineSince = 0L // an exam that starts offline does not count that time (legacy)
        lastWall = wall()
        lastElapsed = elapsed()
    }

    // ---- Leaving the app ----

    /** A change of configuration (rotation, a resize) recreates the Activity: that is not the student leaving. */
    fun onPaused(changingConfigurations: Boolean) {
        if (changingConfigurations) return
        pausedAt = elapsed()
    }

    fun onResumed() {
        val since = pausedAt
        pausedAt = 0L
        if (since == 0L) return
        if (!AkmViolationRules.shouldRecordBackground(intentionalExit = false, promptPending = isSuppressed())) return
        report(Type.APP_BACKGROUND, mapOf("durationSeconds" to AkmViolationRules.backgroundSeconds(since, elapsed())))
    }

    // ---- Overlay ----

    fun onTouchObscured() {
        val now = elapsed()
        if (!AkmViolationRules.shouldReportObscured(lastObscuredReport, now)) return
        lastObscuredReport = now
        report(Type.FLOATING_WINDOW, null)
    }

    // ---- Split screen: one row per episode ----

    fun onMultiWindowChanged(restricted: Boolean) {
        if (!restricted) {
            episodeActive = false
            return
        }
        if (episodeActive) return
        episodeActive = true
        report(Type.SPLIT_SCREEN, null)
    }

    // ---- Connection ----

    fun onOnlineChanged(value: Boolean) {
        if (value == online) return
        online = value
        if (!value) {
            offlineSince = elapsed()
            return
        }
        val since = offlineSince
        offlineSince = 0L
        if (since == 0L) return
        val seconds = AkmViolationRules.connectionLossSeconds(since, elapsed())
        if (AkmViolationRules.shouldReportConnectionLoss(seconds)) report(Type.CONNECTION_LOSS, mapOf("durationSeconds" to seconds))
    }

    // ---- Clock ----

    fun onTimeChanged() {
        val nowWall = wall()
        val nowElapsed = elapsed()
        if (AkmViolationRules.isTimeTampered(lastWall, nowWall, lastElapsed, nowElapsed)) {
            report(
                Type.TIME_TAMPERING,
                mapOf(
                    "deviceTimeBefore" to isoTime(AkmViolationRules.expectedWallNow(lastWall, lastElapsed, nowElapsed)),
                    "deviceTimeAfter" to isoTime(nowWall),
                ),
            )
        }
        lastWall = nowWall
        lastElapsed = nowElapsed
    }
}
