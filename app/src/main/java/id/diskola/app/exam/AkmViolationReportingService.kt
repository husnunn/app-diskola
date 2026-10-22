package id.diskola.app.exam

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import id.diskola.app.R
import id.diskola.app.repository.AkmViolationQueue
import id.diskola.app.worker.AkmNotifications
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Keeps the process alive and sends the violations while an exam session runs (doc `06` §10.4; legacy `AkmViolationReportingService`):
 * a foreground service of type `dataSync`, flushing every 30–60 s and again whenever the connection returns. Without it, a student who
 * leaves for another app could have the process frozen before the record of that leaves the phone.
 *
 * Started only from the foreground UI (the "Mulai Ujian" press) — Android 12+ refuses a foreground service started from the background.
 * Unlike legacy it is **not sticky** (a sticky restart had no extras and became a notification nobody could stop), `startForeground` is
 * guarded, and Android 15's `dataSync` time limit ([onTimeout]) ends it cleanly; the WorkManager job covers the rest.
 */
@AndroidEntryPoint
class AkmViolationReportingService : Service() {

    @Inject
    lateinit var queue: AkmViolationQueue

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private var examId = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (t: Throwable) {
            // Not allowed to run in the foreground (restricted background start): the periodic worker still sends the rows.
            Timber.tag(TAG).w(t, "startForeground ditolak")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_EXAM_ID, 0) ?: 0
        if (id <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        examId = id
        if (loop == null) {
            loop = scope.launch {
                while (isActive) {
                    delay(Random.nextLong(MIN_INTERVAL_MS, MAX_INTERVAL_MS))
                    queue.flush(id, "timer")
                }
            }
        }
        if (networkCallback == null) watchConnection()
        return START_NOT_STICKY
    }

    /** Android 15 stops a `dataSync` service after its time budget: say so, stop, and let the worker carry on. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Timber.tag(TAG).w("batas waktu layanan latar tercapai (type=%d)", fgsType)
        stopSelf()
    }

    override fun onDestroy() {
        networkCallback?.let {
            try {
                (getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager)?.unregisterNetworkCallback(it)
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "unregisterNetworkCallback gagal")
            }
        }
        networkCallback = null
        scope.cancel()
        super.onDestroy()
    }

    /** One flush each time a network comes back — exactly when a violation noticed offline can finally leave the phone. */
    private fun watchConnection() {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val id = examId
                if (id > 0) scope.launch { queue.flush(id, "koneksi") }
            }
        }
        try {
            manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
            networkCallback = callback
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "registerDefaultNetworkCallback gagal")
        }
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, AkmNotifications.ensureChannel(this))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ujian sedang berlangsung")
            .setContentText("Sistem pemantauan ujian aktif di latar belakang")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    companion object {
        private const val TAG = "AKM-VIOLATION"
        private const val EXTRA_EXAM_ID = "akm_id"
        private const val NOTIFICATION_ID = 0x414B4D
        private const val MIN_INTERVAL_MS = 30_000L
        private const val MAX_INTERVAL_MS = 60_000L

        /** Call from foreground UI only. Never throws: a refusal just means the worker does the job. */
        fun start(context: Context, examId: Int) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AkmViolationReportingService::class.java).putExtra(EXTRA_EXAM_ID, examId))
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "layanan pelaporan tidak dapat dimulai")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AkmViolationReportingService::class.java))
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "layanan pelaporan tidak dapat dihentikan")
            }
        }
    }
}
