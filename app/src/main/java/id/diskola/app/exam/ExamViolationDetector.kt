package id.diskola.app.exam

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import id.diskola.app.utils.AkmViolationRules
import id.diskola.app.utils.AkmViolationRules.Type
import id.diskola.app.utils.AkmViolationTracker
import timber.log.Timber

/**
 * Watches an exam session for the things legacy recorded (doc `06` §10.1): leaving the app, a split/floating window, an overlay that
 * covers the screen, a screenshot, a lost connection and a changed clock. It decides *when* something counts ([AkmViolationRules]);
 * what happens next — a row in Room, maybe a penalty dialog — is [report]'s business. Everything runs on the main thread.
 *
 * Differences from legacy, all deliberate (`docs/FLOW_QUESTIONS.md`, 2026-10-16 "A2"):
 *  - the connection and the clock are watched for the whole session, not only while a screen is resumed (legacy lost any outage that
 *    started while the app was in the background — exactly when the student can do something with another device);
 *  - the clock watcher reads its baseline when it starts and reports the time the clock *would* have shown;
 *  - a screenshot is detected on API 34+ only: below that legacy's `MediaStore` fallback needed storage permissions the manifest removes,
 *    so it never fired (and `FLAG_SECURE` already blacks the picture out).
 */
class ExamViolationDetector(
    private val context: Context,
    /** Records the violation (and lets the session decide about a penalty). `metadata` may be null. */
    report: (type: String, metadata: Map<String, Any>?) -> Unit,
    /** The system's pin prompt is open, or the student just left on purpose (data settings): not the student's doing. */
    isSuppressed: () -> Boolean,
) {

    private val reportToSession: (String, Map<String, Any>?) -> Unit = report

    private val tracker = AkmViolationTracker(
        report = reportToSession,
        isSuppressed = isSuppressed,
        elapsed = { SystemClock.elapsedRealtime() },
        wall = { System.currentTimeMillis() },
    )

    private var screenshotHandle: Any? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var timeReceiverRegistered = false
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = tracker.onTimeChanged()
    }

    /** The session begins: clean counters, and the watchers that do not depend on a screen start. */
    fun start() {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        tracker.start(onlineNow = manager?.let(::isValidated) ?: true)
        startConnectionWatch(manager)
        startTimeWatch()
    }

    fun stop(activity: Activity?) {
        stopConnectionWatch()
        stopTimeWatch()
        unregisterScreenshot(activity)
    }

    fun onPaused(changingConfigurations: Boolean) = tracker.onPaused(changingConfigurations)

    fun onResumed() = tracker.onResumed()

    /** A touch arrived through another app's window (debounced to one row per five seconds). */
    fun onTouchObscured() = tracker.onTouchObscured()

    /** One row per episode: it starts when the window stops being the whole screen and ends when it is full again. */
    fun onMultiWindowChanged(restricted: Boolean) = tracker.onMultiWindowChanged(restricted)

    // ---- Screenshot (API 34+) ----

    fun registerScreenshot(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || screenshotHandle != null) return
        screenshotHandle = ScreenCaptureApi34.register(activity) { reportToSession(Type.SCREENSHOT, null) }
    }

    fun unregisterScreenshot(activity: Activity?) {
        val handle = screenshotHandle ?: return
        screenshotHandle = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ScreenCaptureApi34.unregister(activity, handle)
    }

    /** Kept in its own object so older devices never load a class that mentions an API 34 type. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object ScreenCaptureApi34 {
        fun register(activity: Activity, onCaptured: () -> Unit): Any? = try {
            val callback = Activity.ScreenCaptureCallback { onCaptured() }
            activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
            callback
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "registerScreenCaptureCallback gagal")
            null
        }

        fun unregister(activity: Activity?, handle: Any) {
            try {
                (handle as? Activity.ScreenCaptureCallback)?.let { activity?.unregisterScreenCaptureCallback(it) }
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "unregisterScreenCaptureCallback gagal")
            }
        }
    }

    // ---- Connection ----

    private fun startConnectionWatch(manager: ConnectivityManager?) {
        manager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                tracker.onOnlineChanged(
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                )
            }

            override fun onLost(network: Network) = tracker.onOnlineChanged(false)
        }
        try {
            manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
            networkCallback = callback
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "registerDefaultNetworkCallback gagal")
        }
    }

    private fun stopConnectionWatch() {
        val callback = networkCallback ?: return
        networkCallback = null
        try {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.unregisterNetworkCallback(callback)
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "unregisterNetworkCallback gagal")
        }
    }

    private fun isValidated(manager: ConnectivityManager): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork ?: return false) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ---- Clock ----

    private fun startTimeWatch() {
        try {
            ContextCompat.registerReceiver(context, timeReceiver, IntentFilter(Intent.ACTION_TIME_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            timeReceiverRegistered = true
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "registerReceiver(ACTION_TIME_CHANGED) gagal")
        }
    }

    private fun stopTimeWatch() {
        if (!timeReceiverRegistered) return
        timeReceiverRegistered = false
        try {
            context.unregisterReceiver(timeReceiver)
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "unregisterReceiver gagal")
        }
    }

    private companion object {
        const val TAG = "AKM-VIOLATION"
    }
}

/** Whether the Activity's window is no longer the whole screen (doc 06 §10.1): multi-window, picture-in-picture, an OEM floating window. */
internal object ExamWindowCheck {

    fun isRestricted(activity: Activity): Boolean {
        if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) return true
        if (AkmViolationRules.isOemFloatingConfig(activity.resources.configuration.toString())) return true
        return isWindowShrunk(activity)
    }

    /** API 30+ only: compares the window with the display mode. Older devices rely on the checks above. */
    private fun isWindowShrunk(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            val display = activity.display ?: return false
            val mode = display.mode
            val rotated = display.rotation == Surface.ROTATION_90 || display.rotation == Surface.ROTATION_270
            AkmViolationRules.isWindowShrunk(bounds.width(), bounds.height(), mode.physicalWidth, mode.physicalHeight, rotated)
        } catch (t: Throwable) {
            false
        }
    }
}
