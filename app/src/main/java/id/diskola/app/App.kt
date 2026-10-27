package id.diskola.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.multidex.MultiDexApplication
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import id.diskola.app.repository.AkmProblemReportQueue
import id.diskola.app.repository.AkmRepository
import id.diskola.app.repository.AkmViolationQueue
import id.diskola.app.worker.AkmWork
import id.diskola.app.utils.PreferenceClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class  App : MultiDexApplication(), Application.ActivityLifecycleCallbacks, Configuration.Provider {

    @Inject
    lateinit var preference: PreferenceClass
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var akmRepository: AkmRepository
    @Inject
    lateinit var violationQueue: AkmViolationQueue
    @Inject
    lateinit var problemReportQueue: AkmProblemReportQueue

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        // Release builds log nothing: request bodies and exceptions can carry PINs and tokens (doc 08 Q14).
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // Exam answers still unsent (WorkManager keeps its jobs across restarts, but a job that was never created — a download from before
        // this build, a deadline whose work was pruned — is re-created here; KEEP makes repeating this harmless).
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { akmRepository.scheduleDeadlines() }.onFailure { Timber.e(it, "Gagal menjadwalkan ulang pengiriman jawaban ujian") }
            // Violations recorded before the process died (a session left by a kill, a crash): their periodic sender is put back.
            runCatching { violationQueue.examIdsWithRows().forEach { AkmWork.enqueueViolationUploader(this@App, it) } }
                .onFailure { Timber.e(it, "Gagal menjadwalkan ulang pengiriman pelanggaran ujian") }
            runCatching { if (problemReportQueue.countUnsent() > 0) AkmWork.enqueueProblemReportUpload(this@App) }
                .onFailure { Timber.e(it, "Gagal menjadwalkan ulang pengiriman laporan masalah") }
        }

        // The app is light-only: never follow the system's dark mode (see DiskolaTheme).
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // listen to phone connectivity
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//            (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.registerDefaultNetworkCallback(
//                object :
//                    ConnectivityManager.NetworkCallback() {
//                    override fun onAvailable(network: network) {
//                        appComponent.socketClass.initSocket()
//                        appComponent.socketClass.connect()
//                    }
//
//                    override fun onLost(network: network) {
//                        appComponent.socketClass.disconnect()
//                    }
//                })
//        }

        try { initFirebaseServices() } catch (_: Exception) {}

        // install keyboard emoji provider
//        EmojiManager.install(GoogleEmojiProvider())
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun onTrimMemory(level: Int) {
        if (level == TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_RUNNING_CRITICAL) {
            Runtime.getRuntime().gc()
        }
        super.onTrimMemory(level)
    }

    private fun initFirebaseServices() {
        // subscribe to notif topic unlogged
        FirebaseMessaging.getInstance().subscribeToTopic("unlogged")

        // get firebase token and id
        FirebaseInstallations.getInstance().id.addOnSuccessListener {
            preference.putString("firebase_id", it)
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            preference.putString("token", it)
        }

        // get firebase remote config
//        Firebase.remoteConfig.apply {
//            setConfigSettingsAsync(remoteConfigSettings {
//                minimumFetchIntervalInSeconds = 3600
//            }).addOnCompleteListener {
//                if (it.isSuccessful) {
//                    setDefaultsAsync(DefaultRemoteConfig.minVersion).addOnCompleteListener {
//                        if (it.isSuccessful) {
//                            fetchAndActivate().addOnCompleteListener {
//                                if (it.isSuccessful) {
//                                    try {
//                                        appComponent.moshi.adapter(MinVersionClass::class.java)
//                                            .fromJson(getString("minVersion").also { Timber.e(it) })
//                                            ?.let {
//                                                if (BuildConfig.VERSION_CODE < it.minVersion) {
//                                                    currentAct?.let { activity ->
//                                                        MaterialAlertDialogBuilder(
//                                                            activity,
//                                                            R.style.DialogTheme
//                                                        )
//                                                            .setTitle("Update Tersedia")
//                                                            .setMessage("Silahkan update ke versi terbaru aplikasi")
//                                                            .setCancelable(it.needUpdate)
//                                                            .setPositiveButton("Update") { dialog, _ ->
//                                                                try {
//                                                                    startActivity(
//                                                                        Intent(
//                                                                            Intent.ACTION_VIEW,
//                                                                            Uri.parse("market://details?id=$packageName")
//                                                                        )
//                                                                    )
//                                                                } catch (anfe: ActivityNotFoundException) {
//                                                                    startActivity(
//                                                                        Intent(
//                                                                            Intent.ACTION_VIEW,
//                                                                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
//                                                                        )
//                                                                    )
//                                                                }
//                                                                dialog.dismiss()
//                                                            }
//                                                            .show()
//                                                    }
//                                                }
//                                            }
//                                    } catch (e: Exception) {
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    var currentAct: Activity? = null
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentAct = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        currentAct = null
    }

    override fun onActivityResumed(activity: Activity) {
        currentAct = activity
    }

    override fun onActivityPaused(activity: Activity) {
        currentAct = null
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
