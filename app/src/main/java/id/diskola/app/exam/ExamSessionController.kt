package id.diskola.app.exam

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.repository.AkmProblemReportQueue
import id.diskola.app.repository.AkmSettingsRepository
import id.diskola.app.repository.AkmViolationQueue
import id.diskola.app.utils.AkmExamState
import id.diskola.app.utils.AkmLockdownRules
import id.diskola.app.utils.AkmLockdownRules.Action
import id.diskola.app.utils.AkmPenaltyRules
import id.diskola.app.utils.AkmSessionRules
import id.diskola.app.utils.AkmSessionRules.EndReason
import id.diskola.app.utils.AkmViolationRules
import id.diskola.app.worker.AkmWork
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** What the exam screens and the overlays read; everything else stays inside [ExamSessionController]. */
data class ExamSessionState(
    /** The exam whose session is running; 0 = none. */
    val examId: Int = 0,
    /** Screen pinning ("mode ketat") is wanted for this session. */
    val strict: Boolean = false,
    /** Non-null while the "Mode Terkunci Terlepas" dialog is up. */
    val recovery: Recovery? = null,
    /** A penalty is running (doc 06 §11): the one non-cancelable dialog, the pill in the header, the actions hidden. Null = none. */
    val penalty: PenaltyState? = null,
    /** The window is no longer the whole screen (split screen, floating window, picture-in-picture): the exam is covered ("Layar Penuh Diperlukan"). */
    val multiWindowBlocked: Boolean = false,
) {
    val active: Boolean get() = examId != 0

    data class PenaltyState(
        /** What the cause card says (the newest violation); null for a penalty carried over from the last session. */
        val cause: String?,
        val remainingMillis: Long,
    )

    data class Recovery(
        /** "Aktifkan" was pressed and the answer is awaited ("Memeriksa..."). */
        val checking: Boolean = false,
        val message: String = AkmSessionRules.RECOVERY_MESSAGE,
        /** After three failed tries the dialog also offers a way out ("Keluar dari ujian"). */
        val canEscape: Boolean = false,
    )
}

/**
 * One thing the session recorded. It is already stored for sending ([AkmViolationQueue.record]) by the time this is emitted; the
 * penalty layer decides from [triggersPenalty] whether a dialog follows. [message] is what the penalty dialog's cause card says.
 */
data class ExamViolation(val examId: Int, val type: String, val message: String, val triggersPenalty: Boolean)

sealed interface ExamSessionEvent {
    /** The student chose "Keluar dari ujian" on the recovery dialog: the screens must run the abort flow. */
    data object LeaveRequested : ExamSessionEvent
}

/**
 * The exam session of this process (doc `06` §9, §12; plan A2a). One singleton instead of per-screen state because the app is a
 * single Activity: a session spans the lobby and the question screens, navigation inside it must not look like leaving it, and the
 * pin, the window flags and the "session open" marker have to outlive any one screen.
 *
 * It owns: the session marker, `FLAG_SECURE` / keep-screen-on / hide-overlay on the window, and screen pinning driven by
 * [AkmLockdownRules]. `MainActivity` forwards its lifecycle, focus and touches here; the screens read [state] and call [checkpoint].
 * Everything runs on the main thread.
 */
