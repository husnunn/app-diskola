package id.diskola.app.utils.session

import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point that subscribes/unsubscribes every session-scoped FCM topic (doc
 * `02-auth-login-sesi.md` §8.4, decision Q7). The legacy app scattered these calls across
 * `App.onCreate`, `HomePage.onCreate`, `AkunPage2` and `IntentUtil.logOut` — here it's one place,
 * called once at login success and once at logout, so topics can never go stale mid-session.
 *
 * Topic names and the `"topics/…"` + plain-name duplicate for the user topic are kept exactly as
 * the legacy app sent them — that's the backend contract, only *where* it's executed changed.
 */
@Singleton
class FcmTopicManager @Inject constructor() {

    private fun sessionTopics(userUuid: String, userId: Int, classId: Int, schoolUuid: String): List<String> =
        listOfNotNull(
            userUuid.takeIf { it.isNotBlank() }?.let { "school-leave-request-approved-$it" },
            userUuid.takeIf { it.isNotBlank() }?.let { "school-leave-request-rejected-$it" },
            userUuid.takeIf { it.isNotBlank() }?.let { "topics/diskola-notification-user-$it" },
            userUuid.takeIf { it.isNotBlank() }?.let { "diskola-notification-user-$it" },
            schoolUuid.takeIf { it.isNotBlank() }?.let { "diskola-notification-school-$it" },
            classId.takeIf { it > 0 }?.let { "diskola-notification-theory-$it" },
            classId.takeIf { it > 0 }?.let { "diskola-notification-task-$it" },
            userId.takeIf { it > 0 }?.let { "Klaspay-US-$it" },
            classId.takeIf { it > 0 }?.let { "attendance-$it" },
            "loggedin",
        )

    fun subscribeForSession(userUuid: String, userId: Int, classId: Int, schoolUuid: String) {
        val messaging = FirebaseMessaging.getInstance()
        messaging.unsubscribeFromTopic("unlogged")
        sessionTopics(userUuid, userId, classId, schoolUuid).forEach { messaging.subscribeToTopic(it) }
    }

    fun unsubscribeAll(userUuid: String, userId: Int, classId: Int, schoolUuid: String) {
        val messaging = FirebaseMessaging.getInstance()
        sessionTopics(userUuid, userId, classId, schoolUuid).forEach { messaging.unsubscribeFromTopic(it) }
        messaging.subscribeToTopic("unlogged")
    }
}
