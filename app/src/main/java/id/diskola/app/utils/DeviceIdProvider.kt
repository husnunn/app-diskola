package id.diskola.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * `device_id` sent with `login-account`/`login-sso` (doc `02-auth-login-sesi.md` §4.3/§5.2) — the
 * legacy app's `DeviceFingerprint.kt` used `Settings.Secure.ANDROID_ID` for this, which is what
 * backend's `DEVICE_CONFLICT`/single-device-login check keys off. Deliberately separate from
 * [DeviceUtil]'s random per-install UUID, which only exists for the Asesmen exam-password header
 * and must never be confused with this one.
 */
class DeviceIdProvider @Inject constructor(@ApplicationContext private val context: Context) {

    @SuppressLint("HardwareIds")
    fun get(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
}
