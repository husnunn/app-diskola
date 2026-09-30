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
import id.diskola.app.utils.PreferenceClass
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class  App : MultiDexApplication(), Application.ActivityLifecycleCallbacks, Configuration.Provider {

    @Inject
    lateinit var preference: PreferenceClass
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

//        if (BuildConfig.BUILD_TYPE != "release") {
        Timber.plant(Timber.DebugTree())
//        }

        // Apply saved theme mode at startup (follow system by default)
        try {
            val mode = preference.getString("theme_mode").ifBlank { "system" }
            when (mode) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            if (preference.getString("theme_mode").isEmpty()) {
                preference.putString("theme_mode", mode)
            }
        } catch (_: Exception) { }

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
