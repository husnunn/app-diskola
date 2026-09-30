package id.diskola.app.utils

import java.util.UUID
import javax.inject.Inject

/**
 * `X-Device-Fingerprint` for the Asesmen exam-school password/answer endpoints — ported verbatim
 * from the legacy app's `utils/AkmDeviceFingerprint.kt` (read directly from
 * `developer/proyek diskola/Diskola_App/android-portal`, not guessed): a random UUID generated
 * once and persisted, NOT derived from any hardware/OS identifier. Backend binds a checked exam
 * password to whichever value sent this header (`docs/repo lama/dokumentasi-asesmen.md:202-205` —
 * "perangkat lain" is a real server error), so it must stay stable for this app install.
 */
class DeviceUtil @Inject constructor(private val preference: PreferenceClass) {

    fun getFingerprint(): String {
        val stored = preference.getString(KEY).trim()
        if (stored.isNotEmpty()) return stored

        return UUID.randomUUID().toString().also { preference.putString(KEY, it) }
    }

    companion object {
        private const val KEY = "akm_device_fingerprint"
    }
}