@Singleton
class ExamSessionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val examState: AkmExamState,
    private val settings: AkmSettingsRepository,
    private val violationQueue: AkmViolationQueue,
    private val problemReports: AkmProblemReportQueue,
) {

    private val _state = MutableStateFlow(ExamSessionState())
    val state: StateFlow<ExamSessionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ExamSessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ExamSessionEvent> = _events.asSharedFlow()

    private val _violations = MutableSharedFlow<ExamViolation>(extraBufferCapacity = 16)
    val violations: SharedFlow<ExamViolation> = _violations.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activityRef: WeakReference<ComponentActivity>? = null
    private var lock = AkmLockdownRules.State()
    private var ticker: Job? = null
    private var recoveryWatch: Job? = null
    private var penaltyTicker: Job? = null

    /** Leaving the app is not the student's doing while the system's pin prompt is open or right after a deliberate exit. */
    private val detector = ExamViolationDetector(
        context = context,
        report = { type, metadata -> reportViolation(type, metadata) },
        isSuppressed = { AkmLockdownRules.promptPending(lock, now()) || AkmLockdownRules.isIntentionalExit(lock, now()) },
    )

    private fun now(): Long = SystemClock.elapsedRealtime()

    private fun activity(): ComponentActivity? = activityRef?.get()

    fun isActiveFor(examId: Int): Boolean = _state.value.examId == examId

    // ---- Activity binding (MainActivity) ----

    private val multiWindowListener = Consumer<MultiWindowModeChangedInfo> { syncMultiWindow() }
    private val pictureInPictureListener = Consumer<PictureInPictureModeChangedInfo> { syncMultiWindow() }
    private val configurationListener = Consumer<Configuration> { syncMultiWindow() }

    /** A (re)created Activity takes over the session: its window gets the flags again (it was rotated or recreated). */
    fun attach(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        activity.addOnMultiWindowModeChangedListener(multiWindowListener)
        activity.addOnPictureInPictureModeChangedListener(pictureInPictureListener)
        activity.addOnConfigurationChangedListener(configurationListener)
        if (_state.value.active) applySessionWindow(activity, on = true)
    }

    fun detach(activity: ComponentActivity) {
        activity.removeOnMultiWindowModeChangedListener(multiWindowListener)
        activity.removeOnPictureInPictureModeChangedListener(pictureInPictureListener)
        activity.removeOnConfigurationChangedListener(configurationListener)
        if (activityRef?.get() === activity) activityRef = null
    }

    /** `FLAG_SECURE` (screenshots and recordings come out black, the recents thumbnail too) and the rule that overlays do not get touches. */
    private fun applySessionWindow(activity: ComponentActivity, on: Boolean) {
        val window = activity.window
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            window.decorView.filterTouchesWhenObscured = true
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.decorView.filterTouchesWhenObscured = false
        }
    }

    // ---- Starting and ending ----

    /**
     * Opens the session of [examId] (the student pressed "Mulai Ujian"). Returns whether the **previous** session of this exam was
     * left open (marker still set and none running here); that is already recorded as a `not_submitted` violation.
     */
    fun begin(examId: Int): Boolean {
        if (examId <= 0 || _state.value.examId == examId) return false
        if (_state.value.active) end(_state.value.examId, EndReason.Left)
        val unclosed = AkmSessionRules.isUnclosed(sessionAlreadyRunning = false, markerActive = examState.isSessionActive(examId))
        examState.setSessionActive(examId, true)
        val strict = settings.isStrict()
        _state.value = ExamSessionState(examId = examId, strict = strict)
        activity()?.let { applySessionWindow(it, on = true) }
        applyResumeFlags(resumed = true)
        ioScope.launch { violationQueue.clearPause(examId) }
        detector.start()
        resumeStoredPenalty(examId)
        syncMultiWindow()
        // Foreground service + the periodic safety net: both only start here, with the app in front.
        AkmViolationReportingService.start(context, examId)
        AkmWork.enqueueViolationUploader(context, examId)
        if (unclosed) {
            // The previous session of this exam was left without handing in (killed, crashed, "Ya, keluar"): that is recorded now.
            reportViolation(
                AkmViolationRules.Type.APP_BACKGROUND,
                mapOf("reason" to AkmViolationRules.NOT_SUBMITTED_REASON),
                AkmViolationRules.NOT_SUBMITTED_DESCRIPTION,
            )
        }
        if (strict) {
            lock = AkmLockdownRules.arm(now())
            apply(AkmLockdownRules.retryLock(lock, now(), isPinned()))
        } else {
            lock = AkmLockdownRules.State()
        }
        return unclosed
    }

    /** "Ya, keluar" / "Keluar dari ujian": leaves without handing in — the marker stays set on purpose. */
    fun abort(examId: Int) = end(examId, EndReason.Abort)

    /**
     * Closes the session. The marker is cleared only for the reasons that hand the exam in ([EndReason.completes]) — and that also
     * when no session of this exam is running here (the answers may have gone out through the background worker).
     */
    fun end(examId: Int, reason: EndReason) {
        if (reason.completes) examState.markHandedIn(examId)
        if (_state.value.examId != examId) return
        ticker?.cancel()
        recoveryWatch?.cancel()
        penaltyTicker?.cancel()
        if (lock.desired || isPinned()) {
            lock = AkmLockdownRules.release(lock, now())
            stopLockTaskQuietly()
        }
        detector.stop(activity())
        AkmViolationReportingService.stop(context)
        activity()?.let { applySessionWindow(it, on = false) }
        applyHideOverlay(false)
        _state.value = ExamSessionState()
        // One last try to get what was recorded to the school; whatever is left is carried by the periodic job.
        ioScope.launch { violationQueue.flush(examId, "akhir-sesi") }
    }

    /**
     * An exam screen was found without a running session — the process was killed and the back stack restored, or a link landed
     * there. The password is asked again next time and a pin that outlived the process is let go (`process_restored`, doc 06 §12.3).
     */
    fun onStaleExamScreen(examId: Int) {
        examState.setPasswordChecked(examId, false)
        if (!_state.value.active && isPinned()) stopLockTaskQuietly()
    }

    // ---- Lifecycle (forwarded by MainActivity) ----

    fun onActivityResumed() {
        val current = _state.value
        if (!current.active) return
        applyResumeFlags(resumed = true)
        // Same order as legacy `onResume`: first what happened while away, then the screen checks, then the lock.
        detector.onResumed()
        activity()?.let(detector::registerScreenshot)
        syncMultiWindow()
        if (!current.strict) return
        if (AkmLockdownRules.watchdogExpired(lock, now())) {
            // Six hours is not an exam: never keep a phone locked for good.
            lock = AkmLockdownRules.release(lock, now())
            stopLockTaskQuietly()
            _state.update { it.copy(strict = false, recovery = null) }
            return
        }
        checkpoint("resume")
        apply(AkmLockdownRules.ensureLocked(lock, now(), isPinned()))
    }

    fun onActivityPaused() {
        if (!_state.value.active) return
        applyResumeFlags(resumed = false)
        detector.onPaused(changingConfigurations = activity()?.isChangingConfigurations == true)
        detector.unregisterScreenshot(activity())
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        val current = _state.value
        if (!current.active || !current.strict) return
        lock = AkmLockdownRules.onFocusChanged(lock, hasFocus, now())
        if (hasFocus) {
            evaluateNow()
            checkpoint("focus_kembali")
        }
    }

    /**
     * Whether this touch must not reach the screen: the pin was lost, so every touch is held back until the recovery dialog is
     * answered. Touches pass while a system prompt is open, during a deliberate exit, or when the dialog itself owns them.
     */
    fun shouldBlockTouch(event: MotionEvent): Boolean {
        val current = _state.value
        if (!current.active) return false
        val time = now()
        val systemOwned = AkmLockdownRules.promptPending(lock, time) || AkmLockdownRules.isIntentionalExit(lock, time)
        if (current.strict && !systemOwned && current.recovery == null &&
            event.actionMasked == MotionEvent.ACTION_DOWN && !checkpoint("sentuh_layar")
        ) {
            return true
        }
        // Another app's window is over the exam: its touches are dropped, and the first of a burst is recorded (doc 06 §10.1).
        if (!systemOwned && event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0) {
            detector.onTouchObscured()
            return true
        }
        return false
    }

    // ---- Violations ----

    /** Stores the violation for sending, then tells the penalty layer. The row is written first so that nothing is lost if the UI part fails. */
    private fun reportViolation(
        type: String,
        metadata: Map<String, Any>?,
        description: String = AkmViolationRules.description(type),
    ) {
        val examId = _state.value.examId
        if (examId == 0) return
        ioScope.launch { violationQueue.record(examId, type, metadata) }
        _violations.tryEmit(
            ExamViolation(
                examId = examId,
                type = type,
                message = AkmViolationRules.penaltyCause(description),
                triggersPenalty = AkmViolationRules.triggersPenalty(type),
            ),
        )
        applyPenaltyDecision(AkmViolationRules.penaltyCause(description), AkmViolationRules.triggersPenalty(type))
    }

    // ---- Penalty (doc 06 §11) ----

    /**
     * The violation is already stored and on its way to the school; this only decides about the penalty. The setting is read again now
     * (legacy re-read it too), and nothing the app itself shows holds a penalty back — only the system's own pin prompt and a deliberate exit.
     */
    private fun applyPenaltyDecision(cause: String, triggersPenalty: Boolean) {
        val now = now()
        when (
            AkmPenaltyRules.decide(
                penaltyOn = settings.isPenaltyApplied(),
                triggersPenalty = triggersPenalty,
                alreadyActive = _state.value.penalty != null,
                systemOwned = AkmLockdownRules.promptPending(lock, now) || AkmLockdownRules.isIntentionalExit(lock, now),
            )
        ) {
            AkmPenaltyRules.Decision.None -> Unit
            AkmPenaltyRules.Decision.UpdateCause -> _state.update { it.copy(penalty = it.penalty?.copy(cause = cause)) }
            AkmPenaltyRules.Decision.Start -> startPenalty(cause)
        }
    }

    private fun startPenalty(cause: String?) {
        val examId = _state.value.examId
        if (examId == 0) return
        val totalMillis = settings.penaltySeconds() * 1000L
        val endAt = AkmPenaltyRules.endAt(System.currentTimeMillis(), settings.penaltySeconds())
        examState.setPenalty(examId, endAt, totalMillis)
        _state.update { it.copy(penalty = ExamSessionState.PenaltyState(cause = cause, remainingMillis = totalMillis)) }
        runPenaltyTicker(examId)
    }

    /** A penalty that was still running when the exam was left (or the process died) goes on from where it was — it is not restarted. */
    private fun resumeStoredPenalty(examId: Int) {
        if (!settings.isPenaltyApplied()) return
        val remaining = AkmPenaltyRules.remainingMillis(examState.penaltyEnd(examId), System.currentTimeMillis(), examState.penaltyTotalMillis(examId))
        if (remaining <= 0L) {
            examState.clearPenalty(examId)
            return
        }
        _state.update { it.copy(penalty = ExamSessionState.PenaltyState(cause = null, remainingMillis = remaining)) }
        runPenaltyTicker(examId)
    }

    /** Twice a second the time left is recomputed from the stored end; at zero the dialog closes and the penalty is forgotten. */
    private fun runPenaltyTicker(examId: Int) {
        penaltyTicker?.cancel()
        penaltyTicker = scope.launch {
            while (_state.value.examId == examId && _state.value.penalty != null) {
                val remaining = AkmPenaltyRules.remainingMillis(
                    examState.penaltyEnd(examId),
                    System.currentTimeMillis(),
                    examState.penaltyTotalMillis(examId),
                )
                if (remaining <= 0L) {
                    examState.clearPenalty(examId)
                    _state.update { it.copy(penalty = null) }
                    return@launch
                }
                _state.update { it.copy(penalty = it.penalty?.copy(remainingMillis = remaining)) }
                delay(PENALTY_TICK_MS)
            }
        }
    }

    // ---- Laporkan Masalah (doc 06 §13) ----

    /** Whole seconds until the next report may be filed. */
    fun problemReportCooldownSeconds(): Int = problemReports.cooldownSecondsLeft()

    /** Stores the report (with the violations of this session) and sends it as soon as there is a connection. Returns at once. */
    fun submitProblemReport(message: String, confirmedSystemFault: Boolean) {
        val examId = _state.value.examId
        if (examId == 0) return
        ioScope.launch {
            problemReports.file(examId, message, confirmedSystemFault)
            problemReports.flush("laporan-baru")
        }
    }

    /** The window is the whole screen or it is not: blocks the exam while it is not, and counts each episode once. */
    private fun syncMultiWindow() {
        val current = _state.value
        if (!current.active) return
        val activity = activity() ?: return
        val restricted = ExamWindowCheck.isRestricted(activity)
        detector.onMultiWindowChanged(restricted)
        if (current.multiWindowBlocked != restricted) _state.update { it.copy(multiWindowBlocked = restricted) }
    }

    // ---- Lockdown ----

    /**
     * A protected action (hand in, change question, open an instruction…) asks permission here. False when the pin was lost: the
     * action is held back and the recovery dialog is shown (`enforceLockdownAtCheckpoint`).
     */
    fun checkpoint(reason: String): Boolean {
        val current = _state.value
        if (!current.active || !current.strict) return true
        if (!AkmLockdownRules.hasActiveBreach(lock, now(), isPinned())) return true
        Timber.tag(TAG).w("Mode terkunci terlepas (checkpoint: %s)", reason)
        if (current.recovery == null) showRecovery(AkmSessionRules.RECOVERY_MESSAGE)
        return false
    }

    /** "Aktifkan" on the recovery dialog. */
    fun onRecoveryActivate() {
        val current = _state.value
        if (!current.active || !current.strict || current.recovery?.checking == true) return
        _state.update { it.copy(recovery = (it.recovery ?: ExamSessionState.Recovery()).copy(checking = true)) }
        apply(AkmLockdownRules.forceRelock(lock, now(), isPinned()))
        // Already pinned again (or nothing to ask): the answer is known now, not after a tick.
        if (lock.promptSince == 0L) {
            lock = AkmLockdownRules.evaluate(lock, now(), isPinned())
            resolveRecovery(wasRecovery = true)
        }
    }

    /** "Keluar dari ujian" on the recovery dialog (offered after three failed tries). */
    fun onRecoveryEscape() {
        _events.tryEmit(ExamSessionEvent.LeaveRequested)
    }

    /** Opening the data settings on purpose (legacy `openDataSettings`): the pin drops for a moment and that is not a breach. */
    fun beginTemporaryExit() {
        if (!_state.value.active) return
        lock = AkmLockdownRules.beginTemporaryExit(lock, now())
        stopLockTaskQuietly()
    }

    fun endTemporaryExit() {
        if (!_state.value.active) return
        lock = AkmLockdownRules.endTemporaryExit(lock)
    }

    private fun apply(step: AkmLockdownRules.Step) {
        lock = step.state
        if (step.action == Action.StartLockTask) {
            startLockTask(AkmLockdownRules.FOREGROUND_RACE_RETRIES)
            startTicker()
        }
    }

    private fun startLockTask(retriesLeft: Int) {
        val activity = activity()
        if (activity == null) {
            failLockTask(IllegalStateException("tidak ada Activity"))
            return
        }
        activity.window.decorView.post {
            try {
                activity.startLockTask()
            } catch (e: IllegalArgumentException) {
                // "Invalid task, not in foreground": some OEMs need a moment after a screen change.
                if (retriesLeft > 0) {
                    scope.launch {
                        delay(AkmLockdownRules.FOREGROUND_RACE_RETRY_DELAY_MS)
                        if (lock.promptSince != 0L) startLockTask(retriesLeft - 1)
                    }
                } else {
                    failLockTask(e)
                }
            } catch (t: Throwable) {
                // A device that has pinning switched off (or refuses it): the request is over.
                failLockTask(t)
            }
        }
    }

    private fun failLockTask(error: Throwable) {
        Timber.tag(TAG).w(error, "startLockTask ditolak")
        val wasRecovery = lock.viaRecovery
        lock = AkmLockdownRules.onStartLockTaskFailed(lock)
        ticker?.cancel()
        resolveRecovery(wasRecovery)
    }

    /** While a request is open, look at it a few times a second: pin state and focus events decide how it ended. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (lock.promptSince != 0L) {
                delay(TICK_MS)
                evaluateNow()
            }
        }
    }

    private fun evaluateNow() {
        val before = lock
        lock = AkmLockdownRules.evaluate(lock, now(), isPinned())
        if (before.promptSince != 0L && lock.promptSince == 0L) resolveRecovery(before.viaRecovery)
    }

    /** A request just ended: close the dialog when it worked, otherwise show it (with the "belum aktif" text after an "Aktifkan"). */
    private fun resolveRecovery(wasRecovery: Boolean) {
        if (!_state.value.strict) return
        if (!lock.declined && lock.everPinned) {
            _state.update { it.copy(recovery = null) }
        } else if (lock.declined) {
            showRecovery(if (wasRecovery) AkmSessionRules.RECOVERY_RETRY_MESSAGE else AkmSessionRules.RECOVERY_MESSAGE)
        }
    }

    private fun showRecovery(message: String) {
        _state.update { it.copy(recovery = ExamSessionState.Recovery(checking = false, message = message, canEscape = AkmLockdownRules.escapeAvailable(lock))) }
        watchRecovery()
    }

    /**
     * The dialog can be up while the system's pin prompt is still open (some devices give the prompt no window focus, so "no prompt
     * appeared" cannot be told from "prompt not answered yet"). Whenever the pin turns up the dialog closes by itself — the student
     * should not have to tap "Aktifkan" for something that already worked.
     */
    private fun watchRecovery() {
        recoveryWatch?.cancel()
        recoveryWatch = scope.launch {
            while (_state.value.recovery != null && _state.value.strict) {
                delay(RECOVERY_WATCH_MS)
                if (isPinned()) {
                    lock = AkmLockdownRules.evaluate(lock, now(), pinned = true)
                    _state.update { it.copy(recovery = null) }
                }
            }
        }
    }

    private fun isPinned(): Boolean = try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    } catch (t: Throwable) {
        false
    }

    private fun stopLockTaskQuietly() {
        try {
            if (isPinned()) activity()?.stopLockTask()
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "stopLockTask gagal")
        }
    }

    // ---- Window ----

    /** Keep-screen-on and hide-overlay only while the Activity is in front, like legacy `onResume`/`onPause`. */
    private fun applyResumeFlags(resumed: Boolean) {
        val window = activity()?.window ?: return
        if (resumed) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyHideOverlay(resumed)
    }

    /** Other apps' overlay windows are hidden over the exam (API 31+, normal permission `HIDE_OVERLAY_WINDOWS`). */
    private fun applyHideOverlay(hide: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            activity()?.window?.setHideOverlayWindows(hide)
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "setHideOverlayWindows ditolak")
        }
    }

    private companion object {
        const val TAG = "AKM-SESSION"
        const val TICK_MS = 250L
        const val RECOVERY_WATCH_MS = 500L
        const val PENALTY_TICK_MS = 500L
    }
}
