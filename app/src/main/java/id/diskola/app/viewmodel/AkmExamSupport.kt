package id.diskola.app.viewmodel

import id.diskola.app.utils.AkmRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Calls [onTimeUp] once the exam's end time has passed, checking the clock at most every [maxStepMillis] so a device clock that is
 * moved while the screen is open is noticed within that long. With no end time (unreadable) it never fires: nothing is guessed.
 */
internal class DeadlineWatcher(
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxStepMillis: Long = 15_000L,
) {
    private var job: Job? = null

    fun start(endAtMillis: Long?, onTimeUp: suspend () -> Unit) {
        job?.cancel()
        if (endAtMillis == null) return
        job = scope.launch {
            while (true) {
                val now = clock()
                if (AkmRules.isTimeUp(now, endAtMillis)) {
                    onTimeUp()
                    return@launch
                }
                delay((endAtMillis - now).coerceIn(1L, maxStepMillis))
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
