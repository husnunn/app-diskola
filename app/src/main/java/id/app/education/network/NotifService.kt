package id.app.education.network

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.AndroidEntryPoint
import id.app.education.R
import id.app.education.utils.IntentUtil
import id.app.education.utils.NotifUtil
import id.app.education.utils.PreferenceClass
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotifService : FirebaseMessagingService(), LifecycleOwner {

    @Inject
    lateinit var moshi: Moshi
    @Inject
    lateinit var pref: PreferenceClass
    @Inject
    lateinit var intentUtil: IntentUtil
    @Inject
    lateinit var notifUtil: NotifUtil

    private lateinit var lifecycleRegistry: LifecycleRegistry
    // Implement the lifecycle property
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data

//        com.google.firebase.messaging.RemoteMessage$Notification@a72bb74
        Timber.e("notif - received notif: ${message.notification}")

        // {detail=, body=this lorem lorem lorem lorem lorem lorem, menu=NOTIFICATION-USER, title=Pengumuman all, child_id=234}
        Timber.e("notif - received data: $data")

        // Create notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.app_name) // Use consistent channel ID
            val name = getString(R.string.app_name)
            val descriptionText = "diskola desc"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        try {
            val builder = NotificationCompat.Builder(applicationContext, getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(data["title"])
                .setContentText(data["body"])
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(data["body"]))
                .setPriority(NotificationCompat.PRIORITY_HIGH)


            // Set notification data
            data["data"]?.let { pref.putString("notif_data", it) }

            Timber.e("notif - calling handleNotificationData()") // Tambahkan log sebelum pemanggilan
            // Handle page data and notification logic
            handleNotificationData(data, builder)
            Timber.e("notif - finished handleNotificationData()") // Tambahkan log setelah pemanggilan
        } catch (e: Exception) {
            Timber.e("notif - builder catch $e")
        }
    }

    private fun handleNotificationData(data: Map<String, String>, builder: NotificationCompat.Builder) {
        Timber.e("notif --> handleNotificationData data: $data")

        val pageData = data["page"] ?: ""
        val menu = data["menu"] ?: ""
        val id = data["child_id"]?.toIntOrNull() ?: 0  // Gunakan child_id sebagai ID

        if (menu == "NOTIFICATION-USER") {
            Timber.e("notif --> setting pending intent for NOTIFICATION-USER")
//            builder.setContentIntent(getPendingIntent(menu, id))
            postNotification(builder, id)
        } else if (pageData.isNotEmpty()) {
            val adapter = moshi.adapter(NotifPage::class.java).lenient() // Use lenient parsing
            Timber.e("notif --> pageData: ${pageData}")
            try {
                // Validate and parse the JSON string
                if (isValidJson(pageData)) {
                    adapter.fromJson(pageData)?.let { page ->
                        Timber.e("notif --> page --> id: ${page.id}")
                        when (page.menu) {
                            "logout" -> intentUtil.logOut(this)
                            "email_verified" -> pref.putBoolean("is_verified", true)
                            else -> {
//                                builder.setContentIntent(getPendingIntent(page.menu, page.id, page.subId))
                                postNotification(builder, page.id)
                            }
                        }
                    }
                } else {
                    Timber.e("Invalid JSON: $pageData")
                }
            } catch (e: Exception) {
                Timber.e("notif - handleNotificationData catch $e")
            }
        } else if (data["body"].orEmpty().isNotEmpty()) {

        } else {
//            builder.setContentIntent(getPendingIntent())
            postNotification(builder, 0)
        }
    }

    private fun postNotification(builder: NotificationCompat.Builder, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
    }

//    private fun getPendingIntent(menu: String = "", id: Int = 0, subId: Int = 0,data: Map<String, String> = emptyMap()) =
//        TaskStackBuilder.create(applicationContext).run {
//            Timber.d("NotifService : menu:$menu, id:$id, subId:,")
//            addNextIntentWithParentStack(
//                if (pref.getBoolean("logged_in"))
//                    when (menu) {
//                        "ebede" -> Intent(applicationContext, MainActivity::class.java).putExtra("ebede", id)
//                        else -> Intent(applicationContext, MainActivity::class.java)
//                    }
//                else {
//                    if (menu.isNotEmpty()) pref.putString("notif_goto", menu)
//                    Intent(applicationContext, LoginActivity::class.java)
//
//                }
//            )
//            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
//        }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        pref.putString("token", token)
        if (pref.getString("user_uuid").isNotEmpty()) {
            // Update the FCM token
        }
    }

    // Validate JSON format
    private fun isValidJson(json: String): Boolean {
        return try {
            // Attempt to parse with a generic adapter
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Any::class.java)
                .fromJson(json) != null
        } catch (e: Exception) {
            false
        }
    }

}

@Keep
@JsonClass(generateAdapter = true)
data class NotifPage(val id: Int = 0, val menu: String = "", val uuid: String? = "", val subId: Int = 0)