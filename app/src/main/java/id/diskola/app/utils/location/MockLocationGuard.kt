package id.diskola.app.utils.location

import android.location.Location
import android.os.Build

/**
 * New in the Compose app (decision 2026-09-30, doc `07` §19#5): the legacy app accepted any GPS
 * fix. A fix flagged as coming from a mock provider is refused before any presensi submit
 * (Masuk/Pulang sekolah and Dinas Luar). Root detection is deliberately not attempted.
 */
object MockLocationGuard {

    const val MESSAGE = "Lokasi palsu terdeteksi, presensi tidak bisa diproses."

    fun isMock(location: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
}
